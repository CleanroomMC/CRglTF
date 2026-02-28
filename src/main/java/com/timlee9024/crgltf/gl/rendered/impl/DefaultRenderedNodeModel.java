package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfCalcJointMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import de.javagl.jgltf.model.ElementType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

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

	public Skinning skinning;

	@Override
	public void setWeights(float[] weights) {
		morphing.weights = weights;
	}

	@Override
	public float[] getWeights() {
		return morphing.weights;
	}

	public void renderMeshModels() {
		if (skinning == DefaultRenderedNodeModel.Skinning.DUMMY) {
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
			skinning.isAllJointZeroMatrixChecked = false;
			if (skinning.isAllJointZeroMatrix) return;
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
			if (skinning == DefaultRenderedNodeModel.Skinning.DUMMY) {
				if (isGlobalTransformZeroMatrix()) {
					weights = originalWeights;
					return;
				}
			} else {
				if (skinning.checkAllJointsZeroMatrix()) {
					weights = originalWeights;
					return;
				}
			}

			for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.morphing.restoreAttributesForMorphing();
				}
			}
			if (weights != null) {
				for (int i = 0; i < weights.length; i++) {
					float weight = weights[i];
					if (weight != 0) {
						for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
							for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
								renderedMeshPrimitiveModel.morphing.applyMorphWeight(i, weight);
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

	public class Skinning {
		public static final Skinning DUMMY = DefaultRenderedNodeModel.DUMMY.new Skinning() {

			@Override
			public void runCalcSkinMatrixPass() {
			}

			@Override
			public void runApplySkinMatrixPass() {
			}

		};

		public boolean isAllJointZeroMatrixChecked;

		public boolean isAllJointZeroMatrix;

		public NodeAccessor[] jointNodeAccessors;

		public int glJointMatrixBuffer;

		public boolean checkAllJointsZeroMatrix() {
			for (NodeAccessor nodeAccessor : jointNodeAccessors) {
				if (!nodeAccessor.isGlobalTransformZeroMatrix()) {
					isAllJointZeroMatrix = false;
					isAllJointZeroMatrixChecked = true;
					return false;
				}
			}
			isAllJointZeroMatrix = true;
			isAllJointZeroMatrixChecked = true;
			return true;
		}

		public void runCalcJointMatrixPass() {
		}

		public void runCalcSkinMatrixPass() {
			if (isAllJointZeroMatrixChecked) {
				if (isAllJointZeroMatrix) return;
			} else {
				if (checkAllJointsZeroMatrix()) return;
			}
			try (MemoryStack stack = MemoryStack.stackPush()) {
				FloatBuffer jointMatrixBuffer = stack.mallocFloat(jointNodeAccessors.length * ElementType.MAT4.getNumComponents());
				for (int i = 0; i < jointNodeAccessors.length; i++) {
					jointNodeAccessors[i].getGlobalTransformMatrix().get(i * ElementType.MAT4.getNumComponents(), jointMatrixBuffer);
				}
				GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glJointMatrixBuffer);
				GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, jointMatrixBuffer);
			}
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfCalcSkinMatrixPassConstants.getInstance().getJointMatrices(), glJointMatrixBuffer);
			for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.skinning.calculateSkinMatrix();
				}
			}
		}

		public void runApplySkinMatrixPass() {
			if (isAllJointZeroMatrix) return;
			for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.skinning.applySkinMatrix();
				}
			}
		}
	}

	public class SkinningWithInverseBindMatrices extends Skinning {
		public int glInverseBindMatrixVAO;

		@Override
		public void runCalcJointMatrixPass() {
			if (isAllJointZeroMatrixChecked) {
				if (isAllJointZeroMatrix) return;
			} else {
				if (checkAllJointsZeroMatrix()) return;
			}
			try (MemoryStack stack = MemoryStack.stackPush()) {
				FloatBuffer jointMatrixBuffer = stack.mallocFloat(jointNodeAccessors.length * ElementType.MAT4.getNumComponents());
				for (int i = 0; i < jointNodeAccessors.length; i++) {
					jointNodeAccessors[i].getGlobalTransformMatrix().get(i * ElementType.MAT4.getNumComponents(), jointMatrixBuffer);
				}
				GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glJointMatrixBuffer);
				GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, jointMatrixBuffer);
			}

			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfCalcJointMatrixPassConstants.getInstance().getJointMatrices(), glJointMatrixBuffer);
			GL30.glBindVertexArray(glInverseBindMatrixVAO);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, jointNodeAccessors.length);
		}

		@Override
		public void runCalcSkinMatrixPass() {
			if (isAllJointZeroMatrix) return;
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfCalcSkinMatrixPassConstants.getInstance().getJointMatrices(), glJointMatrixBuffer);
			for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.skinning.calculateSkinMatrix();
				}
			}
		}
	}

}
