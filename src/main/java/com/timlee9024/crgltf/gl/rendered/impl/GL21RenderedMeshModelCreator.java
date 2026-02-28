package com.timlee9024.crgltf.gl.rendered.impl;

import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;

import java.util.List;

public class GL21RenderedMeshModelCreator {

	public GL21RenderedMeshPrimitiveModelCreator renderedMeshPrimitiveModelCreator;

	public GL21RenderedMeshModel create(MeshModel meshModel) {
		List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
		GL21RenderedMeshModel renderedMeshModel = new GL21RenderedMeshModel();
		renderedMeshModel.renderedMeshPrimitiveModels = new GL21RenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
		renderedMeshModel.weights = meshModel.getWeights();
		if (renderedMeshModel.weights != null) {
			int allZeroWeightsLength = renderedMeshPrimitiveModelCreator.allZeroWeightsLength;
			for (int i = 0; i < meshPrimitiveModels.size(); i++) {
				renderedMeshModel.renderedMeshPrimitiveModels[i] = renderedMeshPrimitiveModelCreator.create(meshPrimitiveModels.get(i));
			}
			renderedMeshPrimitiveModelCreator.allZeroWeightsLength = allZeroWeightsLength;
		} else {
			for (int i = 0; i < meshPrimitiveModels.size(); i++) {
				renderedMeshModel.renderedMeshPrimitiveModels[i] = renderedMeshPrimitiveModelCreator.create(meshPrimitiveModels.get(i));
			}
		}
		return renderedMeshModel;
	}

}
