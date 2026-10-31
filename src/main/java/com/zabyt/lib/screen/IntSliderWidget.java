package com.zabyt.lib.screen;

import java.util.function.DoubleConsumer;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class IntSliderWidget extends AbstractSliderButton {

    private final String label;
    private final int min;
    private final int max;
    private final DoubleConsumer onChange;

    public IntSliderWidget(int x, int y, int w, int h, String label, int min, int max, int current,
            DoubleConsumer onChange) {
        super(x, y, w, h, Component.empty(),
                (Math.clamp(current, min, max) - min) / (double) Math.max(1, max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        int v = min + (int) Math.round(value * (max - min));
        setMessage(Component.literal(label + ": " + v));
    }

    @Override
    protected void applyValue() {
        int v = min + (int) Math.round(value * (max - min));
        onChange.accept(v);
    }
}
