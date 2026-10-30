package com.zabyt.lib.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;


public final class ScreenZones {


    public record Zone(int x, int y, int width, int height) {

        public boolean intersects(int px, int py, int pw, int ph) {
            return px < x + width && x < px + pw && py < y + height && y < py + ph;
        }
    }

    private static final int HOTBAR_HALF = 91;
    private static final int OFFHAND_WIDTH = 29;
    private static final int BAR_HEIGHT = 22;

    private ScreenZones() {
    }


    public static List<Zone> vanillaZones(Minecraft mc) {
        List<Zone> zones = new ArrayList<>();
        LocalPlayer player = mc.player;
        if (player == null || mc.getWindow() == null) {
            return zones;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;


        zones.add(new Zone(cx - HOTBAR_HALF, h - BAR_HEIGHT, HOTBAR_HALF * 2 + 1, BAR_HEIGHT));


        if (!player.getOffhandItem().isEmpty()) {
            boolean offhandRight = player.getMainArm().getOpposite() == net.minecraft.world.entity.HumanoidArm.RIGHT;
            int ox = offhandRight ? cx + HOTBAR_HALF : cx - HOTBAR_HALF - OFFHAND_WIDTH;
            zones.add(new Zone(ox, h - 23, OFFHAND_WIDTH, 24));
        }


        var effects = player.getActiveEffects();
        if (!effects.isEmpty()) {
            int beneficial = 0;
            int harmful = 0;
            for (var inst : effects) {
                if (inst.showIcon()) {
                    if (inst.getEffect().value().isBeneficial()) {
                        beneficial++;
                    } else {
                        harmful++;
                    }
                }
            }
            int columns = Math.max(beneficial, harmful);
            if (columns > 0) {
                zones.add(new Zone(w - columns * 25, 1, columns * 25,
                        beneficial > 0 && harmful > 0 ? 1 + 26 + 26 : 1 + 26));
            }
        }
        return zones;
    }
}
