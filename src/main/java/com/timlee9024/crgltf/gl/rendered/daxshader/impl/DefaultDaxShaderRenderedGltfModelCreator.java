package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import com.timlee9024.crgltf.gl.rendered.impl.CommonNodeAccessor;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedGltfModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedGltfModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMaterialModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedNodeModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.TextureModel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class DefaultDaxShaderRenderedGltfModelCreator extends DefaultRenderedGltfModelCreator {

	@Override
	public DefaultRenderedGltfModel create(GltfModel gltfModel) {
		glBufferLookup = new IdentityHashMap<>(gltfModel.getBufferViewModels().size());
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.glBufferLookup = glBufferLookup;

		List<TextureModel> textureModels = gltfModel.getTextureModels();
		List<MaterialModel> materialModels = gltfModel.getMaterialModels();
		DefaultDaxShaderRenderedMaterialModelCreator renderedMaterialModelCreator = (DefaultDaxShaderRenderedMaterialModelCreator) this.renderedMaterialModelCreator;
		renderedMaterialModelCreator.textureModels = textureModels;
		renderedMaterialModelCreator.renderedTextureModelLookup = new IdentityHashMap<>(textureModels.size());
		renderedMaterialModelCreator.gltfMaterialExtraLookup = ((DefaultDaxShaderRenderedMeshPrimitiveModelCreator) renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator).gltfMaterialExtraLookup = new IdentityHashMap<>(materialModels.size());
		renderedMaterialModelCreator.combinedRenderedTextureModelCreator.glTextureLookup = new IdentityHashMap<>(textureModels.size());
		Map<MaterialModel, DefaultRenderedMaterialModel> renderedMaterialModelLookup = new IdentityHashMap<>(materialModels.size());
		GltfMaterialToTextureConstants.getInstance().setupCanvasQuad();
		for (MaterialModel materialModel : materialModels) {
			renderedMaterialModelLookup.put(materialModel, renderedMaterialModelCreator.create(materialModel));
		}
		renderedMaterialModelCreator.combinedRenderedTextureModelCreator.deleteGlTextureLookup();
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.renderedMaterialModelLookup = renderedMaterialModelLookup;

		renderedNodeModelCreator.renderedMeshModelLookup = new IdentityHashMap<>(gltfModel.getMeshModels().size());

		nodeModels = gltfModel.getNodeModels();
		CommonNodeAccessor[] nodeAccessors = new CommonNodeAccessor[nodeModels.size()];

		nodeAccessorLookup = new IdentityHashMap<>(nodeModels.size());
		renderedNodeModelLookup = new IdentityHashMap<>(nodeModels.size());

		for (int i = 0; i < nodeModels.size(); i++) {
			nodeAccessors[i] = createNodeAccessor(nodeModels.get(i));
		}

		Map<NodeModel, Map.Entry<CommonNodeAccessor, List<DefaultRenderedNodeModel>>> rootNodeLookup = createRootNodeLookup();
		List<SceneModel> sceneModels = gltfModel.getSceneModels();
		DefaultDaxShaderRenderedSceneModel[] renderedSceneModels = new DefaultDaxShaderRenderedSceneModel[sceneModels.size()];
		for (int i = 0; i < sceneModels.size(); i++) {
			SceneModel sceneModel = sceneModels.get(i);
			DefaultDaxShaderRenderedSceneModel renderedSceneModel = renderedSceneModels[i] = (DefaultDaxShaderRenderedSceneModel) renderedSceneModelCreator.create(sceneModel);
			List<DefaultRenderedNodeModel> renderedNodeModels = new ArrayList<>(nodeModels.size());
			for (NodeModel nodeModel : sceneModel.getNodeModels()) {
				renderedNodeModels.addAll(rootNodeLookup.get(nodeModel).getValue());
			}
			renderedSceneModel.renderedNodeModels = renderedSceneModel.daxShaderRenderedNodeModels = renderedNodeModels.toArray(new DefaultDaxShaderRenderedNodeModel[0]);
			checkMorphingAndSkinning(renderedSceneModel);
		}

		DefaultDaxShaderRenderedGltfModel renderedGltfModel = new DefaultDaxShaderRenderedGltfModel();
		renderedGltfModel.glTextures = renderedMaterialModelCreator.combinedRenderedTextureModelCreator.glTextures;
		renderedGltfModel.glBufferViews = glBuffers;
		renderedGltfModel.glVertexArrays = glVertexArrays;
		renderedGltfModel.renderedSceneModels = renderedGltfModel.daxShaderRenderedSceneModels = renderedSceneModels;
		renderedGltfModel.nodeAccessors = nodeAccessors;
		renderedGltfModel.rootNodeAccessors = new CommonNodeAccessor[rootNodeLookup.size()];
		int i = 0;
		for (Map.Entry<CommonNodeAccessor, List<DefaultRenderedNodeModel>> entry : rootNodeLookup.values()) {
			renderedGltfModel.rootNodeAccessors[i++] = entry.getKey();
		}
		return renderedGltfModel;
	}

}
