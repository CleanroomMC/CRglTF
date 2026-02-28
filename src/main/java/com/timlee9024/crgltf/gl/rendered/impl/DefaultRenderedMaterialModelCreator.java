package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;

import java.util.Map;

public class DefaultRenderedMaterialModelCreator {

	public CombinedRenderedTextureModelCreator combinedRenderedTextureModelCreator;

	protected final TextureModel dummyTextureModel = new TextureModel() {
		@Override
		public Integer getMagFilter() {
			return GltfConstants.GL_LINEAR;
		}

		@Override
		public Integer getMinFilter() {
			return GltfConstants.GL_LINEAR;
		}

		@Override
		public Integer getWrapS() {
			return GltfConstants.GL_REPEAT;
		}

		@Override
		public Integer getWrapT() {
			return GltfConstants.GL_REPEAT;
		}

		@Override
		public ImageModel getImageModel() {
			return null;
		}

		@Override
		public String getName() {
			return "";
		}

		@Override
		public Map<String, Object> getExtensions() {
			return Map.of();
		}

		@Override
		public Object getExtras() {
			return null;
		}
	};

	public DefaultRenderedMaterialModel create(MaterialModel materialModel) {
		if (materialModel instanceof MaterialModelV2 materialModelV2) {
			combinedRenderedTextureModelCreator.materialModelV2 = materialModelV2;
			DefaultRenderedMaterialModel renderedMaterialModel = new DefaultRenderedMaterialModel();

			combinedRenderedTextureModelCreator.program = VanillaRenderConstants.getInstance().getMaterialToColorTextureProgram();
			TextureModel textureModel = materialModelV2.getBaseColorTexture();
			renderedMaterialModel.baseColorTexture = combinedRenderedTextureModelCreator.create(textureModel != null ? textureModel : dummyTextureModel);

			combinedRenderedTextureModelCreator.program = VanillaRenderConstants.getInstance().getMaterialToEmissiveTextureProgram();
			textureModel = materialModelV2.getEmissiveTexture();
			renderedMaterialModel.emissiveTexture = combinedRenderedTextureModelCreator.create(textureModel != null ? textureModel : dummyTextureModel);

			renderedMaterialModel.alphaMode = materialModelV2.getAlphaMode();
			renderedMaterialModel.alphaCutoff = materialModelV2.getAlphaCutoff();
			renderedMaterialModel.doubleSided = materialModelV2.isDoubleSided();
			return renderedMaterialModel;
		}
		return DefaultRenderedMaterialModel.DEFAULT;
	}

}
