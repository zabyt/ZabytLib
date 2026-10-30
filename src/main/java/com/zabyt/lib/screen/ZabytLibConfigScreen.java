package com.zabyt.lib.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.zabyt.lib.ZabytLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Files;

public class ZabytLibConfigScreen extends Screen {

    private static final int PREVIEW_SIZE = 48;

    private final Screen parent;
    private EditBox userBox;
    private String status = "";
    private String statusColor = "gray";

    public ZabytLibConfigScreen(Screen parent) {
        super(Component.literal("ZabytLib Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ZabytLib.ensureMenuIconTexture(this.minecraft);
        int cx = this.width / 2;

        EditBox pathBox = new EditBox(this.font, cx - 150, 66, 210, 20,
                Component.literal("Icon file"));
        pathBox.setValue(ZabytLib.iconFile().getFileName().toString());
        pathBox.setEditable(false);
        this.addRenderableWidget(pathBox);

        this.addRenderableWidget(Button.builder(Component.literal("Browse image..."), b -> browseForImage())
                .bounds(cx + 64, 66, 86, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset to default"), b -> {
            ZabytLib.resetIcon(this.minecraft);
            setStatus("Reset to bundled icon", "gray");
        }).bounds(cx - 150, 92, 300, 20).build());

        userBox = new EditBox(this.font, cx - 150, 128, 210, 20,
                Component.literal("Username"));
        userBox.setMaxLength(16);
        userBox.setHint(Component.literal("Player username"));
        this.addRenderableWidget(userBox);

        this.addRenderableWidget(Button.builder(Component.literal("Use face"), b -> {
            String name = userBox.getValue().trim();
            if (!name.isEmpty()) {
                setStatus("Fetching face...", "yellow");
                ZabytLib.fetchPlayerFace(this.minecraft, name, s -> setStatus(s, "white"));
            }
        }).bounds(cx + 64, 128, 86, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - 100, this.height - 30, 200, 20).build());
    }

    private void browseForImage() {
        Minecraft mc = this.minecraft;
        Thread picker = new Thread(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose menu icon");
            chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
            File selected = chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION
                    ? chooser.getSelectedFile() : null;
            if (selected == null) {
                return;
            }
            mc.execute(() -> {
                try (var in = Files.newInputStream(selected.toPath())) {
                    NativeImage img = NativeImage.read(in);
                    ZabytLib.applyAndSaveIcon(mc, img);
                    setStatus("Icon updated", "white");
                } catch (Exception e) {
                    setStatus("Could not load that image", "red");
                }
            });
        }, "zabytlib-icon-picker");
        picker.start();
    }

    private void setStatus(String text, String color) {
        this.status = text;
        this.statusColor = color;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        graphics.centeredText(this.font, this.title, cx, 20, 0xFFFFFF);
        graphics.text(this.font, "Menu button icon:", cx - 150, 54, 0xA0A0A0);
        graphics.text(this.font, "Use a player skin face:", cx - 150, 116, 0xA0A0A0);

        if (ZabytLib.ensureMenuIconTexture(this.minecraft)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("zabytlib", "menu_icon"),
                    cx + 156, 54, 0f, 0f, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE);
            graphics.fill(cx + 155, 53, cx + 205, 103, 0x40FFFFFF);
        }

        int color = switch (statusColor) {
            case "red" -> 0xFF5555;
            case "white" -> 0xFFFFFF;
            case "yellow" -> 0xFFFF55;
            default -> 0xAAAAAA;
        };
        if (!status.isEmpty()) {
            graphics.centeredText(this.font, status, cx, 158, color);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
