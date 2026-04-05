package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfCalcJointMatrixPassConstants;
import de.javagl.jgltf.model.ElementType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class DefaultNodeSkinWithInverseBindMatrices extends DefaultNodeSkin {
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

		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfCalcJointMatrixPassConstants.getInstance().getJointMatrixBufferBinding(), glJointMatrixBuffer);
		GL30.glBindVertexArray(glInverseBindMatrixVAO);
		GL11.glDrawArrays(GL11.GL_POINTS, 0, jointNodeAccessors.length);
	}

}
