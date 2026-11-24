package dev.pearlx.mc;

import dev.pearlx.core.Log;
import dev.pearlx.core.PearlXCore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Client entrypoint. Pearl X has no server entrypoint and never touches server logic. */
public final class PearlXClient implements ClientModInitializer {

    private static PearlXCore core;
    private static boolean stopped;

    @Override
    public void onInitializeClient() {
        Path directory = FabricLoader.getInstance().getConfigDir().resolve("pearlx");
        PearlXCore instance = new PearlXCore(new McClientPort(), directory);
        try {
            instance.init();
        } catch (Throwable throwable) {
            Log.error("Pearl X could not initialise and will stay inactive", throwable);
            return;
        }
        core = instance;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (stopped || core == null) {
                return;
            }
            try {
                core.tick();
            } catch (Throwable throwable) {
                // Never crash the game because of Pearl X; report it once and disable ticking.
                stopped = true;
                Log.error("Pearl X disabled itself after an unexpected error", throwable);
                if (client.player != null) {
                    client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Pearl X: disabled after an internal error. See the log for details."));
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            stopped = true;
            if (core != null) {
                core.shutdown();
            }
        });
    }

    public static PearlXCore core() {
        return core;
    }

    public static boolean isActive() {
        return core != null && !stopped;
    }
}
