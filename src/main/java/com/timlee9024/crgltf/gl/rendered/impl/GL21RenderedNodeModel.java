package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class GL21RenderedNodeModel extends CommonNodeAccessor {

	public Matrix4f[] calculatedJointMatrices;

	public Matrix4fc[] jointMatrices;

	public GL21RenderedMeshModel[] renderedMeshModels;

	public float[] originalWeights;

	public float[] weights;

	public NodeAccessor[] jointNodeAccessors;

	public Matrix4fc[] inverseBindMatrices;

	public void renderMeshModels() {
		if (jointNodeAccessors != null) {
			boolean isAllJointZeroMatrix = true;
			if (inverseBindMatrices != null) {
				for (int i = 0; i < jointNodeAccessors.length; i++) {
					NodeAccessor nodeAccessor = jointNodeAccessors[i];
					if (nodeAccessor.isGlobalTransformZeroMatrix()) {
						jointMatrices[i] = NodeAccessor.ZERO_MATRIX;
					} else {
						jointMatrices[i] = jointNodeAccessors[i].getGlobalTransformMatrix().mul(inverseBindMatrices[i], calculatedJointMatrices[i]);
						isAllJointZeroMatrix = false;
					}
				}
			} else {
				for (int i = 0; i < jointNodeAccessors.length; i++) {
					NodeAccessor nodeAccessor = jointNodeAccessors[i];
					if (nodeAccessor.isGlobalTransformZeroMatrix()) {
						jointMatrices[i] = NodeAccessor.ZERO_MATRIX;
					} else {
						jointMatrices[i] = nodeAccessor.getGlobalTransformMatrix();
						isAllJointZeroMatrix = false;
					}
				}
			}
			if (isAllJointZeroMatrix) {
				weights = originalWeights;
				return;
			}

			if (weights != null) {
				for (GL21RenderedMeshModel renderedMeshModel : renderedMeshModels) {
					renderedMeshModel.renderMeshPrimitiveModels(weights);
				}
			} else {
				for (GL21RenderedMeshModel renderedMeshModel : renderedMeshModels) {
					renderedMeshModel.renderMeshPrimitiveModels();
				}
			}
		} else {
			if (!isGlobalTransformZeroMatrix()) {
				//To match glTF spec requirement for NODE_SKINNED_MESH_LOCAL_TRANSFORMS.
				GL11.glPushMatrix();
				try (MemoryStack stack = MemoryStack.stackPush()) {
					GL11.glMultMatrixf(getGlobalTransformMatrix().get(stack.mallocFloat(16)));
				}
				if (weights != null) {
					for (GL21RenderedMeshModel renderedMeshModel : renderedMeshModels) {
						renderedMeshModel.renderMeshPrimitiveModels(weights);
					}
				} else {
					for (GL21RenderedMeshModel renderedMeshModel : renderedMeshModels) {
						renderedMeshModel.renderMeshPrimitiveModels();
					}
				}
				GL11.glPopMatrix();
			}
		}
		weights = originalWeights;
	}

	@Override
	public void setWeights(float[] weights) {
		this.weights = weights;
	}

	@Override
	public float[] getWeights() {
		return weights;
	}
}
