package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GL21ExtGltfMorphingPassConstants;
import org.lwjgl.opengl.GL20;

public class GL21ExtRenderedMeshModel {

	public GL21ExtRenderedMeshPrimitiveModel[] renderedMeshPrimitiveModels;

	public float[] weights;

	public void renderMeshPrimitiveModels() {
		for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
			renderedMeshPrimitiveModel.render();
		}
	}

	public void applyMorphWeight() {
		boolean zeroOrOne = false;
		for (float weight : weights) {
			if (weight != 0) zeroOrOne = !zeroOrOne;
		}

		for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
			renderedMeshPrimitiveModel.morphing.zeroOrOne = zeroOrOne;
			renderedMeshPrimitiveModel.morphing.restoreAttributesForMorphing();
		}

		for (int i = 0; i < weights.length; i++) {
			float weight = weights[i];
			if (weight != 0) {
				GL20.glUniform1f(GL21ExtGltfMorphingPassConstants.getInstance().getWeightUniform(), weight);
				for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.morphing.applyMorphTarget(i);
				}
			}
		}
	}

}
