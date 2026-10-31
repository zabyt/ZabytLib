package com.zabyt.lib.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zabyt.lib.ZabytLib;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class ModrinthUpdateChecker {

    private static final Set<String> CHECKED = new HashSet<>();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private ModrinthUpdateChecker() {
    }

    public static void check(Minecraft minecraft, String modId, String projectId) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment() || !CHECKED.add(modId)) {
            return;
        }
        String current = FabricLoader.getInstance().getModContainer(modId)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
        if (current == null || current.isBlank()) {
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://api.modrinth.com/v2/project/" + projectId + "/version"))
                        .header("User-Agent", "zabytlib/1.0 (github.com/zabyt)")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonArray versions = JsonParser.parseString(resp.body()).getAsJsonArray();
                    for (JsonElement element : versions) {
                        JsonObject version = element.getAsJsonObject();
                        if (version.get("version_type").getAsString().equals("release")) {
                            return version.get("version_number").getAsString();
                        }
                    }
                    if (!versions.isEmpty()) {
                        return versions.get(0).getAsJsonObject().get("version_number").getAsString();
                    }
                }
            } catch (Exception e) {
                ZabytLib.LOGGER.debug("Update check failed for {}", modId, e);
            }
            return null;
        }).whenComplete((latest, err) -> {
            if (latest != null && err == null && isNewer(current, latest)) {
                minecraft.execute(() -> ZabytToasts.show(minecraft, "zabytlib-update-" + modId,
                        "Update available",
                        modTitle(modId) + " v" + latest + " is out (you have v" + current + ")"));
            }
        });
    }

    private static String modTitle(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(c -> c.getMetadata().getName())
                .filter(n -> n != null && !n.isBlank())
                .orElse(modId);
    }

    static boolean isNewer(String current, String latest) {
        if (current.equals(latest)) {
            return false;
        }
        long[] c = parseNumber(current);
        long[] l = parseNumber(latest);
        if (c == null || l == null) {
            return false;
        }
        for (int i = 0; i < Math.max(c.length, l.length); i++) {
            long cv = i < c.length ? c[i] : 0;
            long lv = i < l.length ? l[i] : 0;
            if (cv != lv) {
                return lv > cv;
            }
        }
        return false;
    }

    private static long[] parseNumber(String version) {
        String core = version.split("[-+ ]")[0];
        String[] parts = core.split("\\.");
        long[] out = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Long.parseLong(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return out;
    }
}
