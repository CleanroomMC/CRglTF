package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshModel;

public class DefaultDaxShaderRenderedMeshModel extends DefaultRenderedMeshModel {

	public DefaultDaxShaderRenderedMeshPrimitiveModel[] daxShaderRenderedMeshPrimitiveModels;

	public void renderMeshPrimitiveModelsForDaxShader() {
		for (DefaultDaxShaderRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : daxShaderRenderedMeshPrimitiveModels) {
			renderedMeshPrimitiveModel.renderForDaxShader();
		}
	}
}
