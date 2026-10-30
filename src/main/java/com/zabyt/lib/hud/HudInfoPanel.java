package com.zabyt.lib.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;


public class HudInfoPanel {


    public record Line(Component text, int color) {
        public static Line of(Component text, int color) {
            return new Line(text, color);
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private boolean enabled = true;

    public void setLines(List<Line> newLines) {
        lines.clear();
        lines.addAll(newLines);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }


    public int[] render(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (!enabled || lines.isEmpty() || minecraft.player == null || minecraft.level == null) {
            return null;
        }
        var font = minecraft.font;

        int widest = 0;
        for (Line line : lines) {
            widest = Math.max(widest, font.width(line.text()));
        }
        int lineH = font.lineHeight + 1;
        int pad = 3;
        int panelW = widest + pad * 2 + 1;
        int panelH = lines.size() * lineH - 1 + pad * 2;

        int[] pos = AutoPlacer.place(ScreenZones.vanillaZones(minecraft),
                AutoPlacer.bottomRowCandidates(),
                graphics.guiWidth(), graphics.guiHeight(), panelW, panelH);

        graphics.fill(pos[0], pos[1], pos[0] + panelW, pos[1] + panelH, 0x90000000);
        int y = pos[1] + pad;
        for (Line line : lines) {
            graphics.text(font, line.text(), pos[0] + pad, y, line.color(), true);
            y += lineH;
        }
        return pos;
    }
}
