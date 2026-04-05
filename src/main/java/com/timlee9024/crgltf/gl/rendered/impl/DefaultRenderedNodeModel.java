package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

public class DefaultRenderedNodeModel extends CommonNodeAccessor {

	public static final DefaultRenderedNodeModel DUMMY = new DefaultRenderedNodeModel() {

		@Override
		public void setWeights(float[] weights) {
		}

		@Override
		public void renderMeshModels() {
		}

	};

	public DefaultRenderedMeshModel[] renderedMeshModels;

	public Morphing morphing;

	public DefaultNodeSkin nodeSkin;

	@Override
	public void setWeights(float[] weights) {
		morphing.weights = weights;
	}

	@Override
	public float[] getWeights() {
		return morphing.weights;
	}

	public void renderMeshModels() {
		if (nodeSkin == DefaultNodeSkin.DUMMY) {
			if (isGlobalTransformZeroMatrix()) return;
			//To match glTF spec requirement for NODE_SKINNED_MESH_LOCAL_TRANSFORMS.
			GL11.glPushMatrix();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				GL11.glMultMatrixf(getGlobalTransformMatrix().get(stack.mallocFloat(16)));
			}
			for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				renderedMeshModel.renderMeshPrimitiveModels();
			}
			GL11.glPopMatrix();
		} else {
			nodeSkin.isAllJointZeroMatrixChecked = false;
			if (nodeSkin.isAllJointZeroMatrix) return;
			for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				renderedMeshModel.renderMeshPrimitiveModels();
			}
		}
	}

	public class Morphing {
		public static final Morphing DUMMY = DefaultRenderedNodeModel.DUMMY.new Morphing() {

			@Override
			public void runMorphingPass() {
			}

		};

		public float[] originalWeights;

		public float[] weights;

		public void runMorphingPass() {
			if (nodeSkin == DefaultNodeSkin.DUMMY) {
				if (isGlobalTransformZeroMatrix()) {
					weights = originalWeights;
					return;
				}
			} else {
				if (nodeSkin.checkAllJointsZeroMatrix()) {
					weights = originalWeights;
					return;
				}
			}

			GL20.glUniform1f(GltfMorphingPassConstants.getInstance().getWeightUniform(), 1);
			for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.morphing.restoreAttributesForMorphing();
				}
			}
			if (weights != null) {
				for (int i = 0; i < weights.length; i++) {
					float weight = weights[i];
					if (weight != 0) {
						GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
						GL20.glUniform1f(GltfMorphingPassConstants.getInstance().getWeightUniform(), weight);
						for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
							for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
								renderedMeshPrimitiveModel.morphing.applyMorphTarget(i);
							}
						}
					}
				}
			} else {
				for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
					renderedMeshModel.applyMorphWeight();
				}
			}
			weights = originalWeights;
		}

	}

}
