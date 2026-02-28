package com.timlee9024.crgltf.gl.constants;

import com.timlee9024.crgltf.CRglTFMod;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class GltfMaterialToTextureConstants {

	private static GltfMaterialToTextureConstants instance;

	protected final int glVertShader;
	protected final int glCanvasFramebuffer;
	protected final int glCanvasQuadBuffer;

	public static GltfMaterialToTextureConstants getInstance() {
		return instance;
	}

	public GltfMaterialToTextureConstants() {
		glVertShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		GL20.glShaderSource(glVertShader,
				"void main() {" +
						"gl_Position = gl_Vertex;" +
						"}");
		GL20.glCompileShader(glVertShader);

		if (OpenGlHelper.framebufferSupported) glCanvasFramebuffer = OpenGlHelper.glGenFramebuffers();
		else glCanvasFramebuffer = 0;

		glCanvasQuadBuffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glCanvasQuadBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, new float[]{
				-1.0f, -1.0f, 0.0f,
				1.0f, -1.0f, 0.0f,
				-1.0f, 1.0f, 0.0f,
				-1.0f, 1.0f, 0.0f,
				1.0f, -1.0f, 0.0f,
				1.0f, 1.0f, 0.0f,
		}, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	public int getGlCanvasFramebuffer() {
		return glCanvasFramebuffer;
	}

	public void setupCanvasQuad() {
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glCanvasQuadBuffer);
		GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	}

	public class Program {
		protected final int glProgram;

		protected final int u_width;
		protected final int u_height;

		protected final int u_hasBaseColorTexture;
		protected final int u_hasMetallicRoughnessTexture;
		protected final int u_hasNormalTexture;
		protected final int u_hasOcclusionTexture;
		protected final int u_hasEmissiveTexture;

		protected final int u_baseColorFactor;
		protected final int u_metallicFactor;
		protected final int u_roughnessFactor;
		protected final int u_normalScale;
		protected final int u_occlusionStrength;
		protected final int u_emissiveFactor;

		public Program(String fragShaderSource) {
			int glFragShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
			GL20.glShaderSource(glFragShader, fragShaderSource);
			GL20.glCompileShader(glFragShader);
			String log = GL20.glGetShaderInfoLog(glFragShader);
			if (!log.isEmpty()) CRglTFMod.logger.error(log);

			glProgram = GL20.glCreateProgram();

			GL20.glAttachShader(glProgram, glVertShader);
			GL20.glAttachShader(glProgram, glFragShader);

			GL20.glLinkProgram(glProgram);
			GL20.glDeleteShader(glFragShader);

			u_width = GL20.glGetUniformLocation(glProgram, "u_width");
			u_height = GL20.glGetUniformLocation(glProgram, "u_height");

			u_hasBaseColorTexture = GL20.glGetUniformLocation(glProgram, "u_hasBaseColorTexture");
			u_hasMetallicRoughnessTexture = GL20.glGetUniformLocation(glProgram, "u_hasMetallicRoughnessTexture");
			u_hasNormalTexture = GL20.glGetUniformLocation(glProgram, "u_hasNormalTexture");
			u_hasOcclusionTexture = GL20.glGetUniformLocation(glProgram, "u_hasOcclusionTexture");
			u_hasEmissiveTexture = GL20.glGetUniformLocation(glProgram, "u_hasEmissiveTexture");

			u_baseColorFactor = GL20.glGetUniformLocation(glProgram, "u_baseColorFactor");
			u_metallicFactor = GL20.glGetUniformLocation(glProgram, "u_metallicFactor");
			u_roughnessFactor = GL20.glGetUniformLocation(glProgram, "u_roughnessFactor");
			u_normalScale = GL20.glGetUniformLocation(glProgram, "u_normalScale");
			u_occlusionStrength = GL20.glGetUniformLocation(glProgram, "u_occlusionStrength");
			u_emissiveFactor = GL20.glGetUniformLocation(glProgram, "u_emissiveFactor");

			int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
			GL20.glUseProgram(glProgram);
			int location;
			location = GL20.glGetUniformLocation(glProgram, "u_baseColorTexture");
			if (location != -1) GL20.glUniform1i(location, 0);
			location = GL20.glGetUniformLocation(glProgram, "u_metallicRoughnessTexture");
			if (location != -1) GL20.glUniform1i(location, 1);
			location = GL20.glGetUniformLocation(glProgram, "u_normalTexture");
			if (location != -1) GL20.glUniform1i(location, 2);
			location = GL20.glGetUniformLocation(glProgram, "u_occlusionTexture");
			if (location != -1) GL20.glUniform1i(location, 3);
			location = GL20.glGetUniformLocation(glProgram, "u_emissiveTexture");
			if (location != -1) GL20.glUniform1i(location, 4);
			GL20.glUseProgram(currentProgram);
		}

		public int getGlProgram() {
			return glProgram;
		}

		public int getWidthLocation() {
			return u_width;
		}

		public int getHeightLocation() {
			return u_height;
		}

		public int getHasBaseColorTextureLocation() {
			return u_hasBaseColorTexture;
		}

		public int getHasMetallicRoughnessTextureLocation() {
			return u_hasMetallicRoughnessTexture;
		}

		public int getHasNormalTextureLocation() {
			return u_hasNormalTexture;
		}

		public int getHasOcclusionTextureLocation() {
			return u_hasOcclusionTexture;
		}

		public int getHasEmissiveTextureLocation() {
			return u_hasEmissiveTexture;
		}

		public int getBaseColorFactorLocation() {
			return u_baseColorFactor;
		}

		public int getMetallicFactorLocation() {
			return u_metallicFactor;
		}

		public int getRoughnessFactorLocation() {
			return u_roughnessFactor;
		}

		public int getNormalScaleLocation() {
			return u_normalScale;
		}

		public int getOcclusionStrengthLocation() {
			return u_occlusionStrength;
		}

		public int getEmissiveFactorLocation() {
			return u_emissiveFactor;
		}

		public int getBaseColorTextureIndex() {
			return GL13.GL_TEXTURE0;
		}

		public int getMetallicRoughnessTextureIndex() {
			return GL13.GL_TEXTURE1;
		}

		public int getNormalTextureIndex() {
			return GL13.GL_TEXTURE2;
		}

		public int getOcclusionTextureIndex() {
			return GL13.GL_TEXTURE3;
		}

		public int getEmissiveTextureIndex() {
			return GL13.GL_TEXTURE4;
		}
	}
}
