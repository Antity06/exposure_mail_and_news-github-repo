package com.coolnydude.exposureletters.event;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CommonEvents {
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Disabled: Notes are crafted from Paper in crafting grids (2x2 or 3x3) and back.
    }

    private CommonEvents() {}
}
