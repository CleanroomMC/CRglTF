package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshModel;

public class GL21DaxShaderRenderedMeshModel extends GL21RenderedMeshModel {

	public GL21DaxShaderRenderedMeshPrimitiveModel[] daxShaderRenderedMeshPrimitiveModels;

	public void renderMeshPrimitiveModelsForDaxShader() {
		if (weights != null) {
			for (GL21DaxShaderRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : daxShaderRenderedMeshPrimitiveModels) {
				renderedMeshPrimitiveModel.weights = weights;
				renderedMeshPrimitiveModel.renderForDaxShader();
			}
		} else {
			for (GL21DaxShaderRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : daxShaderRenderedMeshPrimitiveModels) {
				renderedMeshPrimitiveModel.weights = allZeroWeights;
				renderedMeshPrimitiveModel.renderForDaxShader();
			}
		}
	}

	public void renderMeshPrimitiveModelsForDaxShader(float[] nodeWeights) {
		for (GL21DaxShaderRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : daxShaderRenderedMeshPrimitiveModels) {
			renderedMeshPrimitiveModel.weights = nodeWeights;
			renderedMeshPrimitiveModel.renderForDaxShader();
		}
	}
}
