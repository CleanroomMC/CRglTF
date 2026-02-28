package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.image.PixelData;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;
import java.util.Map;

public class CombinedRenderedTextureModelCreator extends DefaultRenderedTextureModelCreator {
	public Map<TextureModel, Integer> glTextureLookup;

	public GltfMaterialToTextureConstants.Program program;
	public MaterialModelV2 materialModelV2;

	protected TextureModel sourceTextureModel;
	protected DefaultRenderedTextureModel fallbackRenderedTextureModel;
	protected int hasTextureUniformLocation;
	protected int width;
	protected int height;
	protected int magFilter;

	@Override
	public DefaultRenderedTextureModel create(TextureModel textureModel) {
		width = 2;
		height = 2;
		sourceTextureModel = materialModelV2.getBaseColorTexture();
		setWidthHeightFromSourceTexture();
		sourceTextureModel = materialModelV2.getMetallicRoughnessTexture();
		setWidthHeightFromSourceTexture();
		sourceTextureModel = materialModelV2.getNormalTexture();
		setWidthHeightFromSourceTexture();
		sourceTextureModel = materialModelV2.getOcclusionTexture();
		setWidthHeightFromSourceTexture();
		sourceTextureModel = materialModelV2.getEmissiveTexture();
		setWidthHeightFromSourceTexture();

		int glTexture = GL11.glGenTextures();
		glTextures.add(glTexture);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

		Integer filter = textureModel.getMagFilter();
		magFilter = filter != null ? filter : GL11.GL_LINEAR;
		Integer minFilter = textureModel.getMinFilter();
		Integer wrapS = textureModel.getWrapS();
		Integer wrapT = textureModel.getWrapT();

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter != null ? minFilter : GL11.GL_NEAREST_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS != null ? wrapS : GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapT != null ? wrapT : GL11.GL_REPEAT);

		OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, glTexture, 0);
		GL11.glViewport(0, 0, width, height);
		GL20.glUseProgram(program.getGlProgram());

		hasTextureUniformLocation = program.getHasEmissiveTextureLocation();
		fallbackRenderedTextureModel = DefaultRenderedTextureModel.WHITE_TEXTURE;
		GL13.glActiveTexture(program.getEmissiveTextureIndex());
		bindSourceTexture();

		sourceTextureModel = materialModelV2.getOcclusionTexture();
		hasTextureUniformLocation = program.getHasOcclusionTextureLocation();
		fallbackRenderedTextureModel = DefaultRenderedTextureModel.WHITE_TEXTURE;
		GL13.glActiveTexture(program.getOcclusionTextureIndex());
		bindSourceTexture();

		sourceTextureModel = materialModelV2.getNormalTexture();
		hasTextureUniformLocation = program.getHasNormalTextureLocation();
		fallbackRenderedTextureModel = DefaultRenderedTextureModel.FLAT_NORMAL_TEXTURE;
		GL13.glActiveTexture(program.getNormalTextureIndex());
		bindSourceTexture();

		sourceTextureModel = materialModelV2.getMetallicRoughnessTexture();
		hasTextureUniformLocation = program.getHasMetallicRoughnessTextureLocation();
		fallbackRenderedTextureModel = DefaultRenderedTextureModel.WHITE_TEXTURE;
		GL13.glActiveTexture(program.getMetallicRoughnessTextureIndex());
		bindSourceTexture();

		sourceTextureModel = materialModelV2.getBaseColorTexture();
		hasTextureUniformLocation = program.getHasBaseColorTextureLocation();
		fallbackRenderedTextureModel = DefaultRenderedTextureModel.WHITE_TEXTURE;
		GL13.glActiveTexture(program.getBaseColorTextureIndex());
		bindSourceTexture();

		if (program.getWidthLocation() != -1) GL20.glUniform1i(program.getWidthLocation(), width);
		if (program.getHeightLocation() != -1) GL20.glUniform1i(program.getHeightLocation(), height);

		if (program.getBaseColorFactorLocation() != -1) {
			float[] baseColorFactor = materialModelV2.getBaseColorFactor();
			if (baseColorFactor != null) GL20.glUniform4fv(program.getBaseColorFactorLocation(), baseColorFactor);
			else GL20.glUniform4f(program.getBaseColorFactorLocation(), 1, 1, 1, 1);
		}

		if (program.getMetallicFactorLocation() != -1) {
			GL20.glUniform1f(program.getMetallicFactorLocation(), materialModelV2.getMetallicFactor());
		}

		if (program.getRoughnessFactorLocation() != -1) {
			GL20.glUniform1f(program.getRoughnessFactorLocation(), materialModelV2.getRoughnessFactor());
		}

		if (program.getNormalScaleLocation() != -1) {
			GL20.glUniform1f(program.getNormalScaleLocation(), materialModelV2.getNormalScale());
		}

		if (program.getOcclusionStrengthLocation() != -1) {
			GL20.glUniform1f(program.getOcclusionStrengthLocation(), materialModelV2.getOcclusionStrength());
		}

		if (program.getEmissiveFactorLocation() != -1) {
			float[] emissiveFactor = materialModelV2.getEmissiveFactor();
			if (emissiveFactor != null) GL20.glUniform3fv(program.getEmissiveFactorLocation(), emissiveFactor);
			else GL20.glUniform3f(program.getEmissiveFactorLocation(), 0, 0, 0);
		}

		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 12);

		DefaultRenderedTextureModel renderedTextureModel = new DefaultRenderedTextureModel();
		renderedTextureModel.glTexture = glTexture;
		return renderedTextureModel;
	}

	protected void setWidthHeightFromSourceTexture() {
		if (sourceTextureModel != null) {
			PixelData pixelData = pixelDataLookup.get(sourceTextureModel.getImageModel());
			width = Math.max(width, pixelData.getWidth());
			height = Math.max(height, pixelData.getHeight());
		}
	}

	protected void bindSourceTexture() {
		if (sourceTextureModel != null) {
			if (hasTextureUniformLocation != -1) GL20.glUniform1i(hasTextureUniformLocation, 1);
			PixelData pixelData = pixelDataLookup.get(sourceTextureModel.getImageModel());
			Integer glTexture = glTextureLookup.get(sourceTextureModel);
			if (glTexture == null) {
				glTexture = GL11.glGenTextures();
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture);
				GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, pixelData.getWidth(), pixelData.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelData.getPixelsRGBA());
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
				glTextureLookup.put(sourceTextureModel, glTexture);
			} else GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter);
		} else {
			if (hasTextureUniformLocation != -1) GL20.glUniform1i(hasTextureUniformLocation, 0);
			fallbackRenderedTextureModel.bindTexture();
		}
	}

	public void deleteGlTextureLookup() {
		glTextureLookup.forEach(((textureModel, glTexture) -> GL11.glDeleteTextures(glTexture)));
	}
}
