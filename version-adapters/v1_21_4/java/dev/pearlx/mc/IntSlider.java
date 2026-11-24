package dev.pearlx.mc;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/** Whole-number slider used for the numeric configuration inputs. */
final class IntSlider extends AbstractSliderButton {

    private final String label;
    private final String suffix;
    private final int min;
    private final int max;
    private final IntConsumer onChange;
    private boolean initialising = true;

    IntSlider(int x, int y, int width, int height, String label, String suffix,
              int min, int max, int value, IntConsumer onChange) {
        super(x, y, width, height, Component.empty(),
                (Math.max(min, Math.min(max, value)) - min) / (double) Math.max(1, max - min));
        this.label = label;
        this.suffix = suffix;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        updateMessage();
        initialising = false;
    }

    int currentValue() {
        return (int) Math.round(min + value * (max - min));
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + ": " + currentValue() + suffix));
    }

    @Override
    protected void applyValue() {
        updateMessage();
        if (!initialising && onChange != null) {
            onChange.accept(currentValue());
        }
    }
}
