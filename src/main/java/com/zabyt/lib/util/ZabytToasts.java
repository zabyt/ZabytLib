package com.zabyt.lib.util;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class ZabytToasts {

    private static final Map<String, SystemToast.SystemToastId> TOKENS = new HashMap<>();

    private ZabytToasts() {
    }

    public static void show(Minecraft minecraft, String modId, String title, String message) {
        SystemToast.SystemToastId token = TOKENS.computeIfAbsent(modId, k -> new SystemToast.SystemToastId());
        SystemToast.addOrUpdate(minecraft.gui.toastManager(), token,
                Component.literal(title), Component.literal(message));
    }

    public static SystemToast.SystemToastId token(String key) {
        return TOKENS.computeIfAbsent(key, k -> new SystemToast.SystemToastId());
    }
}
