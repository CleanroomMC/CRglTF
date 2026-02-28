package com.timlee9024.crgltf.gl.rendered.impl;

import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class DefaultRenderedMeshModelCreator {

	public boolean hasMorphTargets;
	public DefaultRenderedMeshPrimitiveModelCreator renderedMeshPrimitiveModelCreator;

	public DefaultRenderedMeshModel create(MeshModel meshModel) {
		List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
		DefaultRenderedMeshPrimitiveModel[] renderedMeshPrimitiveModels = new DefaultRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
		for (int i = 0; i < renderedMeshPrimitiveModels.length; i++) {
			DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = renderedMeshPrimitiveModelCreator.create(meshPrimitiveModels.get(i));
			renderedMeshPrimitiveModels[i] = renderedMeshPrimitiveModel;
			if (renderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY)
				hasMorphTargets = true;
		}

		DefaultRenderedMeshModel renderedMeshModel = new DefaultRenderedMeshModel();
		renderedMeshModel.renderedMeshPrimitiveModels = renderedMeshPrimitiveModels;

		float[] weights = meshModel.getWeights();
		if (weights != null) {
			renderedMeshModel.weights = weights;
		} else {
			renderedMeshModel.weights = ArrayUtils.EMPTY_FLOAT_ARRAY;
		}
		return renderedMeshModel;
	}

	public DefaultRenderedMeshModel createAlias(MeshModel meshModel, DefaultRenderedMeshModel baseRenderedMeshModel) {
		for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : baseRenderedMeshModel.renderedMeshPrimitiveModels) {
			if (renderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY || renderedMeshPrimitiveModel.skinning != DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY) {
				DefaultRenderedMeshModel renderedMeshModel = new DefaultRenderedMeshModel();
				List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
				renderedMeshModel.renderedMeshPrimitiveModels = new DefaultRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
				for (int i = 0; i < renderedMeshModel.renderedMeshPrimitiveModels.length; i++) {
					renderedMeshPrimitiveModel = renderedMeshModel.renderedMeshPrimitiveModels[i] = renderedMeshPrimitiveModelCreator.createAlias(meshPrimitiveModels.get(i), baseRenderedMeshModel.renderedMeshPrimitiveModels[i]);
					if (renderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY)
						hasMorphTargets = true;
				}
				renderedMeshModel.weights = baseRenderedMeshModel.weights;
				return renderedMeshModel;
			}
		}
		return baseRenderedMeshModel;
	}

}
