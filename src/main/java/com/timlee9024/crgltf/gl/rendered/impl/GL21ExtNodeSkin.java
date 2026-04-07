package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.GL31Abstraction;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import de.javagl.jgltf.model.ElementType;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class GL21ExtNodeSkin {
	public static final GL21ExtNodeSkin DUMMY = new GL21ExtNodeSkin() {

		@Override
		public void runCalcSkinMatrixPass(GL21ExtRenderedMeshModel[] renderedMeshModels) {
		}

		@Override
		public void runApplySkinMatrixPass(GL21ExtRenderedMeshModel[] renderedMeshModels) {
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

	public void runCalcSkinMatrixPass(GL21ExtRenderedMeshModel[] renderedMeshModels) {
		if (isAllJointZeroMatrixChecked) {
			if (isAllJointZeroMatrix) return;
		} else {
			if (checkAllJointsZeroMatrix()) return;
		}

		GL31Abstraction.glBindUniformBufferBase(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getGlProgram(), GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointMatricesUniformBuffer(), glJointMatrixBuffer);

		GL31Abstraction.glBindUniformBuffer(glJointMatrixBuffer);
		for (int startJoint = 0; startJoint < jointNodeAccessors.length; startJoint += GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize()) {
			GL20.glUniform1i(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getStartJointUniform(), startJoint);
			try (MemoryStack stack = MemoryStack.stackPush()) {
				int jointSize = jointNodeAccessors.length - startJoint;
				if (jointSize > GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize()) jointSize = GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize();
				FloatBuffer jointMatrixBuffer = stack.mallocFloat(jointSize * ElementType.MAT4.getNumComponents());
				for (int i = 0; i < jointSize; i++) {
					jointNodeAccessors[i + startJoint].getGlobalTransformMatrix().get(i * ElementType.MAT4.getNumComponents(), jointMatrixBuffer);
				}
				GL31Abstraction.glUniformBufferSubData(0, jointMatrixBuffer);
			}
			for (GL21ExtRenderedMeshModel renderedMeshModel : renderedMeshModels) {
				for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.skinning.calculateSkinMatrix(startJoint / GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize());
				}
			}
		}
	}

	public void runApplySkinMatrixPass(GL21ExtRenderedMeshModel[] renderedMeshModels) {
		if (isAllJointZeroMatrix) return;
		for (GL21ExtRenderedMeshModel renderedMeshModel : renderedMeshModels) {
			for (GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
				renderedMeshPrimitiveModel.skinning.applySkinMatrix();
			}
		}
	}
}
