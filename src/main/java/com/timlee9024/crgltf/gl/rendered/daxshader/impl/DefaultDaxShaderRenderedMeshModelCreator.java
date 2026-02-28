package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshPrimitiveModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class DefaultDaxShaderRenderedMeshModelCreator extends DefaultRenderedMeshModelCreator {

	@Override
	public DefaultRenderedMeshModel create(MeshModel meshModel) {
		List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
		DefaultDaxShaderRenderedMeshPrimitiveModel[] renderedMeshPrimitiveModels = new DefaultDaxShaderRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
		for (int i = 0; i < renderedMeshPrimitiveModels.length; i++) {
			DefaultDaxShaderRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = (DefaultDaxShaderRenderedMeshPrimitiveModel) renderedMeshPrimitiveModelCreator.create(meshPrimitiveModels.get(i));
			renderedMeshPrimitiveModels[i] = renderedMeshPrimitiveModel;
			if (renderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY)
				hasMorphTargets = true;
		}

		DefaultDaxShaderRenderedMeshModel renderedMeshModel = new DefaultDaxShaderRenderedMeshModel();
		renderedMeshModel.renderedMeshPrimitiveModels = renderedMeshPrimitiveModels;
		renderedMeshModel.daxShaderRenderedMeshPrimitiveModels = renderedMeshPrimitiveModels;

		float[] weights = meshModel.getWeights();
		if (weights != null) {
			renderedMeshModel.weights = weights;
		} else {
			renderedMeshModel.weights = ArrayUtils.EMPTY_FLOAT_ARRAY;
		}
		return renderedMeshModel;
	}

	@Override
	public DefaultRenderedMeshModel createAlias(MeshModel meshModel, DefaultRenderedMeshModel baseRenderedMeshModel) {
		for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : baseRenderedMeshModel.renderedMeshPrimitiveModels) {
			if (renderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY || renderedMeshPrimitiveModel.skinning != DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY) {
				DefaultDaxShaderRenderedMeshModel renderedMeshModel = new DefaultDaxShaderRenderedMeshModel();
				List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
				renderedMeshModel.renderedMeshPrimitiveModels = renderedMeshModel.daxShaderRenderedMeshPrimitiveModels = new DefaultDaxShaderRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
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
