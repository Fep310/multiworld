package me.isaiah.multiworld.command;

import java.io.IOException;
import java.util.HashMap;

import me.isaiah.multiworld.I18n;
import me.isaiah.multiworld.MultiworldMod;
import me.isaiah.multiworld.Utils;
import me.isaiah.multiworld.config.FileConfiguration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * The "/mw gamemode" Command.
 * Configures the enforced gamemode for a world. When a player enters that world,
 * their gamemode is automatically changed to match.
 * Using "DEFAULT" removes enforcement.
 */
public class GamemodeCommand implements Command {

    public static int run(MinecraftServer mc, ServerPlayerEntity plr, String[] args) {
        ServerWorld w = Command.getWorldFor(plr);

        if (args.length < 2) {
            MultiworldMod.message(plr, I18n.CMD_GAMEMODE_USAGE);
            return 1;
        }

        String mode = args[1].toUpperCase();

        if (args.length >= 3) {
            String a2 = args[2];

            HashMap<String, ServerWorld> worlds = new HashMap<>();
            mc.getWorldRegistryKeys().forEach(r -> {
                ServerWorld world = mc.getWorld(r);
                worlds.put(r.getValue().toString(), world);
            });

            if (a2.indexOf(':') == -1) a2 = "multiworld:" + a2;

            if (worlds.containsKey(a2)) {
                w = worlds.get(a2);
            }
        }

        if (!mode.equals("SURVIVAL") && !mode.equals("CREATIVE") &&
                !mode.equals("ADVENTURE") && !mode.equals("SPECTATOR") &&
                !mode.equals("DEFAULT")) {
            MultiworldMod.message(plr, I18n.CMD_GAMEMODE_USAGE);
            return 1;
        }

        try {
            Identifier worldId = w.getRegistryKey().getValue();
            // Prefer the world's own config file (multiworld-world.yml); fall back to legacy config.
            FileConfiguration config = Utils.getConfigOrNull(worldId);
            if (config == null) {
                config = Util.get_config(w);
            }
            config.set("gamemode", mode);
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MultiworldMod.message(plr, String.format(I18n.CMD_GAMEMODE_SET,
                w.getRegistryKey().getValue().toString(), mode));
        return 1;
    }

}
