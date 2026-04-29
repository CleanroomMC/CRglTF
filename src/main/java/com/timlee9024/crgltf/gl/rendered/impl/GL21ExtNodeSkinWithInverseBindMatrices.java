package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import com.timlee9024.crgltf.gl.GL31Abstraction;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import de.javagl.jgltf.model.ElementType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class GL21ExtNodeSkinWithInverseBindMatrices extends GL21ExtNodeSkin {
	public int glJointAndInverseBindMatrixVAO;
	public int[] glJointMatrixBuffers;

	@Override
	public void runCalcJointMatrixPass() {
		for (NodeAccessor nodeAccessor : jointNodeAccessors) {
			if (!nodeAccessor.isGlobalTransformZeroMatrix()) {
				isAllJointZeroMatrix = false;

				try (MemoryStack stack = MemoryStack.stackPush()) {
					FloatBuffer jointMatrixBuffer = stack.mallocFloat(jointNodeAccessors.length * ElementType.MAT4.getNumComponents());
					for (int i = 0; i < jointNodeAccessors.length; i++) {
						jointNodeAccessors[i].getGlobalTransformMatrix().get(i * ElementType.MAT4.getNumComponents(), jointMatrixBuffer);
					}
					GL31Abstraction.glBindUniformBuffer(glJointMatrixBuffer); //No need to unbind GL_ARRAY_BUFFER by this workaround
					GL31Abstraction.glUniformBufferSubData(0, jointMatrixBuffer);
				}

				GL30Abstraction.glBindVertexArray(glJointAndInverseBindMatrixVAO);
				for (int i = 0; i < glJointMatrixBuffers.length; i++) {
					GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glJointMatrixBuffers[i]);
					int startJoint = i * GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize();
					int jointSize = jointNodeAccessors.length - startJoint;
					if (jointSize > GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize()) jointSize = GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize();
					GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
					GL11.glDrawArrays(GL11.GL_POINTS, startJoint, jointSize);
					GL30Abstraction.glEndTransformFeedback();
				}
				break;
			}
		}
	}

	@Override
	public void runCalcSkinMatrixPass(GL21ExtRenderedMeshModel[] renderedMeshModels) {
		if (isAllJointZeroMatrix) return;

		for (int i = 0; i < glJointMatrixBuffers.length; i++) {
			GL31Abstraction.glBindUniformBufferBase(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getGlProgram(), GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointMatricesUniformBuffer(), glJointMatrixBuffers[i]);
			GL20.glUniform1i(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getStartJointUniform(), i * GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize());
			for (GL21ExtRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.skinning.calculateSkinMatrix(i);
				}
			}
		}
	}
}
