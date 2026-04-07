package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedMeshModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedMeshModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedMeshPrimitiveModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class GL21ExtDaxShaderRenderedMeshModelCreator extends GL21ExtRenderedMeshModelCreator {

	@Override
	public GL21ExtDaxShaderRenderedMeshModel create(MeshModel meshModel) {
		List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
		GL21ExtDaxShaderRenderedMeshPrimitiveModel[] renderedMeshPrimitiveModels = new GL21ExtDaxShaderRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
		for (int i = 0; i < renderedMeshPrimitiveModels.length; i++) {
			GL21ExtDaxShaderRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = (GL21ExtDaxShaderRenderedMeshPrimitiveModel) renderedMeshPrimitiveModelCreator.create(meshPrimitiveModels.get(i));
			renderedMeshPrimitiveModels[i] = renderedMeshPrimitiveModel;
			if (renderedMeshPrimitiveModel.morphing != GL21ExtRenderedMeshPrimitiveModel.Morphing.DUMMY)
				hasMorphTargets = true;
		}

		GL21ExtDaxShaderRenderedMeshModel renderedMeshModel = new GL21ExtDaxShaderRenderedMeshModel();
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
	public GL21ExtRenderedMeshModel createAlias(MeshModel meshModel, GL21ExtRenderedMeshModel baseRenderedMeshModel) {
		for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : baseRenderedMeshModel.renderedMeshPrimitiveModels) {
			if (renderedMeshPrimitiveModel.morphing != GL21ExtRenderedMeshPrimitiveModel.Morphing.DUMMY || renderedMeshPrimitiveModel.skinning != GL21ExtRenderedMeshPrimitiveModel.Skinning.DUMMY) {
				GL21ExtDaxShaderRenderedMeshModel renderedMeshModel = new GL21ExtDaxShaderRenderedMeshModel();
				List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
				renderedMeshModel.renderedMeshPrimitiveModels = renderedMeshModel.daxShaderRenderedMeshPrimitiveModels = new GL21ExtDaxShaderRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
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
