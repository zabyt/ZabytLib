package com.zabyt.lib.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;


public class HudDragScreen extends Screen {

    protected static final int SNAP = 7;

    private final Screen parent;
    private final DraggableHud target;
    private boolean dragging;
    private double grabDx;
    private double grabDy;

    public HudDragScreen(Screen parent, DraggableHud target) {
        super(target.dragTitle());
        this.parent = parent;
        this.target = target;
    }


    protected void onActiveChanged(boolean active) {
    }

    @Override
    protected void init() {
        onActiveChanged(true);
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
        target.applySnapping(this.width, this.height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent evt, boolean doubled) {
        if (super.mouseClicked(evt, doubled)) {
            return true;
        }
        if (evt.button() == 0) {
            dragging = true;
            grabDx = evt.x() - target.xOffset();
            grabDy = evt.y() - target.yOffset();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent evt, double dragX, double dragY) {
        if (super.mouseDragged(evt, dragX, dragY)) {
            return true;
        }
        if (dragging && evt.button() == 0) {
            target.setOffsets((int) Math.round(evt.x() - grabDx), (int) Math.round(evt.y() - grabDy));
            target.applySnapping(this.width, this.height);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent evt) {
        dragging = false;
        return super.mouseReleased(evt);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(this.font,
                Component.literal("Drag anywhere to move the widget"),
                this.width / 2, 12, 0xFFFFFF);

        target.drawGuides(graphics, this.width, this.height);
        target.drawPreview(graphics, this.minecraft, this.width, this.height);

        Component readout = Component.literal(
                String.format("X: %+d   Y: %+d", target.xOffset(), target.yOffset()));
        graphics.centeredText(this.font, readout, this.width / 2, 26, 0xA0FFFFFF);
    }

    @Override
    public void onClose() {
        onActiveChanged(false);
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        onActiveChanged(false);
    }
}
