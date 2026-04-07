package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedMeshModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedNodeModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedNodeModelCreator;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;

import java.util.List;

public class GL21ExtDaxShaderRenderedNodeModelCreator extends GL21ExtRenderedNodeModelCreator {

	@Override
	public GL21ExtDaxShaderRenderedNodeModel create(NodeModel nodeModel) {
		List<MeshModel> meshModels = nodeModel.getMeshModels();
		int meshModelCount = meshModels.size();
		if (meshModelCount == 0) return null;

		renderedMeshModelCreator.hasMorphTargets = false;
		GL21ExtDaxShaderRenderedMeshModel[] renderedMeshModels = new GL21ExtDaxShaderRenderedMeshModel[meshModelCount];
		for (int i = 0; i < meshModelCount; i++) {
			MeshModel meshModel = meshModels.get(i);
			GL21ExtRenderedMeshModel renderedMeshModel = renderedMeshModelLookup.get(meshModel);
			if (renderedMeshModel == null) {
				renderedMeshModelLookup.put(meshModel, renderedMeshModels[i] = (GL21ExtDaxShaderRenderedMeshModel) renderedMeshModelCreator.create(meshModel));
			} else {
				renderedMeshModels[i] = (GL21ExtDaxShaderRenderedMeshModel) renderedMeshModelCreator.createAlias(meshModel, renderedMeshModel);
			}
		}

		GL21ExtDaxShaderRenderedNodeModel renderedNodeModel = new GL21ExtDaxShaderRenderedNodeModel();
		renderedNodeModel.renderedMeshModels = renderedNodeModel.daxShaderRenderedMeshModels = renderedMeshModels;

		if (renderedMeshModelCreator.hasMorphTargets) {
			renderedNodeModel.morphing = renderedNodeModel.new Morphing();
			renderedNodeModel.morphing.weights = renderedNodeModel.morphing.originalWeights = nodeModel.getWeights();
		} else renderedNodeModel.morphing = GL21ExtRenderedNodeModel.Morphing.DUMMY;
		return renderedNodeModel;
	}
}
