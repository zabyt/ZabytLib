package com.zabyt.lib.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;

public class ZabytLibConfig extends SimpleConfig {

    public static final String MOD_ID = "zabytlib";

    private static ZabytLibConfig instance;

    public boolean menuIconEnabled = true;
    public String menuIconCorner = "top_left";
    public int menuIconSize = 24;
    public int menuIconMargin = 4;
    public String infoTitle = "ZabytLib";
    public List<String> infoLines = new ArrayList<>(List.of(
            "zabyt's hud stuff",
            "Put your photo in config/zabytlib/menu-icon.png",
            "Edit this text in config/zabytlib.json"
    ));

    protected ZabytLibConfig() {
        super(MOD_ID);
    }

    public java.util.List<Component> infoComponents() {
        List<Component> out = new ArrayList<>();
        for (String line : infoLines) {
            out.add(Component.literal(line));
        }
        return out;
    }

    public static ZabytLibConfig get() {
        if (instance == null) {
            instance = load(MOD_ID, ZabytLibConfig.class);
        }
        return instance;
    }
}
