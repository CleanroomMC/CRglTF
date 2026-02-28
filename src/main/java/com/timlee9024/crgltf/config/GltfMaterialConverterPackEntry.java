package com.timlee9024.crgltf.config;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.io.File;
import java.util.HashMap;

public class GltfMaterialConverterPackEntry extends GuiConfigEntries.SelectValueEntry {

	public GltfMaterialConverterPackEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
		super(owningScreen, owningEntryList, configElement, new HashMap<>());
	}

	@Override
	public void valueButtonPressed(int slotIndex) {
		selectableValues.clear();
		selectableValues.put("", I18n.format("config.crgltf.gltf_material_converter_pack.disabled"));
		File gltfMaterialConverterPackDir = ModConfig.getInstance().getGltfMaterialConverterPackDir();
		if (gltfMaterialConverterPackDir.exists()) {
			for (File file : gltfMaterialConverterPackDir.listFiles()) {
				if (file.isDirectory()) {
					String fileName = file.getName();
					selectableValues.put(fileName, fileName);
				} else {
					String fileName = file.getName();
					if (fileName.endsWith(".zip")) selectableValues.put(fileName, fileName);
				}
			}
		} else gltfMaterialConverterPackDir.mkdir();
		super.valueButtonPressed(slotIndex);
	}

}
