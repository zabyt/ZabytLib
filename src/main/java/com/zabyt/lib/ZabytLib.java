package com.zabyt.lib;

import com.mojang.blaze3d.platform.NativeImage;
import com.zabyt.lib.config.ZabytLibConfig;
import com.zabyt.lib.screen.TextureIconButton;
import com.zabyt.lib.screen.ZabytLibAboutScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ZabytLib implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("zabytlib");

    private static final Identifier MENU_ICON_TEXTURE =
            Identifier.fromNamespaceAndPath("zabytlib", "menu_icon");
    private static boolean menuTextureReady = false;
    private static final java.util.Set<String> CONFIG_USERS = new java.util.TreeSet<>();
    private static final java.util.Map<String, Identifier> MOD_ICON_TEXTURES = new java.util.HashMap<>();


    public static Identifier modIconTexture(Minecraft minecraft, String modId) {
        Identifier cached = MOD_ICON_TEXTURES.get(modId);
        if (cached != null) {
            return cached;
        }
        var container = FabricLoader.getInstance().getModContainer(modId).orElse(null);
        if (container == null || !container.getMetadata().getIconPath(64).isPresent()) {
            return null;
        }
        String iconPath = container.getMetadata().getIconPath(64).get();
        Identifier iconId = toAssetIdentifier(modId, iconPath);
        if (iconId == null) {
            return null;
        }
        try (InputStream in = minecraft.getResourceManager().getResource(iconId)
                .orElseThrow(java.util.NoSuchElementException::new).open()) {
            com.mojang.blaze3d.platform.NativeImage image = com.mojang.blaze3d.platform.NativeImage.read(in);
            Identifier textureId = Identifier.fromNamespaceAndPath("zabytlib", "mod_icon/" + modId);
            Supplier<String> label = () -> "zabytlib mod icon: " + modId;
            minecraft.getTextureManager().register(textureId, new net.minecraft.client.renderer.texture.DynamicTexture(label, image));
            MOD_ICON_TEXTURES.put(modId, textureId);
            return textureId;
        } catch (Exception e) {
            LOGGER.warn("Could not load icon for mod {}", modId, e);
            return null;
        }
    }


    private static Identifier toAssetIdentifier(String modId, String iconPath) {
        String path = iconPath;
        if (path.startsWith("assets/")) {
            path = path.substring("assets/".length());
        }
        int slash = path.indexOf('/');
        if (slash > 0 && slash < path.length() - 1) {
            return Identifier.fromNamespaceAndPath(path.substring(0, slash), path.substring(slash + 1));
        }
        return Identifier.fromNamespaceAndPath(modId, path);
    }


    public static void registerConfigUser(String modId) {
        CONFIG_USERS.add(modId);
    }

    public static java.util.Set<String> getConfigUsers() {
        java.util.LinkedHashSet<String> users = new java.util.LinkedHashSet<>();
        net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();
        for (net.fabricmc.loader.api.ModContainer container : loader.getAllMods()) {
            String id = container.getMetadata().getId();
            boolean dependsOnLib = container.getMetadata().getDependencies().stream()
                    .anyMatch(dep -> dep.getKind() == net.fabricmc.loader.api.metadata.ModDependency.Kind.DEPENDS
                            && dep.getModId().equals("zabytlib"));
            if (dependsOnLib) {
                users.add(id);
            }
        }
        for (String id : CONFIG_USERS) {
            if (!id.equals("zabytlib")) {
                users.add(id);
            }
        }
        return java.util.Collections.unmodifiableSet(users);
    }


    public static boolean hasActiveConfigUsers() {
        for (String id : CONFIG_USERS) {
            if (!id.equals("zabytlib")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen) && !(screen instanceof PauseScreen)) {
                return;
            }
            if (!hasActiveConfigUsers()) {
                return;
            }
            ZabytLibConfig cfg = ZabytLibConfig.get();
            if (!cfg.menuIconEnabled || !ensureMenuIconTexture(client)) {
                return;
            }
            var widgets = Screens.getWidgets(screen);
            int[] spot = alignIconRow(widgets);
            int size, x, y;
            if (spot != null) {
                size = spot[0];
                x = spot[1];
                y = spot[2];
            } else {
                size = Math.max(12, cfg.menuIconSize);
                int margin = Math.max(0, cfg.menuIconMargin);
                x = switch (cfg.menuIconCorner) {
                    case "top_right", "bottom_right" -> scaledWidth - size - margin;
                    default -> margin;
                };
                y = switch (cfg.menuIconCorner) {
                    case "bottom_left", "bottom_right" -> scaledHeight - size - margin;
                    default -> margin;
                };
            }
            Screen current = screen;
            widgets.add(new TextureIconButton(x, y, size, MENU_ICON_TEXTURE,
                    () -> client.gui.setScreen(new ZabytLibAboutScreen(current))));
        });
        LOGGER.info("ZabytLib initialized");
    }


    private static int[] alignIconRow(List<AbstractWidget> widgets) {
        List<AbstractWidget> squares = new ArrayList<>();
        for (AbstractWidget w : widgets) {
            if (w instanceof TextureIconButton) {
                continue;
            }
            if (w.getHeight() >= 12 && w.getHeight() <= 30
                    && Math.abs(w.getWidth() - w.getHeight()) <= 2) {
                squares.add(w);
            }
        }
        if (squares.isEmpty()) {
            LOGGER.info("zabytlib icon-row: no square widgets found ({} total)", widgets.size());
            return null;
        }
        squares.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
        List<List<AbstractWidget>> clusters = new ArrayList<>();
        List<AbstractWidget> current = null;
        int currentY = Integer.MIN_VALUE;
        for (AbstractWidget w : squares) {
            if (current == null || w.getY() - currentY > 3) {
                current = new ArrayList<>();
                clusters.add(current);
            }
            current.add(w);
            currentY = w.getY();
        }
        List<AbstractWidget> row = null;
        for (List<AbstractWidget> c : clusters) {
            if (row == null
                    || c.size() > row.size()
                    || (c.size() == row.size() && c.get(0).getY() > row.get(0).getY())) {
                row = c;
            }
        }
        if (row == null || row.isEmpty()) {
            return null;
        }
        int size = row.get(0).getHeight();
        int gap = 4;
        int m = row.size() + 1;
        int totalWidth = m * size + (m - 1) * gap;
        int leftmost = row.stream().mapToInt(AbstractWidget::getX).min().orElseThrow();
        int rightEnd = row.stream().mapToInt(w -> w.getX() + w.getWidth()).max().orElseThrow();
        int centerY = row.get(0).getY();
        int startX = (leftmost + rightEnd) / 2 - totalWidth / 2;
        row.sort((a, b) -> Integer.compare(a.getX(), b.getX()));
        int cursor = startX;
        for (AbstractWidget w : row) {
            w.setX(cursor);
            w.setWidth(size);
            w.setHeight(size);
            cursor += size + gap;
        }
        return new int[]{size, cursor, centerY};
    }

    public static Path iconFile() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(ZabytLibConfig.MOD_ID).resolve("menu-icon.png");
    }


    public static void setIconImage(Minecraft minecraft, NativeImage image) {
        AbstractTexture old = minecraft.getTextureManager().getTexture(MENU_ICON_TEXTURE);
        Supplier<String> label = () -> "zabytlib menu icon";
        minecraft.getTextureManager().register(MENU_ICON_TEXTURE, new DynamicTexture(label, image));
        menuTextureReady = true;
        if (old != null) {
            old.close();
        }
    }


    public static void applyAndSaveIcon(Minecraft minecraft, NativeImage image) {
        try {
            Files.createDirectories(iconFile().getParent());
            image.writeToFile(iconFile());
        } catch (Exception e) {
            LOGGER.warn("Could not save menu icon file", e);
        }
        setIconImage(minecraft, image);
    }

    public static void resetIcon(Minecraft minecraft) {
        try {
            Files.deleteIfExists(iconFile());
        } catch (Exception e) {
            LOGGER.warn("Could not delete custom menu icon", e);
        }
        menuTextureReady = false;
        ensureMenuIconTexture(minecraft);
    }


    public static void fetchPlayerFace(Minecraft minecraft, String username, Consumer<String> status) {
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient http = HttpClient.newHttpClient();
                String safeName = URLEncoder.encode(username, StandardCharsets.UTF_8);
                HttpRequest req = HttpRequest.newBuilder(
                                URI.create("https://mc-heads.net/avatar/" + safeName + "/64")).build();
                HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200 && resp.body().length > 0) {
                    return NativeImage.read(new java.io.ByteArrayInputStream(resp.body()));
                }
            } catch (Exception e) {
                LOGGER.warn("Face fetch failed for {}", username, e);
            }
            return null;
        }).whenComplete((img, err) -> minecraft.execute(() -> {
            if (img != null && err == null) {
                applyAndSaveIcon(minecraft, img);
                status.accept("Icon set to " + username + "'s face");
            } else {
                status.accept("Failed to fetch face (offline or unknown player)");
            }
        }));
    }

    public static boolean ensureMenuIconTexture(Minecraft minecraft) {
        if (menuTextureReady) {
            return true;
        }
        NativeImage image = null;
        Path custom = iconFile();
        if (Files.exists(custom)) {
            try (InputStream in = Files.newInputStream(custom)) {
                image = NativeImage.read(in);
            } catch (Exception e) {
                LOGGER.warn("Could not read custom menu icon {}, falling back to bundled", custom, e);
            }
        }
        if (image == null) {
            try (InputStream in = Minecraft.class.getResourceAsStream("/assets/zabytlib/icon.png")) {
                if (in != null) {
                    image = NativeImage.read(in);
                }
            } catch (Exception e) {
                LOGGER.error("Could not read bundled menu icon", e);
            }
        }
        if (image == null) {
            return false;
        }
        Supplier<String> label = () -> "zabytlib menu icon";
        minecraft.getTextureManager().register(MENU_ICON_TEXTURE, new DynamicTexture(label, image));
        menuTextureReady = true;
        return true;
    }
}
