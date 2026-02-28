package com.timlee9024.crgltf.gl.constants;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL20;

public class GltfCalcJointMatrixPassConstants {

	private static GltfCalcJointMatrixPassConstants instance;

	protected final int glProgram;

	public static GltfCalcJointMatrixPassConstants getInstance() {
		return instance;
	}

	public GltfCalcJointMatrixPassConstants() {
		glProgram = createGlProgram();
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected int createGlProgram() {
		int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		GL20.glShaderSource(glShader,
				"#version 430\r\n"
						+ "layout(location = " + getInverseBindMatrixIn() + ") in mat4 inverseBindMatrix;"
						+ "layout(std430, binding = " + getJointMatrices() + ") restrict buffer jointMatrixBuffer {mat4 jointMatrices[];};"
						+ "void main() {"
						+ "jointMatrices[gl_VertexID] = jointMatrices[gl_VertexID] * inverseBindMatrix;"
						+ "}");
		GL20.glCompileShader(glShader);

		int glProgram = GL20.glCreateProgram();
		GL20.glAttachShader(glProgram, glShader);
		GL20.glDeleteShader(glShader);
		GL20.glLinkProgram(glProgram);

		return glProgram;
	}

	public int getGlProgram() {
		return glProgram;
	}

	public int getInverseBindMatrixIn() {
		return 0;
	}

	public int getJointMatrices() {
		return 0;
	}

}
