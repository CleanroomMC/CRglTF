package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedMeshModel;

public class GL21ExtDaxShaderRenderedMeshModel extends GL21ExtRenderedMeshModel {

	public GL21ExtDaxShaderRenderedMeshPrimitiveModel[] daxShaderRenderedMeshPrimitiveModels;

	public void renderMeshPrimitiveModelsForDaxShader() {
		for (GL21ExtDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel : daxShaderRenderedMeshPrimitiveModels) {
			daxShaderRenderedMeshPrimitiveModel.renderForDaxShader();
		}
	}
}
