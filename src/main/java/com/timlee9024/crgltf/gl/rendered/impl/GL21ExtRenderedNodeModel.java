package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GL21ExtGltfMorphingPassConstants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

public class GL21ExtRenderedNodeModel extends CommonNodeAccessor {

	public static final GL21ExtRenderedNodeModel DUMMY = new GL21ExtRenderedNodeModel() {

		@Override
		public void setWeights(float[] weights) {
		}

		@Override
		public void renderMeshModels() {
		}

	};

	public GL21ExtRenderedMeshModel[] renderedMeshModels;

	public Morphing morphing;

	public GL21ExtNodeSkin nodeSkin;

	@Override
	public void setWeights(float[] weights) {
		morphing.weights = weights;
	}

	@Override
	public float[] getWeights() {
		return morphing.weights;
	}

	public void renderMeshModels() {
		if (nodeSkin == GL21ExtNodeSkin.DUMMY) {
			if (isGlobalTransformZeroMatrix()) return;
			//To match glTF spec requirement for NODE_SKINNED_MESH_LOCAL_TRANSFORMS.
			GL11.glPushMatrix();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				GL11.glMultMatrixf(getGlobalTransformMatrix().get(stack.mallocFloat(16)));
			}
			for (GL21ExtRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				renderedMeshModel.renderMeshPrimitiveModels();
			}
			GL11.glPopMatrix();
		} else {
			nodeSkin.isAllJointZeroMatrixChecked = false;
			if (nodeSkin.isAllJointZeroMatrix) return;
			for (GL21ExtRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				renderedMeshModel.renderMeshPrimitiveModels();
			}
		}
	}

	public class Morphing {
		public static final Morphing DUMMY = GL21ExtRenderedNodeModel.DUMMY.new Morphing() {

			@Override
			public void runMorphingPass() {
			}

		};

		public float[] originalWeights;

		public float[] weights;

		public void runMorphingPass() {
			if (nodeSkin == GL21ExtNodeSkin.DUMMY) {
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

			if (weights != null) {
				boolean zeroOrOne = false;
				for (float weight : weights) {
					if (weight != 0) zeroOrOne = !zeroOrOne;
				}

				for (GL21ExtRenderedMeshModel renderedMeshModel : renderedMeshModels) {
					for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
						renderedMeshPrimitiveModel.morphing.zeroOrOne = zeroOrOne;
						renderedMeshPrimitiveModel.morphing.restoreAttributesForMorphing();
					}
				}

				for (int i = 0; i < weights.length; i++) {
					float weight = weights[i];
					if (weight != 0) {
						GL20.glUniform1f(GL21ExtGltfMorphingPassConstants.getInstance().getWeightUniform(), weight);
						for (GL21ExtRenderedMeshModel renderedMeshModel : renderedMeshModels) {
							for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
								renderedMeshPrimitiveModel.morphing.applyMorphTarget(i);
							}
						}
					}
				}
			} else {
				for (GL21ExtRenderedMeshModel renderedMeshModel : renderedMeshModels) {
					renderedMeshModel.applyMorphWeight();
				}
			}
			weights = originalWeights;
		}

	}

}
