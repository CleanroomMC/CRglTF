package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedNodeModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedNodeModelCreator;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;

import java.util.List;

public class DefaultDaxShaderRenderedNodeModelCreator extends DefaultRenderedNodeModelCreator {

	@Override
	public DefaultRenderedNodeModel create(NodeModel nodeModel) {
		List<MeshModel> meshModels = nodeModel.getMeshModels();
		int meshModelCount = meshModels.size();
		if (meshModelCount == 0) return null;

		renderedMeshModelCreator.hasMorphTargets = false;
		DefaultDaxShaderRenderedMeshModel[] renderedMeshModels = new DefaultDaxShaderRenderedMeshModel[meshModelCount];
		for (int i = 0; i < meshModelCount; i++) {
			MeshModel meshModel = meshModels.get(i);
			DefaultRenderedMeshModel renderedMeshModel = renderedMeshModelLookup.get(meshModel);
			if (renderedMeshModel == null) {
				renderedMeshModelLookup.put(meshModel, renderedMeshModels[i] = (DefaultDaxShaderRenderedMeshModel) renderedMeshModelCreator.create(meshModel));
			} else {
				renderedMeshModels[i] = (DefaultDaxShaderRenderedMeshModel) renderedMeshModelCreator.createAlias(meshModel, renderedMeshModel);
			}
		}

		DefaultDaxShaderRenderedNodeModel renderedNodeModel = new DefaultDaxShaderRenderedNodeModel();
		renderedNodeModel.renderedMeshModels = renderedNodeModel.daxShaderRenderedMeshModels = renderedMeshModels;

		if (renderedMeshModelCreator.hasMorphTargets) {
			renderedNodeModel.morphing = renderedNodeModel.new Morphing();
			renderedNodeModel.morphing.weights = renderedNodeModel.morphing.originalWeights = nodeModel.getWeights();
		} else renderedNodeModel.morphing = DefaultRenderedNodeModel.Morphing.DUMMY;
		return renderedNodeModel;
	}
}
