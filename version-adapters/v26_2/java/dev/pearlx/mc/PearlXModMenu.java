package dev.pearlx.mc;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/** Mod Menu hook: Pearl X always exposes its own configuration screen. */
public final class PearlXModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            if (!PearlXClient.isActive()) {
                return parent;
            }
            return new PearlXConfigScreen(parent);
        };
    }

    /** Used by the Mod Menu API on some versions; harmless elsewhere. */
    public Screen create(Screen parent) {
        return new PearlXConfigScreen(parent);
    }
}
