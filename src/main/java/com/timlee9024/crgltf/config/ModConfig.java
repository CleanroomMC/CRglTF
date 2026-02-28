package com.timlee9024.crgltf.config;

import com.timlee9024.crgltf.Reference;
import com.timlee9024.crgltf.gl.GltfMaterialConverterPackManager;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

public class ModConfig {
	private static ModConfig instance;

	protected Configuration configuration;
	protected Property gltfMaterialConverterPack;
	protected Property openGlAvailability;
	protected File gltfMaterialConverterPackDir;
	protected boolean hasGltfMaterialConverterPackChanged;

	public static ModConfig getInstance() {
		return instance;
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
		MinecraftForge.EVENT_BUS.register(this);

		configuration = new Configuration(event.getSuggestedConfigurationFile());
		configuration.load();

		gltfMaterialConverterPack = configuration.get(Configuration.CATEGORY_CLIENT, "gltfMaterialConverterPack", "");
		gltfMaterialConverterPack.setLanguageKey("config.crgltf.gltf_material_converter_pack");
		gltfMaterialConverterPack.setConfigEntryClass(GltfMaterialConverterPackEntry.class);

		openGlAvailability = configuration.get(Configuration.CATEGORY_CLIENT, "openglAvailability", "Auto");
		openGlAvailability.setLanguageKey("config.crgltf.opengl_availability");
		openGlAvailability.setConfigEntryClass(OpenGlAvailabilityEntry.class);
		openGlAvailability.setRequiresMcRestart(true);

		if (configuration.hasChanged()) configuration.save();

		gltfMaterialConverterPackDir = new File(event.getModConfigurationDirectory(), "gltf_material_converter_pack");
	}

	@SubscribeEvent
	public void onEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
		if (event.getModID().equals(Reference.MOD_ID) && configuration.hasChanged()) {
			hasGltfMaterialConverterPackChanged = gltfMaterialConverterPack.hasChanged();
			configuration.save();
		}
	}

	@SubscribeEvent
	public void onEvent(ConfigChangedEvent.PostConfigChangedEvent event) {
		if (event.getModID().equals(Reference.MOD_ID) && hasGltfMaterialConverterPackChanged) {
			GltfMaterialConverterPackManager.getInstance().reloadTextureCombinerPrograms();
			FMLClientHandler.instance().refreshResources(VanillaResourceType.MODELS);
		}
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public Property getGltfMaterialConverterPack() {
		return gltfMaterialConverterPack;
	}

	public Property getOpenGlAvailability() {
		return openGlAvailability;
	}

	public File getGltfMaterialConverterPackDir() {
		return gltfMaterialConverterPackDir;
	}
}
