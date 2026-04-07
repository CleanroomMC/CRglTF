package com.timlee9024.crgltf.gl;

import org.lwjgl.opengl.EXTBindableUniform;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;

public class GL31Abstraction {

	public static boolean openGL31UniformBufferObject;

	public static int getMaxUniformBufferSize() {
		if (openGL31UniformBufferObject) {
			return GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BLOCK_SIZE);
		} else {
			return GL11.glGetInteger(EXTBindableUniform.GL_MAX_BINDABLE_UNIFORM_SIZE_EXT);
		}
	}

	public static void glBindUniformBuffer(int buffer) {
		if (openGL31UniformBufferObject) {
			GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, buffer);
		} else {
			GL15.glBindBuffer(EXTBindableUniform.GL_UNIFORM_BUFFER_EXT, buffer);
		}
	}

	public static void glUniformBufferData(long size, int usage) {
		if (openGL31UniformBufferObject) {
			GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, size, usage);
		} else {
			GL15.glBufferData(EXTBindableUniform.GL_UNIFORM_BUFFER_EXT, size, usage);
		}
	}

	public static void glUniformBufferData(FloatBuffer data, int usage) {
		if (openGL31UniformBufferObject) {
			GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, data, usage);
		} else {
			GL15.glBufferData(EXTBindableUniform.GL_UNIFORM_BUFFER_EXT, data, usage);
		}
	}

	public static void glUniformBufferSubData(long offset, FloatBuffer data) {
		if (openGL31UniformBufferObject) {
			GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, offset, data);
		} else {
			GL15.glBufferSubData(EXTBindableUniform.GL_UNIFORM_BUFFER_EXT, offset, data);
		}
	}

	public static void glBindUniformBufferBase(int program, int index, int buffer) {
		if (openGL31UniformBufferObject) {
			GL30Abstraction.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, index, buffer);
		} else {
			EXTBindableUniform.glUniformBufferEXT(program, index, buffer);
		}
	}

}
