package dev.pearlx.mc;

import dev.pearlx.core.ClientPort;
import dev.pearlx.core.Log;
import dev.pearlx.core.ParticleKind;
import dev.pearlx.core.PearlSnapshot;
import dev.pearlx.core.SoundCue;
import dev.pearlx.core.WorldKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;

import java.util.ArrayList;
import java.util.List;

/**
 * All Minecraft API contact for Pearl X on 1.21.6 - 1.21.8.
 *
 * <p>Only client-side information is used: the loaded client level, the entities the client knows
 * about and the local player. Nothing is sent to the server.</p>
 */
public final class McClientPort implements ClientPort {

    private final Minecraft client = Minecraft.getInstance();

    @Override
    public long nowMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public WorldKey currentWorldKey() {
        ClientLevel level = client.level;
        if (level == null) {
            return null;
        }
        ServerData server = client.getCurrentServer();
        if (server != null) {
            String address = server.ip;
            if (address == null || address.isBlank()) {
                address = "unknown-server";
            }
            return WorldKey.server(address);
        }
        IntegratedServer integrated = client.getSingleplayerServer();
        if (integrated != null && integrated.getWorldData() != null) {
            String name = integrated.getWorldData().getLevelName();
            if (name == null || name.isBlank()) {
                name = "singleplayer";
            }
            return WorldKey.singleplayer(name);
        }
        return WorldKey.server("local-or-lan");
    }

    @Override
    public String currentDimension() {
        ClientLevel level = client.level;
        return level == null ? null : level.dimension().location().toString();
    }

    @Override
    public List<PearlSnapshot> scanPearls() {
        List<PearlSnapshot> snapshots = new ArrayList<>();
        ClientLevel level = client.level;
        Player player = client.player;
        if (level == null || player == null) {
            return snapshots;
        }
        String dimension = level.dimension().location().toString();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl)) {
                continue;
            }
            Entity owner = pearl.getOwner();
            snapshots.add(new PearlSnapshot(
                    pearl.getUUID().toString(),
                    owner != null && owner == player,
                    pearl.getX(), pearl.getY(), pearl.getZ(),
                    pearl.getDeltaMovement().x, pearl.getDeltaMovement().y, pearl.getDeltaMovement().z,
                    dimension));
        }
        return snapshots;
    }

    @Override
    public boolean isChunkLoaded(String dimension, double x, double z) {
        ClientLevel level = client.level;
        if (level == null) {
            return false;
        }
        String currentDimension = level.dimension().location().toString();
        if (dimension == null || !dimension.equals(currentDimension)) {
            // A pearl in another dimension is not synchronised to us at all.
            return false;
        }
        int chunkX = ((int) Math.floor(x)) >> 4;
        int chunkZ = ((int) Math.floor(z)) >> 4;
        return level.hasChunk(chunkX, chunkZ);
    }

    @Override
    public void spawnParticle(ParticleKind kind, double x, double y, double z,
                              double dx, double dy, double dz) {
        ClientLevel level = client.level;
        if (level == null) {
            return;
        }
        ParticleOptions particle = switch (kind) {
            case PORTAL -> ParticleTypes.PORTAL;
            case REVERSE_PORTAL -> ParticleTypes.REVERSE_PORTAL;
            case END_ROD -> ParticleTypes.END_ROD;
            // 1.21.9 turned dragon breath into a powered particle, so it is no longer a
            // ParticleOptions on its own.
            case DRAGON_BREATH -> PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
            case ELECTRIC_SPARK -> ParticleTypes.ELECTRIC_SPARK;
            case WITCH -> ParticleTypes.WITCH;
        };
        level.addParticle(particle, x, y, z, dx, dy, dz);
    }

    @Override
    public void playSound(SoundCue cue, double x, double y, double z, float volume, float pitch) {
        ClientLevel level = client.level;
        if (level == null || volume <= 0.0F) {
            return;
        }
        SoundEvent event = cue == SoundCue.TRAVEL_START
                ? SoundEvents.ENDER_PEARL_THROW
                : SoundEvents.ENDERMAN_TELEPORT;
        level.playLocalSound(x, y, z, event, SoundSource.PLAYERS, volume, pitch, false);
    }

    @Override
    public void info(String message) {
        Log.info(message);
        Player player = client.player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    @Override
    public String playerName() {
        Player player = client.player;
        return player == null ? "player" : player.getName().getString();
    }
}
