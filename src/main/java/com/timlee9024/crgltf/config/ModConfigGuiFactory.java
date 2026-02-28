package com.timlee9024.crgltf.config;

import com.timlee9024.crgltf.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.List;
import java.util.Set;

public class ModConfigGuiFactory implements IModGuiFactory {
	@Override
	public void initialize(Minecraft minecraft) {

	}

	@Override
	public boolean hasConfigGui() {
		return true;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen guiScreen) {
		List<IConfigElement> configElements = new ConfigElement(ModConfig.getInstance().getConfiguration().getCategory(Configuration.CATEGORY_CLIENT)).getChildElements();
		DummyConfigElement configElement = new DummyConfigElement("config.crgltf.open_gltf_material_converter_pack_dir", null, ConfigGuiType.CONFIG_CATEGORY, "config.crgltf.open_gltf_material_converter_pack_dir");
		configElement.setConfigEntryClass(OpenGltfMaterialConverterPackDirEntry.class);
		configElements.add(configElement);
		return new GuiConfig(guiScreen,
				configElements,
				Reference.MOD_ID,
				false,
				false,
				I18n.format("config.crgltf.gltf_config_title"));
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}
}
