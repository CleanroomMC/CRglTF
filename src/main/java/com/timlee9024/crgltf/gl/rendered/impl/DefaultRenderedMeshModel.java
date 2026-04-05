package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

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
				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				GL20.glUniform1f(GltfMorphingPassConstants.getInstance().getWeightUniform(), weight);
				for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.morphing.applyMorphTarget(i);
				}
			}
		}
	}

}
