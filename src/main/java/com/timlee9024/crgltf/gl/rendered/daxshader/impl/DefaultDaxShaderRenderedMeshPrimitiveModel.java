package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshPrimitiveModel;

public class DefaultDaxShaderRenderedMeshPrimitiveModel extends DefaultRenderedMeshPrimitiveModel {

	public DefaultDaxShaderRenderedMaterialModel daxShaderRenderedMaterialModel;
	public Runnable glDaxShaderDraw;
	public int glDaxShaderRenderVAO;

	public void renderForDaxShader() {
		daxShaderRenderedMaterialModel.renderForDaxShader(glDaxShaderDraw);
	}
}
