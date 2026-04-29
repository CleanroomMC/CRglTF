package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import de.javagl.jgltf.model.ElementType;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class DefaultNodeSkin {
	public static final DefaultNodeSkin DUMMY = new DefaultNodeSkin() {

		@Override
		public void runCalcJointMatrixPass() {
		}

		@Override
		public void runCalcSkinMatrixPass(DefaultRenderedMeshModel[] renderedMeshModels) {
		}

		@Override
		public void runApplySkinMatrixPass(DefaultRenderedMeshModel[] renderedMeshModels) {
		}

	};

	public boolean isAllJointZeroMatrix = true;

	public NodeAccessor[] jointNodeAccessors;

	public int glJointMatrixBuffer;

	public void runCalcJointMatrixPass() {
		for (NodeAccessor nodeAccessor : jointNodeAccessors) {
			if (!nodeAccessor.isGlobalTransformZeroMatrix()) {
				isAllJointZeroMatrix = false;

				try (MemoryStack stack = MemoryStack.stackPush()) {
					FloatBuffer jointMatrixBuffer = stack.mallocFloat(jointNodeAccessors.length * ElementType.MAT4.getNumComponents());
					for (int i = 0; i < jointNodeAccessors.length; i++) {
						jointNodeAccessors[i].getGlobalTransformMatrix().get(i * ElementType.MAT4.getNumComponents(), jointMatrixBuffer);
					}
					GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glJointMatrixBuffer);
					GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, jointMatrixBuffer);
				}
				break;
			}
		}
	}

	public void runCalcSkinMatrixPass(DefaultRenderedMeshModel[] renderedMeshModels) {
		if (isAllJointZeroMatrix) return;
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfCalcSkinMatrixPassConstants.getInstance().getJointMatrixBufferBinding(), glJointMatrixBuffer);
		for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
			for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
				renderedMeshPrimitiveModel.skinning.calculateSkinMatrix();
			}
		}
	}

	public void runApplySkinMatrixPass(DefaultRenderedMeshModel[] renderedMeshModels) {
		if (isAllJointZeroMatrix) return;
		for (DefaultRenderedMeshModel renderedMeshModel : renderedMeshModels) {
			for (DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
				renderedMeshPrimitiveModel.skinning.applySkinMatrix();
			}
		}
	}
}
