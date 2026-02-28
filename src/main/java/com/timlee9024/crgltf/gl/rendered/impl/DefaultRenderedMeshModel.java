package com.timlee9024.crgltf.gl.rendered.impl;

public class DefaultRenderedMeshModel {

	public DefaultRenderedMeshPrimitiveModel[] renderedMeshPrimitiveModels;

	public float[] weights;

	public void renderMeshPrimitiveModels() {
		for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
			renderedMeshPrimitiveModel.render();
		}
	}

	public void applyMorphWeight() {
		for (int i = 0; i < weights.length; i++) {
			float weight = weights[i];
			if (weight != 0) {
				for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.morphing.applyMorphWeight(i, weight);
				}
			}
		}
	}

}
