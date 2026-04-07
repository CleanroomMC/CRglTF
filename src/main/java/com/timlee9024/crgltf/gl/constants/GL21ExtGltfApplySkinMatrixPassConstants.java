package com.timlee9024.crgltf.gl.constants;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class GL21ExtGltfApplySkinMatrixPassConstants {

	private static GL21ExtGltfApplySkinMatrixPassConstants instance;

	protected final int glProgram;

	public static GL21ExtGltfApplySkinMatrixPassConstants getInstance() {
		return instance;
	}

	public GL21ExtGltfApplySkinMatrixPassConstants() {
		glProgram = createGlProgram();
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected int createGlProgram() {
		int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		GL20.glShaderSource(glShader,
		"#version 120\r\n"
			+ "attribute vec3 a_position;"
			+ "attribute vec3 a_normal;"
			+ "attribute vec4 a_tangent;"
			+ "attribute mat4 a_skinMatrix;"
			+ "varying vec3 v_position;"
			+ "varying vec3 v_normal;"
			+ "varying vec4 v_tangent;"
			+ "void main() {"
			+ "vec4 position = a_skinMatrix * vec4(a_position, 1.0);"
			+ "v_position = position.xyz;"
			+ "mat3 upperLeft = mat3(a_skinMatrix);"
			+ "v_normal = upperLeft * a_normal;"
			+ "v_tangent.xyz = upperLeft * a_tangent.xyz;"
			+ "v_tangent.w = a_tangent.w;"
			+ "gl_Position = vec4(0.0);"
			+ "}");
		GL20.glCompileShader(glShader);

		int glProgram = GL20.glCreateProgram();
		GL20.glAttachShader(glProgram, glShader);
		GL20.glDeleteShader(glShader);
		GL30Abstraction.glTransformFeedbackVaryings(glProgram, new CharSequence[]{"v_position", "v_normal", "v_tangent"}, GL30.GL_INTERLEAVED_ATTRIBS);
		GL20.glLinkProgram(glProgram);

		return glProgram;
	}

	public int getGlProgram() {
		return glProgram;
	}

	public int getPositionAttribute() {
		return 0;
	}

	public int getNormalAttribute() {
		return 1;
	}

	public int getTangentAttribute() {
		return 2;
	}

	public int getSkinMatrixAttribute() {
		return 3;
	}

	public int getSkinBufferPositionOffset() {
		return 0;
	}

	public int getSkinBufferNormalOffset() {
		return Float.BYTES * 3;
	}

	public int getSkinBufferTangentOffset() {
		return Float.BYTES * 6;
	}

	public int getSkinBufferStride() {
		return Float.BYTES * 10;
	}

}
