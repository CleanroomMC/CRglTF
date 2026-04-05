package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshPrimitiveModel;

public class DefaultDaxShaderRenderedMeshPrimitiveModel extends DefaultRenderedMeshPrimitiveModel {

	public static final DefaultDaxShaderRenderedMeshPrimitiveModel DUMMY = new DefaultDaxShaderRenderedMeshPrimitiveModel() {

		@Override
		public void render() {
		}

		@Override
		public void renderForDaxShader() {
		}

	};

	static {
		DUMMY.morphing = Morphing.DUMMY;
		DUMMY.skinning = Skinning.DUMMY;
	}

	public DefaultDaxShaderRenderedMaterialModel daxShaderRenderedMaterialModel;
	public Runnable glDaxShaderDraw;
	public int glDaxShaderRenderVAO;

	public void renderForDaxShader() {
		daxShaderRenderedMaterialModel.renderForDaxShader(glDaxShaderDraw);
	}
}
