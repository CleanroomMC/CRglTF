package com.timlee9024.crgltf.gl.constants;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL20;

public class GltfApplySkinMatrixPassConstants {

	private static GltfApplySkinMatrixPassConstants instance;

	protected final int glProgram;

	public static GltfApplySkinMatrixPassConstants getInstance() {
		return instance;
	}

	public GltfApplySkinMatrixPassConstants() {
		glProgram = createGlProgram();
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected int createGlProgram() {
		int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		GL20.glShaderSource(glShader,
				"#version 430\r\n"
						+ "layout(location = " + getPositionIn() + ") in vec3 position;"
						+ "layout(location = " + getNormalIn() + ") in vec3 normal;"
						+ "layout(location = " + getTangentIn() + ") in vec4 tangent;"
						+ "layout(location = " + getSkinMatrixIn() + ") in mat4 skinMatrix;"
						+ "layout(std430, binding = " + getPositionOut() + ") restrict writeonly buffer positionBuffer {vec4 positionOut[];};"
						+ "layout(std430, binding = " + getNormalOut() + ") restrict writeonly buffer normalBuffer {vec4 normalOut[];};"
						+ "layout(std430, binding = " + getTangentOut() + ") restrict writeonly buffer tangentBuffer {vec4 tangentOut[];};"
						+ "void main() {"
						+ "positionOut[gl_VertexID] = skinMatrix * vec4(position, 1.0);"
						+ "mat3 upperLeft = mat3(skinMatrix);"
						+ "normalOut[gl_VertexID].xyz = upperLeft * normal;"
						+ "tangentOut[gl_VertexID].xyz = upperLeft * tangent.xyz;"
						+ "tangentOut[gl_VertexID].w = tangent.w;"
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

	public int getPositionIn() {
		return 0;
	}

	public int getNormalIn() {
		return 1;
	}

	public int getTangentIn() {
		return 2;
	}

	public int getSkinMatrixIn() {
		return 3;
	}

	public int getPositionOut() {
		return 0;
	}

	public int getNormalOut() {
		return 1;
	}

	public int getTangentOut() {
		return 2;
	}

}
