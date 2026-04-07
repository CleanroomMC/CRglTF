package com.timlee9024.crgltf.gl;

import org.lwjgl.opengl.EXTTransformFeedback;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class GL30Abstraction {

	public static boolean openGL30VertexArrayObject;
	public static boolean openGL30TransformFeedback;

	public static void glBindVertexArray(int array) {
		if (openGL30VertexArrayObject) {
			GL30.glBindVertexArray(array);
		} else {
			//https://github.com/LWJGL/lwjgl3/issues/1105
			//APPLEVertexArrayObject.glBindVertexArrayAPPLE(array);
		}
	}

	public static void glDeleteVertexArrays(int array) {
		if (openGL30VertexArrayObject) {
			GL30.glDeleteVertexArrays(array);
		} else {
			//APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(array);
		}
	}

	public static void glDeleteVertexArrays(IntBuffer arrays) {
		if (openGL30VertexArrayObject) {
			GL30.glDeleteVertexArrays(arrays);
		} else {
			//APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(arrays);
		}
	}

	public static int glGenVertexArrays() {
		if (openGL30VertexArrayObject) {
			return GL30.glGenVertexArrays();
		} else {
			//return APPLEVertexArrayObject.glGenVertexArraysAPPLE();
			return 0;
		}
	}

	public static void glGenVertexArrays(IntBuffer arrays) {
		if (openGL30VertexArrayObject) {
			GL30.glGenVertexArrays(arrays);
		} else {
			//APPLEVertexArrayObject.glGenVertexArraysAPPLE(arrays);
		}
	}

	public static boolean glIsVertexArray(int array) {
		if (openGL30VertexArrayObject) {
			return GL30.glIsVertexArray(array);
		} else {
			//return APPLEVertexArrayObject.glIsVertexArrayAPPLE(array);
			return false;
		}
	}

	public static void glBeginTransformFeedback(int primitiveMode) {
		if (openGL30TransformFeedback) {
			GL30.glBeginTransformFeedback(primitiveMode);
		} else {
			EXTTransformFeedback.glBeginTransformFeedbackEXT(primitiveMode);
		}
	}

	public static void glBindBufferBase(int target, int index, int buffer) {
		if (openGL30TransformFeedback) {
			GL30.glBindBufferBase(target, index, buffer);
		} else {
			EXTTransformFeedback.glBindBufferBaseEXT(target, index, buffer);
		}
	}

	public static void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
		if (openGL30TransformFeedback) {
			GL30.glBindBufferRange(target, index, buffer, offset, size);
		} else {
			EXTTransformFeedback.glBindBufferRangeEXT(target, index, buffer, offset, size);
		}
	}

	public static void glEndTransformFeedback() {
		if (openGL30TransformFeedback) {
			GL30.glEndTransformFeedback();
		} else {
			EXTTransformFeedback.glEndTransformFeedbackEXT();
		}
	}

	public static String glGetTransformFeedbackVarying(int program, int index, int bufSize, IntBuffer size, IntBuffer type) {
		if (openGL30TransformFeedback) {
			return GL30.glGetTransformFeedbackVarying(program, index, bufSize, size, type);
		} else {
			return EXTTransformFeedback.glGetTransformFeedbackVaryingEXT(program, index, bufSize, size, type);
		}
	}

	public static void glGetTransformFeedbackVarying(int program, int index, IntBuffer length, IntBuffer size, IntBuffer type, ByteBuffer name) {
		if (openGL30TransformFeedback) {
			GL30.glGetTransformFeedbackVarying(program, index, length, size, type, name);
		} else {
			EXTTransformFeedback.glGetTransformFeedbackVaryingEXT(program, index, length, size, type, name);
		}
	}

	public static void glTransformFeedbackVaryings(int program, CharSequence[] varyings, int bufferMode) {
		if (openGL30TransformFeedback) {
			GL30.glTransformFeedbackVaryings(program, varyings, bufferMode);
		} else {
			EXTTransformFeedback.glTransformFeedbackVaryingsEXT(program, varyings, bufferMode);
		}
	}
}
