package com.timlee9024.crgltf.gl.rendered.impl;

import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SkinModel;

import java.util.List;
import java.util.Map;

public class GL21ExtRenderedNodeModelCreator {

	public GL21ExtRenderedMeshModelCreator renderedMeshModelCreator;

	public Map<MeshModel, GL21ExtRenderedMeshModel> renderedMeshModelLookup;

	public GL21ExtRenderedNodeModel create(NodeModel nodeModel) {
		List<MeshModel> meshModels = nodeModel.getMeshModels();
		int meshModelCount = meshModels.size();
		if (meshModelCount == 0) return null;

		renderedMeshModelCreator.hasMorphTargets = false;
		SkinModel skinModel = nodeModel.getSkinModel();
		renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.parentNodeJointCount = skinModel == null ? 0 : skinModel.getJoints().size();
		GL21ExtRenderedMeshModel[] renderedMeshModels = new GL21ExtRenderedMeshModel[meshModelCount];
		for (int i = 0; i < meshModelCount; i++) {
			MeshModel meshModel = meshModels.get(i);
			GL21ExtRenderedMeshModel renderedMeshModel = renderedMeshModelLookup.get(meshModel);
			if (renderedMeshModel == null) {
				renderedMeshModelLookup.put(meshModel, renderedMeshModels[i] = renderedMeshModelCreator.create(meshModel));
			} else {
				renderedMeshModels[i] = renderedMeshModelCreator.createAlias(meshModel, renderedMeshModel);
			}
		}

		GL21ExtRenderedNodeModel renderedNodeModel = new GL21ExtRenderedNodeModel();
		renderedNodeModel.renderedMeshModels = renderedMeshModels;

		if (renderedMeshModelCreator.hasMorphTargets) {
			renderedNodeModel.morphing = renderedNodeModel.new Morphing();
			renderedNodeModel.morphing.weights = renderedNodeModel.morphing.originalWeights = nodeModel.getWeights();
		} else renderedNodeModel.morphing = GL21ExtRenderedNodeModel.Morphing.DUMMY;
		return renderedNodeModel;
	}

}
