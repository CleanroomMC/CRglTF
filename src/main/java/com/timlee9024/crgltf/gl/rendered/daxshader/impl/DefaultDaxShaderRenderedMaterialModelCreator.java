package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.google.gson.Gson;
import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMaterialModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedTextureModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedTextureModelCreator;
import com.timlee9024.crgltf.property.GltfMaterialExtra;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;

import java.util.List;
import java.util.Map;

public class DefaultDaxShaderRenderedMaterialModelCreator extends DefaultRenderedMaterialModelCreator {

	public GltfMaterialToTextureConstants.Program colorTextureProgram;
	public GltfMaterialToTextureConstants.Program normalTextureProgram;
	public GltfMaterialToTextureConstants.Program specularTextureProgram;

	public Map<MaterialModel, GltfMaterialExtra> gltfMaterialExtraLookup;
	public DefaultRenderedTextureModelCreator renderedTextureModelCreator;
	public Map<TextureModel, DefaultRenderedTextureModel> renderedTextureModelLookup;
	public List<TextureModel> textureModels;

	@Override
	public DefaultDaxShaderRenderedMaterialModel create(MaterialModel materialModel) {
		if (materialModel instanceof MaterialModelV2 materialModelV2) {
			combinedRenderedTextureModelCreator.materialModelV2 = materialModelV2;
			DefaultDaxShaderRenderedMaterialModel renderedMaterialModel = new DefaultDaxShaderRenderedMaterialModel();

			Gson gson = new Gson();
			GltfMaterialExtra gltfMaterialExtra = gson.fromJson(gson.toJsonTree(materialModelV2.getExtras()), GltfMaterialExtra.class);
			gltfMaterialExtraLookup.put(materialModel, gltfMaterialExtra);
			if (gltfMaterialExtra != null && gltfMaterialExtra.crgltf != null && gltfMaterialExtra.crgltf.daxShader != null) {
				if (gltfMaterialExtra.crgltf.daxShader.colorMapTexture != null) {
					renderedMaterialModel.baseColorTextureForDaxShader = renderedTextureModelLookup.computeIfAbsent(textureModels.get(gltfMaterialExtra.crgltf.daxShader.colorMapTexture), renderedTextureModelCreator::create);
				} else if (colorTextureProgram != null) {
					combinedRenderedTextureModelCreator.program = colorTextureProgram;
					TextureModel textureModel = materialModelV2.getBaseColorTexture();
					renderedMaterialModel.baseColorTextureForDaxShader = combinedRenderedTextureModelCreator.create(textureModel != null ? textureModel : dummyTextureModel);
				} else {
					TextureModel textureModel = materialModelV2.getBaseColorTexture();
					if (textureModel != null)
						renderedMaterialModel.baseColorTextureForDaxShader = renderedTextureModelLookup.computeIfAbsent(textureModel, renderedTextureModelCreator::create);
					else renderedMaterialModel.baseColorTextureForDaxShader = DefaultRenderedTextureModel.WHITE_TEXTURE;
				}

				if (gltfMaterialExtra.crgltf.daxShader.normalMapTexture != null) {
					renderedMaterialModel.normalTexture = renderedTextureModelLookup.computeIfAbsent(textureModels.get(gltfMaterialExtra.crgltf.daxShader.normalMapTexture), renderedTextureModelCreator::create);
				} else if (normalTextureProgram != null) {
					combinedRenderedTextureModelCreator.program = normalTextureProgram;
					TextureModel textureModel = materialModelV2.getNormalTexture();
					renderedMaterialModel.normalTexture = combinedRenderedTextureModelCreator.create(textureModel != null ? textureModel : dummyTextureModel);
				} else {
					TextureModel textureModel = materialModelV2.getNormalTexture();
					if (textureModel != null)
						renderedMaterialModel.normalTexture = renderedTextureModelLookup.computeIfAbsent(textureModel, renderedTextureModelCreator::create);
					else renderedMaterialModel.normalTexture = DefaultRenderedTextureModel.FLAT_NORMAL_TEXTURE;
				}

				if (gltfMaterialExtra.crgltf.daxShader.specularMapTexture != null) {
					renderedMaterialModel.specularTexture = renderedTextureModelLookup.computeIfAbsent(textureModels.get(gltfMaterialExtra.crgltf.daxShader.specularMapTexture), renderedTextureModelCreator::create);
				} else if (specularTextureProgram != null) {
					combinedRenderedTextureModelCreator.program = specularTextureProgram;
					TextureModel textureModel = materialModelV2.getMetallicRoughnessTexture();
					renderedMaterialModel.specularTexture = combinedRenderedTextureModelCreator.create(textureModel != null ? textureModel : dummyTextureModel);
				} else {
					TextureModel textureModel = materialModelV2.getMetallicRoughnessTexture();
					if (textureModel != null)
						renderedMaterialModel.specularTexture = renderedTextureModelLookup.computeIfAbsent(textureModel, renderedTextureModelCreator::create);
					else renderedMaterialModel.specularTexture = DefaultRenderedTextureModel.UNBIND_TEXTURE;
				}
			} else {
				if (colorTextureProgram != null) {
					combinedRenderedTextureModelCreator.program = colorTextureProgram;
					TextureModel textureModel = materialModelV2.getBaseColorTexture();
					renderedMaterialModel.baseColorTextureForDaxShader = combinedRenderedTextureModelCreator.create(textureModel != null ? textureModel : dummyTextureModel);
				} else {
					TextureModel textureModel = materialModelV2.getBaseColorTexture();
					if (textureModel != null)
						renderedMaterialModel.baseColorTextureForDaxShader = renderedTextureModelLookup.computeIfAbsent(textureModel, renderedTextureModelCreator::create);
					else renderedMaterialModel.baseColorTextureForDaxShader = DefaultRenderedTextureModel.WHITE_TEXTURE;
				}

				if (normalTextureProgram != null) {
					combinedRenderedTextureModelCreator.program = normalTextureProgram;
					TextureModel textureModel = materialModelV2.getNormalTexture();
					renderedMaterialModel.normalTexture = combinedRenderedTextureModelCreator.create(textureModel != null ? textureModel : dummyTextureModel);
				} else {
					TextureModel textureModel = materialModelV2.getNormalTexture();
					if (textureModel != null)
						renderedMaterialModel.normalTexture = renderedTextureModelLookup.computeIfAbsent(textureModel, renderedTextureModelCreator::create);
					else renderedMaterialModel.normalTexture = DefaultRenderedTextureModel.FLAT_NORMAL_TEXTURE;
				}

				if (specularTextureProgram != null) {
					combinedRenderedTextureModelCreator.program = specularTextureProgram;
					TextureModel textureModel = materialModelV2.getMetallicRoughnessTexture();
					renderedMaterialModel.specularTexture = combinedRenderedTextureModelCreator.create(textureModel != null ? textureModel : dummyTextureModel);
				} else {
					TextureModel textureModel = materialModelV2.getMetallicRoughnessTexture();
					if (textureModel != null)
						renderedMaterialModel.specularTexture = renderedTextureModelLookup.computeIfAbsent(textureModel, renderedTextureModelCreator::create);
					else renderedMaterialModel.specularTexture = DefaultRenderedTextureModel.UNBIND_TEXTURE;
				}
			}

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
		return DefaultDaxShaderRenderedMaterialModel.DEFAULT;
	}

}
