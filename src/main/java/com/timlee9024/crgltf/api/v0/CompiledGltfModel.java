package com.timlee9024.crgltf.api.v0;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.image.PixelData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A container that hold a processed {@link GltfModel} and its {@link PixelData} lookup from {@link ImageModel}.
 */
public interface CompiledGltfModel {

	/**
	 * Get the {@link GltfModel} that represent this {@link CompiledGltfModel}.
	 *
	 * @return The {@link GltfModel}.
	 */
	@NotNull GltfModel getGltfModel();

	/**
	 * Get the {@link PixelData}s that is created from {@link ImageModel}s inside the {@link GltfModel} of {@link CompiledGltfModel}.</br>
	 * The individual {@link PixelData} can be got from the {@link ImageModel} reference in the {@link GltfModel#getImageModels()}.
	 *
	 * @return {@link Map} contains each {@link PixelData} created from correspond {@link ImageModel}.
	 */
	@NotNull Map<ImageModel, PixelData> getPixelDataLookup();
}
