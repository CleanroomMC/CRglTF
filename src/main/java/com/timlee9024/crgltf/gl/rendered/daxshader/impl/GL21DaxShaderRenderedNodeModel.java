package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedNodeModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class GL21DaxShaderRenderedNodeModel extends GL21RenderedNodeModel {

	public GL21DaxShaderRenderedMeshModel[] daxShaderRenderedMeshModels;

	public void renderMeshModelsForDaxShader() {
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
				for (GL21DaxShaderRenderedMeshModel renderedMeshModel : daxShaderRenderedMeshModels) {
					renderedMeshModel.renderMeshPrimitiveModelsForDaxShader(weights);
				}
			} else {
				for (GL21DaxShaderRenderedMeshModel renderedMeshModel : daxShaderRenderedMeshModels) {
					renderedMeshModel.renderMeshPrimitiveModelsForDaxShader();
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
					for (GL21DaxShaderRenderedMeshModel renderedMeshModel : daxShaderRenderedMeshModels) {
						renderedMeshModel.renderMeshPrimitiveModelsForDaxShader(weights);
					}
				} else {
					for (GL21DaxShaderRenderedMeshModel renderedMeshModel : daxShaderRenderedMeshModels) {
						renderedMeshModel.renderMeshPrimitiveModelsForDaxShader();
					}
				}
				GL11.glPopMatrix();
			}
		}
	}
}
