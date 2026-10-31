package com.zabyt.lib.screen;

import java.util.function.DoubleConsumer;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class DoubleSliderWidget extends AbstractSliderButton {

    private final String label;
    private final double min;
    private final double max;
    private final DoubleConsumer onChange;
    private final String format;

    public DoubleSliderWidget(int x, int y, int w, int h, String label, double min, double max, double current,
            String format, DoubleConsumer onChange) {
        super(x, y, w, h, Component.empty(),
                (current - min) / Math.max(1.0E-6, max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        this.format = format;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        double v = min + value * (max - min);
        setMessage(Component.literal(label + ": " + String.format(format, v)));
    }

    @Override
    protected void applyValue() {
        double v = min + value * (max - min);
        onChange.accept(v);
    }
}
