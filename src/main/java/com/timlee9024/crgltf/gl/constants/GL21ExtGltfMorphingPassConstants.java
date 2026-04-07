package com.timlee9024.crgltf.gl.constants;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class GL21ExtGltfMorphingPassConstants {

	private static GL21ExtGltfMorphingPassConstants instance;

	protected final int glProgram;

	protected final int positionLocation;
	protected final int normalLocation;
	protected final int tangentLocation;
	protected final int colorLocation;
	protected final int texcoordLocation;
	protected final int positionTargetLocation;
	protected final int normalTargetLocation;
	protected final int tangentTargetLocation;
	protected final int colorTargetLocation;
	protected final int texcoordTargetLocation;

	protected final int weightLocation;

	protected final int glZeroVec4Buffer;

	public static GL21ExtGltfMorphingPassConstants getInstance() {
		return instance;
	}

	public GL21ExtGltfMorphingPassConstants() {
		glProgram = createGlProgram();

		positionLocation = GL20.glGetAttribLocation(glProgram, "a_position");
		normalLocation = GL20.glGetAttribLocation(glProgram, "a_normal");
		tangentLocation = GL20.glGetAttribLocation(glProgram, "a_tangent");
		colorLocation = GL20.glGetAttribLocation(glProgram, "a_color");
		texcoordLocation = GL20.glGetAttribLocation(glProgram, "a_texcoord");
		positionTargetLocation = GL20.glGetAttribLocation(glProgram, "a_positionTarget");
		normalTargetLocation = GL20.glGetAttribLocation(glProgram, "a_normalTarget");
		tangentTargetLocation = GL20.glGetAttribLocation(glProgram, "a_tangentTarget");
		colorTargetLocation = GL20.glGetAttribLocation(glProgram, "a_colorTarget");
		texcoordTargetLocation = GL20.glGetAttribLocation(glProgram, "a_texcoordTarget");

		weightLocation = GL20.glGetUniformLocation(glProgram, "u_weight");

		glZeroVec4Buffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glZeroVec4Buffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, new float[]{0.0f, 0.0f, 0.0f, 0.0f}, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected int createGlProgram() {
		int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		GL20.glShaderSource(glShader,
		"attribute vec3 a_position;"
			+ "attribute vec3 a_normal;"
			+ "attribute vec4 a_tangent;"
			+ "attribute vec4 a_color;"
			+ "attribute vec2 a_texcoord;"
			+ "attribute vec3 a_positionTarget;"
			+ "attribute vec3 a_normalTarget;"
			+ "attribute vec3 a_tangentTarget;"
			+ "attribute vec4 a_colorTarget;"
			+ "attribute vec2 a_texcoordTarget;"
			+ "uniform float u_weight;"
			+ "varying vec3 v_position;"
			+ "varying vec3 v_normal;"
			+ "varying vec4 v_tangent;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_texcoord;"
			+ "void main() {"
			+ "v_position = a_position + a_positionTarget * u_weight;"
			+ "v_normal = a_normal + a_normalTarget * u_weight;"
			+ "v_tangent.xyz = a_tangent.xyz + a_tangentTarget * u_weight;"
			+ "v_tangent.w = a_tangent.w;"
			+ "v_color = a_color + a_colorTarget * u_weight;"
			+ "v_texcoord = a_texcoord + a_texcoordTarget * u_weight;"
			+ "gl_Position = vec4(0.0);"
			+ "}");
		GL20.glCompileShader(glShader);

		int glProgram = GL20.glCreateProgram();
		GL20.glAttachShader(glProgram, glShader);
		GL20.glDeleteShader(glShader);
		GL30Abstraction.glTransformFeedbackVaryings(glProgram, new CharSequence[]{"v_position", "v_normal", "v_tangent", "v_color", "v_texcoord"}, GL30.GL_INTERLEAVED_ATTRIBS);
		GL20.glLinkProgram(glProgram);

		return glProgram;
	}

	public int getGlProgram() {
		return glProgram;
	}

	public int getPositionAttribute() {
		return positionLocation;
	}

	public int getNormalAttribute() {
		return normalLocation;
	}

	public int getTangentAttribute() {
		return tangentLocation;
	}

	public int getColorAttribute() {
		return colorLocation;
	}

	public int getTexcoordAttribute() {
		return texcoordLocation;
	}

	public int getPositionTargetAttribute() {
		return positionTargetLocation;
	}

	public int getNormalTargetAttribute() {
		return normalTargetLocation;
	}

	public int getTangentTargetAttribute() {
		return tangentTargetLocation;
	}

	public int getColorTargetAttribute() {
		return colorTargetLocation;
	}

	public int getTexcoordTargetAttribute() {
		return texcoordTargetLocation;
	}

	public int getWeightUniform() {
		return weightLocation;
	}

	public int getMorphBufferPositionOffset() {
		return 0;
	}

	public int getMorphBufferNormalOffset() {
		return Float.BYTES * 3;
	}

	public int getMorphBufferTangentOffset() {
		return Float.BYTES * 6;
	}

	public int getMorphBufferColorOffset() {
		return Float.BYTES * 10;
	}

	public int getMorphBufferTexcoordOffset() {
		return Float.BYTES * 14;
	}

	public int getMorphBufferStride() {
		return Float.BYTES * 16;
	}

	public int getGlZeroVec4Buffer() {
		return glZeroVec4Buffer;
	}
}
