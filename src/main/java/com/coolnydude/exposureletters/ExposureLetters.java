package com.coolnydude.exposureletters;

import com.coolnydude.exposureletters.network.ModNetwork;
import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ExposureLetters.MOD_ID)
public class ExposureLetters {
    public static final String MOD_ID = "exposure_letters";

    public ExposureLetters(IEventBus modBus) {
        ModRegistry.register(modBus);
        modBus.addListener(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
