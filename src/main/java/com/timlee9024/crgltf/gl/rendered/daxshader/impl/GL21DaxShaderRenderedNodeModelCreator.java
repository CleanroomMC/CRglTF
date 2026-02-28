package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedNodeModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedNodeModelCreator;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;

import java.util.List;

public class GL21DaxShaderRenderedNodeModelCreator extends GL21RenderedNodeModelCreator {

	@Override
	public GL21RenderedNodeModel create(NodeModel nodeModel) {
		List<MeshModel> meshModels = nodeModel.getMeshModels();
		int meshModelCount = meshModels.size();
		if (meshModelCount == 0) return null;

		GL21DaxShaderRenderedMeshModel[] renderedMeshModels = new GL21DaxShaderRenderedMeshModel[meshModelCount];
		for (int i = 0; i < meshModelCount; i++) {
			MeshModel meshModel = meshModels.get(i);
			GL21RenderedMeshModel renderedMeshModel = renderedMeshModelLookup.get(meshModel);
			if (renderedMeshModel == null) {
				renderedMeshModelLookup.put(meshModel, renderedMeshModels[i] = (GL21DaxShaderRenderedMeshModel) renderedMeshModelCreator.create(meshModel));
			} else {
				renderedMeshModels[i] = (GL21DaxShaderRenderedMeshModel) renderedMeshModel;
			}
		}

		GL21DaxShaderRenderedNodeModel renderedNodeModel = new GL21DaxShaderRenderedNodeModel();
		renderedNodeModel.renderedMeshModels = renderedNodeModel.daxShaderRenderedMeshModels = renderedMeshModels;
		renderedNodeModel.weights = renderedNodeModel.originalWeights = nodeModel.getWeights();
		return renderedNodeModel;
	}
}
