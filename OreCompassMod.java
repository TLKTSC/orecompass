package com.example.orecompass;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class OreCompassMod implements ModInitializer {
    public static final String MOD_ID = "orecompass";
    public static Item ORE_COMPASS;

    @Override
    public void onInitialize() {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "ore_compass"));
        ORE_COMPASS = Registry.register(Registries.ITEM, key,
                new OreCompassItem(new Item.Settings().registryKey(key).maxCount(1)));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ORE_COMPASS));
    }
}
