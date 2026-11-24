package dev.pearlx.mc;

import dev.pearlx.core.PearlXConfig;
import dev.pearlx.core.PearlXCore;
import dev.pearlx.core.TimeUnit;
import dev.pearlx.core.WorldKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Pearl X configuration screen.
 *
 * <p>The layout is built from vanilla widgets only (checkboxes, sliders, cycle buttons), so it
 * inherits the game's own look, key handling, tooltips and narration support. Every tab maps one to
 * one onto the categories documented in the README.</p>
 */
public final class PearlXConfigScreen extends Screen {

    private static final int ROW = 24;
    private static final int WIDGET_HEIGHT = 20;

    private enum Tab {
        GENERAL("General"),
        TRACKER("Tracker"),
        ANIMATION("Animation"),
        STORAGE("Storage"),
        ADVANCED("Advanced");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private final Screen parent;
    private Tab tab = Tab.GENERAL;

    public PearlXConfigScreen(Screen parent) {
        super(Component.literal("Pearl X"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        PearlXCore core = PearlXClient.core();
        if (core == null) {
            addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                    .bounds(width / 2 - 100, height - 26, 200, WIDGET_HEIGHT).build());
            return;
        }

        PearlXConfig config = core.config();
        int panelWidth = Math.min(width - 16, 400);
        int left = (width - panelWidth) / 2;
        int tabWidth = panelWidth / Tab.values().length;

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab candidate = tabs[i];
            Button button = Button.builder(Component.literal(candidate.label),
                            pressed -> {
                                tab = candidate;
                                init();
                            })
                    .bounds(left + i * tabWidth, 26, tabWidth - 2, 18)
                    .build();
            if (candidate == tab) {
                button.active = false;
            }
            addRenderableWidget(button);
        }

        int top = 54;
        switch (tab) {
            case GENERAL -> buildGeneral(core, config, left, top, panelWidth);
            case TRACKER -> buildTracker(config, left, top, panelWidth);
            case ANIMATION -> buildAnimation(config, left, top, panelWidth);
            case STORAGE -> buildStorage(core, left, top, panelWidth);
            case ADVANCED -> buildAdvanced(core, config, left, top, panelWidth);
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, WIDGET_HEIGHT).build());
    }

    private void buildGeneral(PearlXCore core, PearlXConfig config, int left, int y, int width) {
        addCheckbox(left, y, "Enable Pearl X",
                "Master switch. Off stops tracking, saving and animations completely.",
                config.enabled, value -> config.enabled = value);
        addCheckbox(left, y + ROW, "Enable Pearl Location Tracker",
                "Feature One: track thrown Ender Pearls and save their locations locally.",
                config.trackerEnabled, value -> config.trackerEnabled = value);
        addCheckbox(left, y + ROW * 2, "Enable Universal Pearl Travel Animation",
                "Feature Two: play the pearl travel animation. Independent from tracking.",
                config.animationEnabled, value -> config.animationEnabled = value);

        WorldKey world = core.currentWorld();
        String worldLabel = world == null ? "No world loaded" : "Current " + world.label();
        addInfo(left, y + ROW * 3, width, worldLabel);
    }

    private void buildTracker(PearlXConfig config, int left, int y, int width) {
        addCheckbox(left, y, "Enable pearl detection",
                "Detect Ender Pearls thrown by you and follow them.",
                config.detectionEnabled, value -> config.detectionEnabled = value);
        addCheckbox(left, y + ROW, "Enable automatic location saving",
                "Save tracked locations on Feature One's own timer. Tracking keeps running when off.",
                config.autoSaveEnabled, value -> config.autoSaveEnabled = value);

        int cycleWidth = 96;
        addSlider(left, y + ROW * 2, width - cycleWidth - 6, "Saving interval", " "
                        + config.savingIntervalUnit.label(),
                1, config.savingIntervalUnit.maxValue(), config.savingIntervalValue,
                "How often active pearl locations are written to disk. Feature One only.",
                value -> config.savingIntervalValue = value);
        addUnitCycle(left + width - cycleWidth, y + ROW * 2, cycleWidth, config.savingIntervalUnit,
                unit -> {
                    config.savingIntervalUnit = unit;
                    config.savingIntervalValue = PearlXConfig.clamp(config.savingIntervalValue, 1, unit.maxValue());
                });

        addCheckbox(left, y + ROW * 3, "Track multiplayer servers",
                "Keep separate records for every server you play on.",
                config.trackServers, value -> config.trackServers = value);
        addCheckbox(left, y + ROW * 4, "Track single-player worlds",
                "Keep separate records for every single-player world.",
                config.trackSingleplayer, value -> config.trackSingleplayer = value);

        addSlider(left, y + ROW * 5, width, "Keep unloaded pearl records", " min",
                1, 180, config.staleRecordMinutes,
                "How long a pearl in an unloaded chunk is remembered before its record expires.",
                value -> config.staleRecordMinutes = value);
    }

    private void buildAnimation(PearlXConfig config, int left, int y, int width) {
        addCheckbox(left, y, "Enable animation feature",
                "Feature Two master switch. Works independently from the location tracker.",
                config.animationEnabled, value -> config.animationEnabled = value);
        addCheckbox(left, y + ROW, "Enable visual effects",
                "Particle effects around each animated pearl. Sound can stay on without visuals.",
                config.visualEffectsEnabled, value -> config.visualEffectsEnabled = value);

        int cycleWidth = 96;
        addSlider(left, y + ROW * 2, width - cycleWidth - 6, "Trigger delay", " "
                        + config.animationDelayUnit.label(),
                1, config.animationDelayUnit.maxValue(), config.animationDelayValue,
                "Feature Two's own timer: how long after the throw the animation starts.",
                value -> config.animationDelayValue = value);
        addUnitCycle(left + width - cycleWidth, y + ROW * 2, cycleWidth, config.animationDelayUnit,
                unit -> {
                    config.animationDelayUnit = unit;
                    config.animationDelayValue = PearlXConfig.clamp(config.animationDelayValue, 1, unit.maxValue());
                });

        addSlider(left, y + ROW * 3, width, "Animation duration", " s",
                1, 15, config.animationDurationSeconds,
                "How long the travel animation runs once triggered.",
                value -> config.animationDurationSeconds = value);

        addCheckbox(left, y + ROW * 4, "Enable sound effects",
                "Play pearl-themed sounds with the animation. Visuals can stay on without sound.",
                config.soundEffectsEnabled, value -> config.soundEffectsEnabled = value);
        addSlider(left, y + ROW * 5, width, "Sound volume", " %",
                0, 100, config.soundVolumePercent,
                "Volume of the animation sounds. 0% is silent.",
                value -> config.soundVolumePercent = value);
    }

    private void buildStorage(PearlXCore core, int left, int y, int width) {
        addAction(left, y, width, "Reset current server locations",
                "Delete the saved pearl records for the server you are on.",
                "Reset current server",
                "Delete every saved pearl location record for the server you are connected to?",
                core::resetCurrentServer);
        addAction(left, y + ROW, width, "Reset current single-player world locations",
                "Delete the saved pearl records for the single-player world you are in.",
                "Reset current world",
                "Delete every saved pearl location record for this single-player world?",
                core::resetCurrentSingleplayerWorld);
        addAction(left, y + ROW * 2, width, "Reset all server locations",
                "Delete saved records for every multiplayer server.",
                "Reset all servers",
                "Delete saved pearl records for every multiplayer server?",
                core::resetAllServers);
        addAction(left, y + ROW * 3, width, "Reset all world locations",
                "Delete saved records for every single-player world.",
                "Reset all worlds",
                "Delete saved pearl records for every single-player world?",
                core::resetAllSingleplayerWorlds);
        addAction(left, y + ROW * 4, width, "Clear all saved tracking records",
                "Delete every stored world file. Configuration settings are not touched.",
                "Clear everything",
                "Delete all saved tracking records for every server and world?",
                core::clearAllRecords);

        addInfo(left, y + ROW * 5, width,
                "Stored world files: " + core.store().worldFileCount()
                        + "  |  records in memory: " + core.trackedCount());
    }

    private void buildAdvanced(PearlXCore core, PearlXConfig config, int left, int y, int width) {
        addCheckbox(left, y, "Enable debugging",
                "Write detailed tracking and animation information to the game log.",
                config.debugEnabled, value -> config.debugEnabled = value);
        addCheckbox(left, y + ROW, "Show tracking information",
                "Print a short tracking summary to chat every ten seconds.",
                config.showTrackingInfo, value -> config.showTrackingInfo = value);

        addRenderableWidget(Button.builder(Component.literal("Reset configuration to defaults"),
                        button -> confirm("Reset configuration",
                                "Restore every Pearl X setting to its default value? Saved pearl "
                                        + "locations are not deleted.",
                                core::resetConfiguration))
                .bounds(left, y + ROW * 2, width, WIDGET_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Settings only. Tracking records stay on disk.")))
                .build());

        addInfo(left, y + ROW * 3, width,
                "Tracked pearls: " + core.trackedCount()
                        + "  |  running animations: " + core.animations().runningSessions()
                        + "  |  storage: config/pearlx");
        addInfo(left, y + ROW * 4, width,
                "Saving timer: " + describe(config.savingIntervalValue, config.savingIntervalUnit)
                        + "  |  Animation delay: " + describe(config.animationDelayValue, config.animationDelayUnit));
    }

    private static String describe(int value, TimeUnit unit) {
        return value + " " + unit.label();
    }

    private void addAction(int x, int y, int width, String label, String tooltip,
                           String confirmTitle, String confirmMessage, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), button ->
                        confirm(confirmTitle, confirmMessage, () -> {
                            action.run();
                            init();
                        }))
                .bounds(x, y, width, WIDGET_HEIGHT)
                .tooltip(Tooltip.create(Component.literal(tooltip)))
                .build());
    }

    private void addCheckbox(int x, int y, String label, String tooltip, boolean initial,
                             Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(Component.literal(label), font)
                .pos(x, y)
                .selected(initial)
                .onValueChange((box, selected) -> {
                    setter.accept(selected);
                    save();
                })
                .tooltip(Tooltip.create(Component.literal(tooltip)))
                .build();
        checkbox.setWidth(Math.min(400, width - 16));
        addRenderableWidget(checkbox);
    }

    private void addSlider(int x, int y, int sliderWidth, String label, String suffix,
                           int min, int max, int value, String tooltip, IntConsumer setter) {
        IntSlider slider = new IntSlider(x, y, Math.max(80, sliderWidth), WIDGET_HEIGHT,
                label, suffix, min, max, value, newValue -> {
                    setter.accept(newValue);
                    save();
                });
        slider.setTooltip(Tooltip.create(Component.literal(tooltip)));
        addRenderableWidget(slider);
    }

    private void addUnitCycle(int x, int y, int cycleWidth, TimeUnit initial, Consumer<TimeUnit> setter) {
        CycleButton<TimeUnit> button = CycleButton.<TimeUnit>builder(
                        unit -> Component.literal(unit.label()), () -> initial)
                .withValues(TimeUnit.values())
                .create(x, y, cycleWidth, WIDGET_HEIGHT, Component.literal("Unit"),
                        (cycle, unit) -> {
                            setter.accept(unit);
                            save();
                            init();
                        });
        button.setTooltip(Tooltip.create(Component.literal("Seconds, minutes or hours for this timer.")));
        addRenderableWidget(button);
    }

    private void addInfo(int x, int y, int width, String text) {
        Button info = Button.builder(Component.literal(text), button -> {
                })
                .bounds(x, y, width, WIDGET_HEIGHT)
                .build();
        info.active = false;
        addRenderableWidget(info);
    }

    private void confirm(String title, String message, Runnable action) {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                action.run();
            }
            minecraft.setScreen(this);
        }, Component.literal(title), Component.literal(message),
                Component.literal("Confirm"), Component.literal("Cancel")));
    }

    private void save() {
        PearlXCore core = PearlXClient.core();
        if (core != null) {
            core.configManager().requestSave();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
