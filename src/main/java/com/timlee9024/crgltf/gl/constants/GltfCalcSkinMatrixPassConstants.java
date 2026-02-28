package com.timlee9024.crgltf.gl.constants;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL20;

public class GltfCalcSkinMatrixPassConstants {

	private static GltfCalcSkinMatrixPassConstants instance;

	protected final int glProgram;

	public static GltfCalcSkinMatrixPassConstants getInstance() {
		return instance;
	}

	public GltfCalcSkinMatrixPassConstants() {
		glProgram = createGlProgram();
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected int createGlProgram() {
		int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		GL20.glShaderSource(glShader,
				"#version 430\r\n"
						+ "layout(location = " + getJointIn() + ") in vec4 joint;"
						+ "layout(location = " + getWeightIn() + ") in vec4 weight;"
						+ "layout(std430, binding = " + getJointMatrices() + ") restrict readonly buffer jointMatrixBuffer {mat4 jointMatrices[];};"
						+ "layout(std430, binding = " + getSkinMatrices() + ") restrict buffer skinMatrixBuffer {mat4 skinMatrices[];};"
						+ "void main() {"
						+ "skinMatrices[gl_VertexID] +="
						+ " weight.x * jointMatrices[int(joint.x)] +"
						+ " weight.y * jointMatrices[int(joint.y)] +"
						+ " weight.z * jointMatrices[int(joint.z)] +"
						+ " weight.w * jointMatrices[int(joint.w)];"
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

	public int getJointIn() {
		return 0;
	}

	public int getWeightIn() {
		return 1;
	}

	public int getJointMatrices() {
		return 0;
	}

	public int getSkinMatrices() {
		return 1;
	}

}
