package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshModelCreator;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;

import java.util.List;

public class GL21DaxShaderRenderedMeshModelCreator extends GL21RenderedMeshModelCreator {

	@Override
	public GL21RenderedMeshModel create(MeshModel meshModel) {
		List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();
		GL21DaxShaderRenderedMeshModel renderedMeshModel = new GL21DaxShaderRenderedMeshModel();
		renderedMeshModel.renderedMeshPrimitiveModels = renderedMeshModel.daxShaderRenderedMeshPrimitiveModels = new GL21DaxShaderRenderedMeshPrimitiveModel[meshPrimitiveModels.size()];
		renderedMeshModel.weights = meshModel.getWeights();
		if (renderedMeshModel.weights != null) {
			int allZeroWeightsLength = renderedMeshPrimitiveModelCreator.allZeroWeightsLength;
			for (int i = 0; i < meshPrimitiveModels.size(); i++) {
				renderedMeshModel.renderedMeshPrimitiveModels[i] = renderedMeshModel.daxShaderRenderedMeshPrimitiveModels[i] = (GL21DaxShaderRenderedMeshPrimitiveModel) renderedMeshPrimitiveModelCreator.create(meshPrimitiveModels.get(i));
			}
			renderedMeshPrimitiveModelCreator.allZeroWeightsLength = allZeroWeightsLength;
		} else {
			for (int i = 0; i < meshPrimitiveModels.size(); i++) {
				renderedMeshModel.renderedMeshPrimitiveModels[i] = renderedMeshModel.daxShaderRenderedMeshPrimitiveModels[i] = (GL21DaxShaderRenderedMeshPrimitiveModel) renderedMeshPrimitiveModelCreator.create(meshPrimitiveModels.get(i));
			}
		}
		return renderedMeshModel;
	}
}
