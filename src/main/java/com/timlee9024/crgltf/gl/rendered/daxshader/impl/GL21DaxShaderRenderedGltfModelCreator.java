package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import com.timlee9024.crgltf.gl.rendered.impl.CommonNodeAccessor;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMaterialModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedGltfModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedGltfModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedNodeModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.TextureModel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class GL21DaxShaderRenderedGltfModelCreator extends GL21RenderedGltfModelCreator {

	@Override
	public GL21RenderedGltfModel create(GltfModel gltfModel) {
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.glBufferLookup = new IdentityHashMap<>(gltfModel.getBufferViewModels().size());

		List<TextureModel> textureModels = gltfModel.getTextureModels();
		List<MaterialModel> materialModels = gltfModel.getMaterialModels();
		DefaultDaxShaderRenderedMaterialModelCreator renderedMaterialModelCreator = (DefaultDaxShaderRenderedMaterialModelCreator) this.renderedMaterialModelCreator;
		renderedMaterialModelCreator.textureModels = textureModels;
		renderedMaterialModelCreator.renderedTextureModelLookup = new IdentityHashMap<>(textureModels.size());
		renderedMaterialModelCreator.gltfMaterialExtraLookup = ((GL21DaxShaderRenderedMeshPrimitiveModelCreator) renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator).gltfMaterialExtraLookup = new IdentityHashMap<>(materialModels.size());
		renderedMaterialModelCreator.combinedRenderedTextureModelCreator.glTextureLookup = new IdentityHashMap<>(textureModels.size());
		Map<MaterialModel, DefaultRenderedMaterialModel> renderedMaterialModelLookup = new IdentityHashMap<>(materialModels.size());
		GltfMaterialToTextureConstants.getInstance().setupCanvasQuad();
		for (MaterialModel materialModel : materialModels) {
			renderedMaterialModelLookup.put(materialModel, renderedMaterialModelCreator.create(materialModel));
		}
		renderedMaterialModelCreator.combinedRenderedTextureModelCreator.deleteGlTextureLookup();
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.renderedMaterialModelLookup = renderedMaterialModelLookup;

		renderedNodeModelCreator.renderedMeshModelLookup = new IdentityHashMap<>(gltfModel.getMeshModels().size());

		jointMatricesLength = 0;
		calculatedJointMatricesLength = 0;
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.allZeroWeightsLength = 0;
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.dynamicFloatBufferSize = 0;

		nodeModels = gltfModel.getNodeModels();
		CommonNodeAccessor[] nodeAccessors = new CommonNodeAccessor[nodeModels.size()];

		nodeAccessorLookup = new IdentityHashMap<>(nodeModels.size());
		renderedNodeModelLookup = new IdentityHashMap<>(nodeModels.size());

		for (int i = 0; i < nodeModels.size(); i++) {
			nodeAccessors[i] = createNodeAccessor(nodeModels.get(i));
		}

		inverseBindMatricesLookup = new IdentityHashMap<>(gltfModel.getAccessorModels().size());
		Map<NodeModel, Map.Entry<CommonNodeAccessor, List<GL21RenderedNodeModel>>> rootNodeLookup = createRootNodeLookup();
		List<SceneModel> sceneModels = gltfModel.getSceneModels();
		GL21DaxShaderRenderedSceneModel[] renderedSceneModels = new GL21DaxShaderRenderedSceneModel[sceneModels.size()];
		for (int i = 0; i < sceneModels.size(); i++) {
			SceneModel sceneModel = sceneModels.get(i);
			GL21DaxShaderRenderedSceneModel renderedSceneModel = renderedSceneModels[i] = (GL21DaxShaderRenderedSceneModel) renderedSceneModelCreator.create(sceneModel);
			List<GL21RenderedNodeModel> renderedNodeModels = new ArrayList<>(nodeModels.size());
			for (NodeModel nodeModel : sceneModel.getNodeModels()) {
				renderedNodeModels.addAll(rootNodeLookup.get(nodeModel).getValue());
			}
			renderedSceneModel.renderedNodeModels = renderedSceneModel.daxShaderRenderedNodeModels = renderedNodeModels.toArray(new GL21DaxShaderRenderedNodeModel[0]);
		}

		GL21DaxShaderRenderedGltfModel renderedGltfModel = new GL21DaxShaderRenderedGltfModel();
		renderedGltfModel.renderedSceneModels = renderedGltfModel.daxShaderRenderedSceneModels = renderedSceneModels;
		renderedGltfModel.nodeAccessors = nodeAccessors;
		renderedGltfModel.rootNodeAccessors = new CommonNodeAccessor[rootNodeLookup.size()];
		int i = 0;
		for (Map.Entry<CommonNodeAccessor, List<GL21RenderedNodeModel>> entry : rootNodeLookup.values()) {
			renderedGltfModel.rootNodeAccessors[i++] = entry.getKey();
		}

		this.renderedGltfModel = renderedGltfModel;
		initSharedRenderData();

		return renderedGltfModel;
	}
}
