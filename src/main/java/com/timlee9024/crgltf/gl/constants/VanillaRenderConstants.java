package com.timlee9024.crgltf.gl.constants;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class VanillaRenderConstants {

	private static VanillaRenderConstants instance;

	protected final GltfMaterialToTextureConstants.Program colorTextureProgram;
	protected final GltfMaterialToTextureConstants.Program emissiveTextureProgram;

	public static VanillaRenderConstants getInstance() {
		return instance;
	}

	public VanillaRenderConstants() {
		colorTextureProgram = createColorTextureProgram();
		emissiveTextureProgram = createEmissiveTextureProgram();
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	protected GltfMaterialToTextureConstants.Program createColorTextureProgram() {
		return GltfMaterialToTextureConstants.getInstance().new Program(
				"uniform int u_width;" +
						"uniform int u_height;" +
						"uniform sampler2D u_baseColorTexture;" +
						"uniform sampler2D u_occlusionTexture;" +
						"uniform vec4 u_baseColorFactor;" +
						"uniform float u_occlusionStrength;" +
						"void main() {" +
						"vec4 baseColor = texture2D(u_baseColorTexture, vec2(gl_FragCoord.x / float(u_width), gl_FragCoord.y / float(u_height))) * u_baseColorFactor;" +
						"float occlusion = (texture2D(u_occlusionTexture, vec2(gl_FragCoord.x / float(u_width), gl_FragCoord.y / float(u_height)))).x * u_occlusionStrength;" +
						"gl_FragColor = baseColor * occlusion;" +
						"}");
	}

	protected GltfMaterialToTextureConstants.Program createEmissiveTextureProgram() {
		return GltfMaterialToTextureConstants.getInstance().new Program(
				"uniform int u_width;" +
						"uniform int u_height;" +
						"uniform sampler2D u_emissiveTexture;" +
						"uniform vec3 u_emissiveFactor;" +
						"void main() {" +
						"gl_FragColor = texture2D(u_emissiveTexture, vec2(gl_FragCoord.x / float(u_width), gl_FragCoord.y / float(u_height))) * vec4(u_emissiveFactor, 1.0);" +
						"}");
	}

	public GltfMaterialToTextureConstants.Program getMaterialToColorTextureProgram() {
		return colorTextureProgram;
	}

	public GltfMaterialToTextureConstants.Program getMaterialToEmissiveTextureProgram() {
		return emissiveTextureProgram;
	}

	public int getColorTextureIndex() {
		return OpenGlHelper.defaultTexUnit;
	}

	public int getEmissiveTextureIndex() {
		return GL13.GL_TEXTURE3;
	}

	public void setupEmissiveTexEnv() {
		GL13.glActiveTexture(getEmissiveTextureIndex());
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_ADD);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_PREVIOUS);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_RGB, GL11.GL_TEXTURE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_RGB, GL11.GL_SRC_COLOR);
	}

}
