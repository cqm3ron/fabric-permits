package cqm3ron.permits;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import cqm3ron.permits.command.PermitCommand;
import cqm3ron.permits.component.ModDataComponentTypes;
import cqm3ron.permits.item.ModItems;


public class Permits implements ModInitializer {
	public static final String MOD_ID = "permits";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ModItems.registerModItems();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			PermitCommand.register(dispatcher, registryAccess);
		});
		ModDataComponentTypes.registerDataComponentTypes();
	}
}