/**
 * Multiworld Mod
 */
package me.isaiah.multiworld.neoforge;

import me.isaiah.multiworld.GamemodeEnforcer;
import me.isaiah.multiworld.MultiworldMod;
import me.isaiah.multiworld.portal.WandEventHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;


@Mod(MultiworldMod.MOD_ID)
public class MultiworldModNeoForge {

	public MultiworldModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(this::commonSetup);

		NeoForge.EVENT_BUS.register(this);

		new xyz.nucleoid.fantasy.FantasyInitializer(modEventBus);
		NeoForgeWorldCreator.init();
		PermForge.init();
		MultiworldMod.init();
	}

	public void commonSetup(final FMLCommonSetupEvent event) {
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		MultiworldMod.on_server_started(event.getServer());
	}

	@SubscribeEvent
	public void onCommandsRegister(RegisterCommandsEvent event) {
		MultiworldMod.register_commands(event.getDispatcher());
	}
	
	@SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        WandEventHandler.leftClickBlock(event.getEntity(), event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        WandEventHandler.rightClickBlock(event.getEntity(), event.getLevel(), event.getHitVec());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity player)) return;
        net.minecraft.server.world.ServerWorld world = MultiworldMod.mc.getWorld(event.getTo());
        if (world != null) GamemodeEnforcer.enforceGamemode(player, world);
    }

}
