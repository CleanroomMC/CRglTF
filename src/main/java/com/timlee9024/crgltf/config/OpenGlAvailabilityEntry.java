package com.timlee9024.crgltf.config;

import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.HashMap;
import java.util.Map;

public class OpenGlAvailabilityEntry extends GuiConfigEntries.SelectValueEntry {

	public OpenGlAvailabilityEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
		super(owningScreen, owningEntryList, configElement, listOpenGlAvailability());
	}

	private static Map<Object, String> listOpenGlAvailability() {
		Map<Object, String> values = new HashMap<>(3);
		values.put("Auto", "Auto");
		values.put("Full", "Full");
		values.put("GL21_FBO", "GL21_FBO");
		return values;
	}
}
