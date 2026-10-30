package com.zabyt.lib.hud;


public final class HudTimes {

    private HudTimes() {
    }


    public static String mmss(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }


    public static String ticksMmss(int ticks) {
        return mmss((int) Math.ceil(ticks / 20.0));
    }
}
