package com.timlee9024.crgltf.gl.constants;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import com.timlee9024.crgltf.gl.GL31Abstraction;
import de.javagl.jgltf.model.ElementType;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

public class GL21ExtGltfCalcSkinMatrixPassConstants {

	private static GL21ExtGltfCalcSkinMatrixPassConstants instance;

	protected final int maxDrawJointSize;

	protected final int glProgram;

	protected final int jointLocation;
	protected final int weightLocation;
	protected final int skinMatrixLocation;

	protected final int startJointLocation;

	protected final int jointMatricesLocation;

	public static GL21ExtGltfCalcSkinMatrixPassConstants getInstance() {
		return instance;
	}

	public GL21ExtGltfCalcSkinMatrixPassConstants() {
		maxDrawJointSize = GL31Abstraction.getMaxUniformBufferSize() / (Float.BYTES * ElementType.MAT4.getNumComponents());

		glProgram = createGlProgram();

		jointLocation = GL20.glGetAttribLocation(glProgram, "a_joint");
		weightLocation = GL20.glGetAttribLocation(glProgram, "a_weight");
		skinMatrixLocation = GL20.glGetAttribLocation(glProgram, "a_skinMatrix");

		startJointLocation = GL20.glGetUniformLocation(glProgram, "u_startJoint");

		if (GL31Abstraction.openGL31UniformBufferObject) {
			jointMatricesLocation = 0;
		}
		else {
			jointMatricesLocation = GL20.glGetUniformLocation(glProgram, "u_jointMatrices");
		}
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected int createGlProgram() {
		int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		if (GL31Abstraction.openGL31UniformBufferObject) {
			GL20.glShaderSource(glShader,
			"#version 120\r\n"
				+ "#extension GL_ARB_uniform_buffer_object: enable\r\n"
				+ "attribute vec4 a_joint;"
				+ "attribute vec4 a_weight;"
				+ "attribute mat4 a_skinMatrix;"
				+ "uniform int u_startJoint;"
				+ "uniform u_jointMatrixBuffer { mat4[" + maxDrawJointSize + "] jointMatrices; };"
				+ "varying mat4 v_skinMatrix;"
				+ "void main() {"
				+ "mat4 skinMatrix = a_skinMatrix;"
				+ "int nextStartJoint = u_startJoint + " + maxDrawJointSize + ";"
				+ "int joint = int(a_joint.x);"
				+ "if(joint >= u_startJoint && joint < nextStartJoint) skinMatrix += a_weight.x * jointMatrices[joint - u_startJoint];"
				+ "joint = int(a_joint.y);"
				+ "if(joint >= u_startJoint && joint < nextStartJoint) skinMatrix += a_weight.y * jointMatrices[joint - u_startJoint];"
				+ "joint = int(a_joint.z);"
				+ "if(joint >= u_startJoint && joint < nextStartJoint) skinMatrix += a_weight.z * jointMatrices[joint - u_startJoint];"
				+ "joint = int(a_joint.w);"
				+ "if(joint >= u_startJoint && joint < nextStartJoint) skinMatrix += a_weight.w * jointMatrices[joint - u_startJoint];"
				+ "v_skinMatrix = skinMatrix;"
				+ "gl_Position = vec4(0.0);"
				+ "}");
			GL20.glCompileShader(glShader);

			int glProgram = GL20.glCreateProgram();
			GL20.glAttachShader(glProgram, glShader);
			GL20.glDeleteShader(glShader);
			GL30Abstraction.glTransformFeedbackVaryings(glProgram, new CharSequence[]{"v_skinMatrix"}, GL30.GL_INTERLEAVED_ATTRIBS);
			GL20.glLinkProgram(glProgram);
			GL31.glUniformBlockBinding(glProgram, GL31.glGetUniformBlockIndex(glProgram, "u_jointMatrixBuffer"), 0);

			return glProgram;
		}
		else {
			GL20.glShaderSource(glShader,
			"#version 120\r\n"
				+ "#extension GL_EXT_bindable_uniform: enable\r\n"
				+ "attribute vec4 a_joint;"
				+ "attribute vec4 a_weight;"
				+ "attribute mat4 a_skinMatrix;"
				+ "uniform int u_startJoint;"
				+ "bindable uniform mat4 u_jointMatrices[" + maxDrawJointSize + "];"
				+ "varying mat4 v_skinMatrix;"
				+ "void main() {"
				+ "mat4 skinMatrix = a_skinMatrix;"
				+ "int nextStartJoint = u_startJoint + " + maxDrawJointSize + ";"
				+ "int joint = int(a_joint.x);"
				+ "if(joint >= u_startJoint && joint < nextStartJoint) skinMatrix += a_weight.x * u_jointMatrices[joint - u_startJoint];"
				+ "joint = int(a_joint.y);"
				+ "if(joint >= u_startJoint && joint < nextStartJoint) skinMatrix += a_weight.y * u_jointMatrices[joint - u_startJoint];"
				+ "joint = int(a_joint.z);"
				+ "if(joint >= u_startJoint && joint < nextStartJoint) skinMatrix += a_weight.z * u_jointMatrices[joint - u_startJoint];"
				+ "joint = int(a_joint.w);"
				+ "if(joint >= u_startJoint && joint < nextStartJoint) skinMatrix += a_weight.w * u_jointMatrices[joint - u_startJoint];"
				+ "v_skinMatrix = skinMatrix;"
				+ "gl_Position = vec4(0.0);"
				+ "}");
			GL20.glCompileShader(glShader);

			int glProgram = GL20.glCreateProgram();
			GL20.glAttachShader(glProgram, glShader);
			GL20.glDeleteShader(glShader);
			GL30Abstraction.glTransformFeedbackVaryings(glProgram, new CharSequence[]{"v_skinMatrix"}, GL30.GL_INTERLEAVED_ATTRIBS);
			GL20.glLinkProgram(glProgram);

			return glProgram;
		}
	}

	public int getGlProgram() {
		return glProgram;
	}

	public int getMaxDrawJointSize() {
		return maxDrawJointSize;
	}

	public int getJointAttribute() {
		return jointLocation;
	}

	public int getWeightAttribute() {
		return weightLocation;
	}

	public int getSkinMatrixAttribute() {
		return skinMatrixLocation;
	}

	public int getStartJointUniform() {
		return startJointLocation;
	}

	public int getJointMatricesUniformBuffer() {
		return jointMatricesLocation;
	}

}
