package com.timlee9024.crgltf.gl.rendered.impl;

public class GL21RenderedMeshModel {

	public float[] allZeroWeights;

	public GL21RenderedMeshPrimitiveModel[] renderedMeshPrimitiveModels;

	public float[] weights;

	public void renderMeshPrimitiveModels() {
		if (weights != null) {
			for (GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
				renderedMeshPrimitiveModel.weights = weights;
				renderedMeshPrimitiveModel.render();
			}
		} else {
			for (GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
				renderedMeshPrimitiveModel.weights = allZeroWeights;
				renderedMeshPrimitiveModel.render();
			}
		}
	}

	public void renderMeshPrimitiveModels(float[] nodeWeights) {
		for (GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
			renderedMeshPrimitiveModel.weights = nodeWeights;
			renderedMeshPrimitiveModel.render();
		}
	}
}
