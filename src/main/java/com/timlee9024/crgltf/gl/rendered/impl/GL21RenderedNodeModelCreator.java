package com.timlee9024.crgltf.gl.rendered.impl;

import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;

import java.util.List;
import java.util.Map;

public class GL21RenderedNodeModelCreator {

	public GL21RenderedMeshModelCreator renderedMeshModelCreator;

	public Map<MeshModel, GL21RenderedMeshModel> renderedMeshModelLookup;

	public GL21RenderedNodeModel create(NodeModel nodeModel) {
		List<MeshModel> meshModels = nodeModel.getMeshModels();
		int meshModelCount = meshModels.size();
		if (meshModelCount == 0) return null;

		GL21RenderedMeshModel[] renderedMeshModels = new GL21RenderedMeshModel[meshModelCount];
		for (int i = 0; i < meshModelCount; i++) {
			MeshModel meshModel = meshModels.get(i);
			GL21RenderedMeshModel renderedMeshModel = renderedMeshModelLookup.get(meshModel);
			if (renderedMeshModel == null) {
				renderedMeshModelLookup.put(meshModel, renderedMeshModels[i] = renderedMeshModelCreator.create(meshModel));
			} else {
				renderedMeshModels[i] = renderedMeshModel;
			}
		}

		GL21RenderedNodeModel renderedNodeModel = new GL21RenderedNodeModel();
		renderedNodeModel.renderedMeshModels = renderedMeshModels;
		renderedNodeModel.weights = renderedNodeModel.originalWeights = nodeModel.getWeights();
		return renderedNodeModel;
	}
}
