package com.zabyt.lib.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;


public interface DraggableHud {


    net.minecraft.network.chat.Component dragTitle();

    int xOffset();

    int yOffset();

    void setOffsets(int x, int y);


    void drawPreview(GuiGraphicsExtractor graphics, Minecraft minecraft, int screenWidth, int screenHeight);


    void applySnapping(int screenWidth, int screenHeight);


    void drawGuides(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight);
}
