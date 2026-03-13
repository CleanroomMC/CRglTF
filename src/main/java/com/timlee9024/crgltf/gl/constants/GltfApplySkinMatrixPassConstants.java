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
						+ "layout(std430, binding = " + getAttributes() + ") restrict writeonly buffer attributesBuffer {vec4 attributes[];};"
						+ "void main() {"
						+ "int offset = gl_VertexID * 3;"
						+ "attributes[offset] = skinMatrix * vec4(position, 1.0);"
						+ "mat3 upperLeft = mat3(skinMatrix);"
						+ "attributes[offset += 1].xyz = upperLeft * normal;"
						+ "attributes[offset += 1].xyz = upperLeft * tangent.xyz;"
						+ "attributes[offset].w = tangent.w;"
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

	public int getAttributes() {
		return 0;
	}

}
