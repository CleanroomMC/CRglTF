package com.timlee9024.crgltf.gl.rendered.impl;

import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;

import java.util.List;
import java.util.Map;

public class DefaultRenderedNodeModelCreator {

	public DefaultRenderedMeshModelCreator renderedMeshModelCreator;

	public Map<MeshModel, DefaultRenderedMeshModel> renderedMeshModelLookup;

	public DefaultRenderedNodeModel create(NodeModel nodeModel) {
		List<MeshModel> meshModels = nodeModel.getMeshModels();
		int meshModelCount = meshModels.size();
		if (meshModelCount == 0) return null;

		renderedMeshModelCreator.hasMorphTargets = false;
		DefaultRenderedMeshModel[] renderedMeshModels = new DefaultRenderedMeshModel[meshModelCount];
		for (int i = 0; i < meshModelCount; i++) {
			MeshModel meshModel = meshModels.get(i);
			DefaultRenderedMeshModel renderedMeshModel = renderedMeshModelLookup.get(meshModel);
			if (renderedMeshModel == null) {
				renderedMeshModelLookup.put(meshModel, renderedMeshModels[i] = renderedMeshModelCreator.create(meshModel));
			} else {
				renderedMeshModels[i] = renderedMeshModelCreator.createAlias(meshModel, renderedMeshModel);
			}
		}

		DefaultRenderedNodeModel renderedNodeModel = new DefaultRenderedNodeModel();
		renderedNodeModel.renderedMeshModels = renderedMeshModels;

		if (renderedMeshModelCreator.hasMorphTargets) {
			renderedNodeModel.morphing = renderedNodeModel.new Morphing();
			renderedNodeModel.morphing.weights = renderedNodeModel.morphing.originalWeights = nodeModel.getWeights();
		} else renderedNodeModel.morphing = DefaultRenderedNodeModel.Morphing.DUMMY;
		return renderedNodeModel;
	}

}
