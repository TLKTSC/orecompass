package com.example.orecompass;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class OreCompassItem extends Item {

    public enum OreMode {
        DIAMOND("Elmas", 10, BlockTags.DIAMOND_ORES, Formatting.AQUA),
        IRON("Demir", 25, BlockTags.IRON_ORES, Formatting.GRAY),
        GOLD("Altın", 38, BlockTags.GOLD_ORES, Formatting.GOLD),
        COAL("Kömür", 60, BlockTags.COAL_ORES, Formatting.DARK_GRAY),
        COPPER("Bakır", 49, BlockTags.COPPER_ORES, Formatting.RED);

        public final String label;
        public final int range;
        public final TagKey<Block> tag;
        public final Formatting color;

        OreMode(String label, int range, TagKey<Block> tag, Formatting color) {
            this.label = label;
            this.range = range;
            this.tag = tag;
            this.color = color;
        }
    }

    private static final String[] DIRS = {
            "önünde", "sağ önünde", "sağında", "sağ arkanda",
            "arkanda", "sol arkanda", "solunda", "sol önünde"
    };

    public OreCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        ItemStack stack = player.getStackInHand(hand);
        OreMode[] modes = OreMode.values();
        int idx = getMode(stack);

        // Shift'siz sağ tık: modu değiştir. Shift + sağ tık: sadece tara.
        if (!player.isSneaking()) {
            idx = (idx + 1) % modes.length;
            setMode(stack, idx);
        }
        OreMode mode = modes[idx];

        BlockPos found = findNearest(serverWorld, player.getBlockPos(), mode);

        Text prefix = Text.literal("[" + mode.label + " modu] ").formatted(mode.color);
        Text msg;
        if (found == null) {
            msg = prefix.copy().append(Text.literal(mode.range + " blok içinde yok").formatted(Formatting.WHITE));
        } else {
            msg = prefix.copy().append(Text.literal(describe(player, found)).formatted(Formatting.WHITE));
        }
        player.sendMessage(msg, true); // action bar

        player.getItemCooldownManager().set(stack, 10);
        return ActionResult.SUCCESS;
    }

    private static BlockPos findNearest(ServerWorld world, BlockPos origin, OreMode mode) {
        int r = mode.range;
        long r2 = (long) r * r;
        int minY = Math.max(world.getBottomY(), origin.getY() - r);
        int maxY = Math.min(world.getBottomY() + world.getHeight() - 1, origin.getY() + r);

        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockPos best = null;
        long bestD2 = Long.MAX_VALUE;

        for (int x = origin.getX() - r; x <= origin.getX() + r; x++) {
            for (int z = origin.getZ() - r; z <= origin.getZ() + r; z++) {
                // Yüklü olmayan chunk'ları atla (yoksa dünya generate eder / lag yapar)
                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;

                long dx = x - origin.getX();
                long dz = z - origin.getZ();
                long dxz = dx * dx + dz * dz;
                if (dxz > r2) continue;

                for (int y = minY; y <= maxY; y++) {
                    long dy = y - origin.getY();
                    long d2 = dxz + dy * dy;
                    if (d2 > r2 || d2 >= bestD2) continue;

                    BlockState state = world.getBlockState(m.set(x, y, z));
                    if (state.isIn(mode.tag)) {
                        bestD2 = d2;
                        best = m.toImmutable();
                    }
                }
            }
        }
        return best;
    }

    private static String describe(PlayerEntity player, BlockPos ore) {
        double dx = ore.getX() + 0.5 - player.getX();
        double dy = ore.getY() + 0.5 - player.getY();
        double dz = ore.getZ() + 0.5 - player.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));

        float yaw = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double fwd = dx * -MathHelper.sin(yaw) + dz * MathHelper.cos(yaw);
        double right = dx * -MathHelper.cos(yaw) + dz * -MathHelper.sin(yaw);

        StringBuilder sb = new StringBuilder();
        sb.append(dist).append(" blok, ");

        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz >= 2.0) {
            double ang = Math.toDegrees(Math.atan2(right, fwd)); // 0 = ileri, +90 = sağ
            int sector = (int) (((ang + 22.5 + 360.0) % 360.0) / 45.0);
            sb.append(DIRS[sector]);
        } else {
            sb.append(dy > 0 ? "tam üstünde" : "tam altında");
        }

        if (horiz >= 2.0) {
            if (dy >= 3) sb.append(", ").append((int) Math.round(dy)).append(" blok yukarıda");
            else if (dy <= -3) sb.append(", ").append((int) Math.round(-dy)).append(" blok aşağıda");
        }
        return sb.toString();
    }

    private static int getMode(ItemStack stack) {
        NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (c == null) return 0;
        int v = c.copyNbt().getInt("mode", 0);
        return Math.floorMod(v, OreMode.values().length);
    }

    private static void setMode(ItemStack stack, int mode) {
        NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = c == null ? new NbtCompound() : c.copyNbt();
        nbt.putInt("mode", mode);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}
