package com.zabyt.lib.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.zabyt.lib.ZabytLib;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ZabytLibAboutScreen extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int ICON_SIZE = ROW_HEIGHT;
    private static final int ROW_WIDTH = 220;

    private final Screen parent;

    private record Row(String modId, String name, ConfigScreenFactory<?> factory, Identifier icon) {
    }

    public ZabytLibAboutScreen(Screen parent) {
        super(Component.literal("Mods using ZabytLib"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ZabytLib.ensureMenuIconTexture(this.minecraft);
        Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        for (var container : FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class)) {
            try {
                ConfigScreenFactory<?> factory = container.getEntrypoint().getModConfigScreenFactory();
                if (factory != null) {
                    factories.put(container.getProvider().getMetadata().getId(), factory);
                }
            } catch (Throwable ignored) {
            }
        }

        List<Row> rows = new ArrayList<>();
        for (String modId : ZabytLib.getConfigUsers()) {
            String name = modId;
            var container = FabricLoader.getInstance().getModContainer(modId).orElse(null);
            if (container != null) {
                name = container.getMetadata().getName();
            }
            Identifier icon = ZabytLib.modIconTexture(this.minecraft, modId);
            if (icon == null) {
                icon = Identifier.fromNamespaceAndPath("zabytlib", "menu_icon");
            }
            rows.add(new Row(modId, name, factories.get(modId), icon));
        }
        rows.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        int x = this.width / 2 - ROW_WIDTH / 2;
        int y = 44;
        for (Row row : rows) {
            this.addRenderableWidget(new ModListRow(x, y, ROW_WIDTH, ROW_HEIGHT, row));
            y += ROW_HEIGHT + 3;
        }
        if (rows.isEmpty()) {
            y += ROW_HEIGHT;
        }

        this.addRenderableWidget(Button.builder(Component.literal("ZabytLib Settings"),
                        b -> this.minecraft.gui.setScreen(new ZabytLibConfigScreen(this)))
                .bounds(this.width / 2 + 24, this.height - 30, 96, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 120, this.height - 30, 96, 20).build());
    }

    private void openConfig(Row row) {
        Screen screen = row.factory().create(this);
        if (screen != null) {
            this.minecraft.gui.setScreen(screen);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        if (ZabytLib.getConfigUsers().isEmpty()) {
            graphics.centeredText(this.font, "No mods use this library yet", this.width / 2, 34, 0xAAAAAA);
        } else {
            graphics.centeredText(this.font, "Click a mod to edit its config", this.width / 2, 34, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    private class ModListRow extends AbstractWidget {

        private final Row row;

        private ModListRow(int x, int y, int width, int height, Row row) {
            super(x, y, width, height, Component.literal(row.name()));
            this.row = row;
        }

        private boolean configurable() {
            return this.row.factory() != null;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            float alpha = getAlpha();
            if (alpha <= 0.01f) {
                return;
            }
            if (!configurable()) {
                alpha *= 0.4f;
            }
            int ai = (int) (alpha * 255f);
            int x = getX();
            int y = getY();


            int iconBorder = isHoveredOrFocused() ? 0xFFFFFF : 0x101010;
            int iconBorderA = isHoveredOrFocused() ? ai : (0xB3 * ai) / 255;
            int ib = (iconBorderA << 24) | iconBorder;
            int iconBg = ((0x66 * ai) / 255) << 24;
            graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, iconBg);
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.row.icon(),
                    x + 1, y + 1, 0f, 0f, ICON_SIZE - 2, ICON_SIZE - 2, ICON_SIZE - 2, ICON_SIZE - 2, (ai << 24) | 0xFFFFFF);
            graphics.fill(x, y, x + ICON_SIZE, y + 1, ib);
            graphics.fill(x, y + ICON_SIZE - 1, x + ICON_SIZE, y + ICON_SIZE, ib);
            graphics.fill(x, y, x + 1, y + ICON_SIZE, ib);
            graphics.fill(x + ICON_SIZE - 1, y, x + ICON_SIZE, y + ICON_SIZE, ib);


            int boxX = x + ICON_SIZE + 3;
            int boxW = getX() + width - boxX;
            int boxBorder = isHoveredOrFocused() ? 0xFFFFFF : 0x2A2A2A;
            int boxA = isHoveredOrFocused() ? ai : (0xB3 * ai) / 255;
            int bb = (boxA << 24) | boxBorder;
            int boxBg = ((0x4C * ai) / 255) << 24;
            graphics.fill(boxX, y, boxX + boxW, y + ROW_HEIGHT, boxBg);
            graphics.fill(boxX, y, boxX + boxW, y + 1, bb);
            graphics.fill(boxX, y + ROW_HEIGHT - 1, boxX + boxW, y + ROW_HEIGHT, bb);
            graphics.fill(boxX, y, boxX + 1, y + ROW_HEIGHT, bb);
            graphics.fill(boxX + boxW - 1, y, boxX + boxW, y + ROW_HEIGHT, bb);

            int textY = y + (ROW_HEIGHT - 8) / 2 + 1;
            String label = this.row.name();
            int maxText = boxW - 8;
            graphics.enableScissor(boxX + 1, y + 1, boxX + boxW - 1, y + ROW_HEIGHT - 1);
            if (ZabytLibAboutScreen.this.font.width(label) > maxText) {
                while (label.length() > 1 && ZabytLibAboutScreen.this.font.width(label + "…") > maxText) {
                    label = label.substring(0, label.length() - 1);
                }
                label = label + "…";
            }
            graphics.text(ZabytLibAboutScreen.this.font, label, boxX + 4, textY, (ai << 24) | 0xE6E6E6, true);
            graphics.disableScissor();

            if (configurable() && isHoveredOrFocused()) {
                String hint = ">";
                graphics.text(ZabytLibAboutScreen.this.font, hint, boxX + boxW - 9, textY, (ai << 24) | 0x55FF55, true);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubled) {
            if (configurable()) {
                ZabytLibAboutScreen.this.openConfig(this.row);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
