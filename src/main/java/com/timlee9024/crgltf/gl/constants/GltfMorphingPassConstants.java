package com.timlee9024.crgltf.gl.constants;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL20;

public class GltfMorphingPassConstants {

	private static GltfMorphingPassConstants instance;

	protected final int glProgram;

	public static GltfMorphingPassConstants getInstance() {
		return instance;
	}

	public GltfMorphingPassConstants() {
		glProgram = createGlProgram();
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected int createGlProgram() {
		int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		GL20.glShaderSource(glShader,
				"#version 430\r\n"
						+ "layout(location = " + getPositionTarget() + ") in vec3 positionTarget;"
						+ "layout(location = " + getNormalTarget() + ") in vec3 normalTarget;"
						+ "layout(location = " + getTangentTarget() + ") in vec3 tangentTarget;"
						+ "layout(location = " + getTangentBase() + ") in vec4 tangentBase;"
						+ "layout(location = " + getColorTarget() + ") in vec4 colorTarget;"
						+ "layout(location = " + getTexcoordTarget() + ") in vec2 texcoordTarget;"
						+ "layout(location = " + getWeight() + ") uniform float weight;"
						+ "layout(std430, binding = " + getMorphBuffer() + ") restrict buffer morphBuffer {mat4 vertexDatas[];};"
						+ "void main() {"
						+ "vertexDatas[gl_VertexID][0].xyz += positionTarget * weight;"
						+ "vertexDatas[gl_VertexID][1] += vec4(0, normalTarget * weight);" //vertexDatas[gl_VertexID][1].yzw += vec3(normalTarget * weight).xyz; doesn't seem to working.
						+ "vertexDatas[gl_VertexID][2].xyz += tangentTarget * weight;"
						+ "vertexDatas[gl_VertexID][2].w = tangentBase.w;"
						+ "vertexDatas[gl_VertexID][3] += colorTarget * weight;"
						+ "vec2 texcoord = texcoordTarget * weight;"
						+ "vertexDatas[gl_VertexID][0].w += texcoord.x;"
						+ "vertexDatas[gl_VertexID][1].x += texcoord.y;"
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

	public int getPositionTarget() {
		return 0;
	}

	public int getNormalTarget() {
		return 1;
	}

	public int getTangentTarget() {
		return 2;
	}

	public int getTangentBase() {
		return 3;
	}

	public int getColorTarget() {
		return 4;
	}

	public int getTexcoordTarget() {
		return 5;
	}

	public int getWeight() {
		return 6;
	}

	public int getMorphBuffer() {
		return 0;
	}

	public int getMorphBufferPositionOffset() {
		return 0;
	}

	public int getMorphBufferNormalOffset() {
		return 5;
	}

	public int getMorphBufferTangentOffset() {
		return 8;
	}

	public int getMorphBufferColorOffset() {
		return 12;
	}

	public int getMorphBufferTexcoordOffset() {
		return 3;
	}

	public int getMorphBufferStride() {
		return 16;
	}

}
