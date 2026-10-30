package com.zabyt.lib.hud;


public final class HudColors {

    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF5555;
    public static final int DARK_RED = 0xFFCC3333;
    public static final int ORANGE = 0xFFFFAA00;
    public static final int GUIDE_LINE = 0x80FFFFFF;

    private HudColors() {
    }


    public static int argb(int alpha, int rgb) {
        return (clamp8(alpha) << 24) | (rgb & 0xFFFFFF);
    }


    public static int pulseAlpha(long now, long periodMs, int minAlpha, int maxAlpha) {
        float phase = Math.floorMod(now, periodMs) / (float) periodMs;
        float triangle = phase < 0.5f ? phase * 2f : 2f - phase * 2f;
        return clamp8(minAlpha + Math.round((maxAlpha - minAlpha) * triangle));
    }


    public static int pulsingRed(long now, long periodMs) {
        return argb(pulseAlpha(now, periodMs, 40, 220), 0xFF0000);
    }


    public static int steadyRedTint() {
        return argb(89, 0xFF0000);
    }

    private static int clamp8(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
