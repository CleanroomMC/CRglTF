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
			+ "layout(location = " + getPositionBaseAttribute() + ") in vec3 positionBase;"
			+ "layout(location = " + getNormalBaseAttribute() + ") in vec3 normalBase;"
			+ "layout(location = " + getTangentBaseAttribute() + ") in vec4 tangentBase;"
			+ "layout(std430, binding = " + getSkinBufferBinding() + ") restrict buffer skinBuffer {mat4 skinMatrices[];};"
			+ "void main() {"
			+ "mat3 upperLeft = mat3(skinMatrices[gl_VertexID]);"
			+ "vec4 position = skinMatrices[gl_VertexID] * vec4(positionBase, 1.0);"
			+ "skinMatrices[gl_VertexID][0] = position;"
			+ "skinMatrices[gl_VertexID][1].xyz = upperLeft * normalBase;"
			+ "skinMatrices[gl_VertexID][2].xyz = upperLeft * tangentBase.xyz;"
			+ "skinMatrices[gl_VertexID][2].w = tangentBase.w;"
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

	public int getPositionBaseAttribute() {
		return 0;
	}

	public int getNormalBaseAttribute() {
		return 1;
	}

	public int getTangentBaseAttribute() {
		return 2;
	}

	public int getSkinBufferBinding() {
		return 0;
	}

	public int getSkinBufferPositionOffset() {
		return 0;
	}

	public int getSkinBufferNormalOffset() {
		return Float.BYTES * 4;
	}

	public int getSkinBufferTangentOffset() {
		return Float.BYTES * 8;
	}

	public int getSkinBufferStride() {
		return Float.BYTES * 16;
	}

}
