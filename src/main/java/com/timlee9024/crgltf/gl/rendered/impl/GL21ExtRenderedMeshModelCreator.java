package com.timlee9024.crgltf.gl.rendered.impl;

import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class GL21ExtRenderedMeshModelCreator {

	public boolean hasMorphTargets;
	public GL21ExtRenderedMeshPrimitiveModelCreator renderedMeshPrimitiveModelCreator;

	public GL21ExtRenderedMeshModel create(MeshModel meshModel) {
		List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
		GL21ExtRenderedMeshPrimitiveModel[] renderedMeshPrimitiveModels = new GL21ExtRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
		for (int i = 0; i < renderedMeshPrimitiveModels.length; i++) {
			GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = renderedMeshPrimitiveModelCreator.create(meshPrimitiveModels.get(i));
			renderedMeshPrimitiveModels[i] = renderedMeshPrimitiveModel;
			if (renderedMeshPrimitiveModel.morphing != GL21ExtRenderedMeshPrimitiveModel.Morphing.DUMMY)
				hasMorphTargets = true;
		}

		GL21ExtRenderedMeshModel renderedMeshModel = new GL21ExtRenderedMeshModel();
		renderedMeshModel.renderedMeshPrimitiveModels = renderedMeshPrimitiveModels;

		float[] weights = meshModel.getWeights();
		if (weights != null) {
			renderedMeshModel.weights = weights;
		} else {
			renderedMeshModel.weights = ArrayUtils.EMPTY_FLOAT_ARRAY;
		}
		return renderedMeshModel;
	}

	public GL21ExtRenderedMeshModel createAlias(MeshModel meshModel, GL21ExtRenderedMeshModel baseRenderedMeshModel) {
		for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : baseRenderedMeshModel.renderedMeshPrimitiveModels) {
			if (renderedMeshPrimitiveModel.morphing != GL21ExtRenderedMeshPrimitiveModel.Morphing.DUMMY || renderedMeshPrimitiveModel.skinning != GL21ExtRenderedMeshPrimitiveModel.Skinning.DUMMY) {
				GL21ExtRenderedMeshModel renderedMeshModel = new GL21ExtRenderedMeshModel();
				List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
				renderedMeshModel.renderedMeshPrimitiveModels = new GL21ExtRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
				for (int i = 0; i < renderedMeshModel.renderedMeshPrimitiveModels.length; i++) {
					renderedMeshPrimitiveModel = renderedMeshModel.renderedMeshPrimitiveModels[i] = renderedMeshPrimitiveModelCreator.createAlias(meshPrimitiveModels.get(i), baseRenderedMeshModel.renderedMeshPrimitiveModels[i]);
					if (renderedMeshPrimitiveModel.morphing != GL21ExtRenderedMeshPrimitiveModel.Morphing.DUMMY)
						hasMorphTargets = true;
				}
				renderedMeshModel.weights = baseRenderedMeshModel.weights;
				return renderedMeshModel;
			}
		}
		return baseRenderedMeshModel;
	}

}
