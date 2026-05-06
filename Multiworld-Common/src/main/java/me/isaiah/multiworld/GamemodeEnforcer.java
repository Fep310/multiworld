package me.isaiah.multiworld;

import java.io.IOException;

import me.isaiah.multiworld.config.FileConfiguration;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

/**
 * Enforces per-world forced gamemodes when a player enters a world.
 * Reads the "gamemode" key from the world's config (multiworld-world.yml or legacy config).
 * A value of "DEFAULT" or the absence of the key means no enforcement.
 */
public class GamemodeEnforcer {

    /**
     * If the destination world has a forced gamemode configured, changes the
     * player's gamemode to match. Does nothing if no enforcement is configured.
     *
     * @param player the player who entered the world
     * @param world  the world the player entered
     */
    public static void enforceGamemode(ServerPlayerEntity player, ServerWorld world) {
        Identifier id = world.getRegistryKey().getValue();
        try {
            FileConfiguration config = Utils.getConfigOrNull(id);
            if (config == null) return;
            if (!config.is_set("gamemode")) return;

            String mode = config.getString("gamemode");
            if (mode == null || mode.equalsIgnoreCase("DEFAULT")) return;

            GameMode gameMode = parseGameMode(mode);
            if (gameMode == null) return;

            player.changeGameMode(gameMode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a gamemode string into a {@link GameMode} enum value.
     * Accepts full names (SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR) case-insensitively.
     *
     * @param mode the string to parse
     * @return the corresponding {@link GameMode}, or {@code null} if unrecognised
     */
    public static GameMode parseGameMode(String mode) {
        if (mode.equalsIgnoreCase("SURVIVAL"))   return GameMode.SURVIVAL;
        if (mode.equalsIgnoreCase("CREATIVE"))   return GameMode.CREATIVE;
        if (mode.equalsIgnoreCase("ADVENTURE"))  return GameMode.ADVENTURE;
        if (mode.equalsIgnoreCase("SPECTATOR"))  return GameMode.SPECTATOR;
        return null;
    }

}
