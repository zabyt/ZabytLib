package com.zabyt.lib.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;


public class TextureIconButton extends AbstractWidget {

    private final Identifier textureId;
    private final Runnable onPress;

    public TextureIconButton(int x, int y, int size, Identifier textureId, Runnable onPress) {
        super(x, y, size, size, Component.empty());
        this.textureId = textureId;
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float alpha = getAlpha();
        if (alpha <= 0.01f) {
            return;
        }
        int ai = (int) (alpha * 255f);
        int x = getX();
        int y = getY();
        int borderRgb = isHoveredOrFocused() ? 0xFFFFFF : 0x101010;
        int borderA = (isHoveredOrFocused() ? ai : (0xB3 * ai) / 255);
        int border = (borderA << 24) | borderRgb;
        int bgColor = ((0x66 * ai) / 255) << 24;
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, bgColor);
        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId,
                x + 2, y + 2, 0f, 0f, width - 4, height - 4,
                width - 4, height - 4, width - 4, height - 4, (ai << 24) | 0xFFFFFF);
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
