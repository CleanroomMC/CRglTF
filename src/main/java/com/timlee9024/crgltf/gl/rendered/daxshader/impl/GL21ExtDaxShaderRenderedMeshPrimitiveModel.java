package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedMeshPrimitiveModel;

public class GL21ExtDaxShaderRenderedMeshPrimitiveModel extends GL21ExtRenderedMeshPrimitiveModel {

	public static final GL21ExtDaxShaderRenderedMeshPrimitiveModel DUMMY = new GL21ExtDaxShaderRenderedMeshPrimitiveModel() {

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
