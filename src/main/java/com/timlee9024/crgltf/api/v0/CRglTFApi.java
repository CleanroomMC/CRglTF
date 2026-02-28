package com.timlee9024.crgltf.api.v0;

import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The main API entry point for other Mods.
 */
public abstract class CRglTFApi {

	private static CRglTFApi instance;

	public static CRglTFApi getInstance() {
		return instance;
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	public abstract void onEvent(FMLLoadCompleteEvent event);

	/**
	 * Get the main instance of {@link UniversalGltfRenderer} for loading and rendering glTF model.
	 *
	 * @return The main instance of {@link UniversalGltfRenderer}.
	 */
	public abstract UniversalGltfRenderer getUniversalGltfRenderer();
}
