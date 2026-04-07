package com.timlee9024.crgltf.gl.constants;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class GL21ExtGltfCalcJointMatrixPassConstants {

	private static GL21ExtGltfCalcJointMatrixPassConstants instance;

	protected final int glProgram;

	protected final int jointMatrixLocation;
	protected final int inverseBindMatrixLocation;

	public static GL21ExtGltfCalcJointMatrixPassConstants getInstance() {
		return instance;
	}

	public GL21ExtGltfCalcJointMatrixPassConstants() {
		glProgram = createGlProgram();

		jointMatrixLocation = GL20.glGetAttribLocation(glProgram, "a_jointMatrix");
		inverseBindMatrixLocation = GL20.glGetAttribLocation(glProgram, "a_inverseBindMatrix");
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected int createGlProgram() {
		int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		GL20.glShaderSource(glShader,
		"attribute mat4 a_jointMatrix;"
			+ "attribute mat4 a_inverseBindMatrix;"
			+ "varying mat4 v_jointMatrix;"
			+ "void main() {"
			+ "v_jointMatrix = a_jointMatrix * a_inverseBindMatrix;"
			+ "gl_Position = vec4(0.0);"
			+ "}");
		GL20.glCompileShader(glShader);

		int glProgram = GL20.glCreateProgram();
		GL20.glAttachShader(glProgram, glShader);
		GL20.glDeleteShader(glShader);
		GL30Abstraction.glTransformFeedbackVaryings(glProgram, new CharSequence[]{"v_jointMatrix"}, GL30.GL_INTERLEAVED_ATTRIBS);
		GL20.glLinkProgram(glProgram);

		return glProgram;
	}

	public int getGlProgram() {
		return glProgram;
	}

	public int getJointMatrixAttribute() {
		return jointMatrixLocation;
	}

	public int getInverseBindMatrixAttribute() {
		return inverseBindMatrixLocation;
	}

}
