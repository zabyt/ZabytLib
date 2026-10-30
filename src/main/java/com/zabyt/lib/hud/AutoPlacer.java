package com.zabyt.lib.hud;

import java.util.List;


public final class AutoPlacer {


    public record Candidate(int horizontal, int vertical, int gapX) {
    }

    private AutoPlacer() {
    }


    public static List<Candidate> bottomRowCandidates() {
        return List.of(
                new Candidate(1, 0, 4),
                new Candidate(-1, 0, 4),
                new Candidate(1, -1, 8),
                new Candidate(-1, -1, 8)
        );
    }


    public static int[] place(List<ScreenZones.Zone> zones, List<Candidate> candidates,
                              int screenWidth, int screenHeight, int widgetW, int widgetH) {
        int cx = screenWidth / 2;
        int hotbarHalf = 91;
        int[] fallback = {screenWidth - widgetW - 4, screenHeight - widgetH - 4};

        for (Candidate c : candidates) {
            int x;
            if (c.horizontal() > 0) {
                x = cx + hotbarHalf + c.gapX();
            } else if (c.horizontal() < 0) {
                x = cx - hotbarHalf - c.gapX() - widgetW;
            } else {
                x = cx - widgetW / 2;
            }
            int y = c.vertical() >= 0
                    ? screenHeight - widgetH - Math.abs(c.vertical())
                    : Math.abs(c.vertical());
            x = Math.max(2, Math.min(screenWidth - widgetW - 2, x));
            y = Math.max(2, Math.min(screenHeight - widgetH - 2, y));

            boolean clear = true;
            for (ScreenZones.Zone z : zones) {
                if (z.intersects(x, y, widgetW, widgetH)) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                return new int[]{x, y};
            }
        }
        return fallback;
    }
}
