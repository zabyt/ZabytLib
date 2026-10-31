package com.zabyt.lib.screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.zabyt.lib.config.ConfigEntry;
import com.zabyt.lib.config.SimpleConfig;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AutoConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int WIDE = 310;
    private static final int SLIM = 200;

    private final Screen parent;
    private final SimpleConfig config;
    private final String modId;

    public AutoConfigScreen(String modId, SimpleConfig config, Screen parent) {
        super(Component.literal(configTitle(modId)));
        this.modId = modId;
        this.config = config;
        this.parent = parent;
    }

    private static String configTitle(String modId) {
        String modName = modId;
        var container = FabricLoader.getInstance().getModContainer(modId).orElse(null);
        if (container != null && !container.getMetadata().getName().isBlank()) {
            modName = container.getMetadata().getName();
        }
        return modName + " Settings";
    }

    @Override
    protected void init() {
        List<Field> fields = collectFields(config.getClass());
        int usableHeight = this.height - 74;
        int rowsPerColumn = Math.max(1, usableHeight / ROW_HEIGHT);
        boolean wide = (fields.size() + rowsPerColumn - 1) / rowsPerColumn <= 1;
        int colWidth = wide ? WIDE : SLIM;
        int gap = wide ? 0 : 10;

        int row = 0;
        int col = 0;
        for (Field field : fields) {
            ConfigEntry meta = field.getAnnotation(ConfigEntry.class);
            String label = meta != null && !meta.label().isEmpty() ? meta.label() : titleCase(field.getName());
            int x = this.width / 2 - (wide ? WIDE / 2 : SLIM + gap / 2) + col * (colWidth + gap);
            int y = 40 + row * ROW_HEIGHT;
            AbstractWidget widget = widgetFor(field, meta, label, x, y, colWidth);
            if (widget != null) {
                this.addRenderableWidget(widget);
                row++;
                if (row >= rowsPerColumn) {
                    row = 0;
                    col++;
                }
            }
        }

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private List<Field> collectFields(Class<?> type) {
        List<Field> out = new ArrayList<>();
        Class<?> current = type;
        while (current != null && !current.equals(SimpleConfig.class) && !current.equals(Object.class)) {
            for (Field field : current.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(mods) || java.lang.reflect.Modifier.isTransient(mods)
                        || java.lang.reflect.Modifier.isFinal(mods)) {
                    continue;
                }
                if (!java.lang.reflect.Modifier.isPublic(mods)) {
                    continue;
                }
                Class<?> ft = field.getType();
                if (ft == boolean.class || ft == int.class || ft == long.class || ft == double.class
                        || ft == float.class || ft == String.class || ft.isEnum()) {
                    out.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return out;
    }

    private AbstractWidget widgetFor(Field field, ConfigEntry meta, String label, int x, int y, int w) {
        Class<?> ft = field.getType();
        try {
            if (ft == boolean.class) {
                boolean value = field.getBoolean(config);
                return CycleButton.onOffBuilder(value)
                        .create(x, y, w, 20, Component.literal(label),
                                (btn, val) -> setField(field, val.toString()));
            }
            if (ft.isEnum()) {
                Object current = field.get(config);
                Object[] values = ft.getEnumConstants();
                return CycleButton.<Object>builder(v -> Component.literal(titleCase(((Enum<?>) v).name().toLowerCase(java.util.Locale.ROOT))), values[0])
                        .withValues(values)
                        .create(x, y, w, 20, Component.literal(label),
                                (btn, val) -> setField(field, ((Enum<?>) val).name()));
            }
            if (ft == int.class || ft == long.class) {
                int min = meta != null ? (int) meta.min() : 0;
                int max = meta != null ? (int) meta.max() : 0;
                if (max > min && meta.slider()) {
                    int value = (int) field.get(config);
                    return new IntSliderWidget(x, y, w, 20, label, min, max, value, v -> setNumericField(field, v));
                }
                int value = (int) field.get(config);
                return numericBox(field, label, x, y, w,
                        v -> setNumericField(field, max > min ? Math.round(Math.clamp(v, min, max)) : Math.round(v)),
                        value);
            }
            if (ft == double.class || ft == float.class) {
                double min = meta != null ? meta.min() : 0.0;
                double max = meta != null ? meta.max() : 1.0;
                double value = field.getDouble(config);
                if (max > min && meta.slider()) {
                    return new DoubleSliderWidget(x, y, w, 20, label, min, max, value, "%.2f",
                            v -> setNumericField(field, v));
                }
                return numericBox(field, label, x, y, w,
                        v -> setNumericField(field, v), value);
            }
            if (ft == String.class) {
                String value = String.valueOf(field.get(config));
                EditBox box = new EditBox(this.font, x, y, w, 20, Component.literal(label));
                box.setValue(value);
                box.setMaxLength(4096);
                box.setHint(Component.literal(label));
                box.setResponder(v -> setField(field, v));
                return box;
            }
        } catch (Exception e) {
            com.zabyt.lib.ZabytLib.LOGGER.warn("Could not add config widget for {}", field, e);
        }
        return null;
    }

    private EditBox numericBox(Field field, String label, int x, int y, int w,
            java.util.function.Consumer<Double> setter, double value) {
        EditBox box = new EditBox(this.font, x, y, w, 20, Component.literal(label));
        box.setValue(formatValue(value));
        box.setHint(Component.literal(label));
        box.setResponder(v -> {
            try {
                double parsed = Double.parseDouble(v.trim());
                setter.accept(parsed);
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    private String formatValue(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void setNumericField(Field field, double value) {
        try {
            Class<?> ft = field.getType();
            if (ft == int.class) {
                field.setInt(config, (int) Math.round(value));
            } else if (ft == long.class) {
                field.setLong(config, (long) Math.round(value));
            } else if (ft == double.class) {
                field.setDouble(config, value);
            } else if (ft == float.class) {
                field.setFloat(config, (float) value);
            }
        } catch (Exception e) {
            com.zabyt.lib.ZabytLib.LOGGER.warn("Could not apply config value to {}", field, e);
        }
    }

    private void setField(Field field, String jsonishValue) {
        try {
            Class<?> ft = field.getType();
            if (ft == boolean.class) {
                field.setBoolean(config, Boolean.parseBoolean(jsonishValue));
            } else if (ft == int.class) {
                field.setInt(config, Integer.parseInt(jsonishValue));
            } else if (ft == long.class) {
                field.setLong(config, Long.parseLong(jsonishValue));
            } else if (ft == double.class) {
                field.setDouble(config, Double.parseDouble(jsonishValue));
            } else if (ft == float.class) {
                field.setFloat(config, Float.parseFloat(jsonishValue));
            } else if (ft == String.class) {
                field.set(config, jsonishValue);
            } else if (ft.isEnum()) {
                for (Object constant : ft.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equals(jsonishValue)) {
                        field.set(config, constant);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            com.zabyt.lib.ZabytLib.LOGGER.warn("Could not apply config value to {}", field, e);
        }
    }

    private static String titleCase(String text) {
        StringBuilder out = new StringBuilder();
        for (String word : text.replace('_', ' ').split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.length() > 0 ? out.toString() : text;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        config.save();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        config.save();
    }
}
