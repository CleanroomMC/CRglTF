package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.image.PixelData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.Map;

public class DefaultRenderedTextureModelCreator {

	public OpenGLObjectRefSet glTextures;
	public Map<ImageModel, PixelData> pixelDataLookup;

	public DefaultRenderedTextureModel create(TextureModel textureModel) {
		PixelData pixelData = pixelDataLookup.get(textureModel.getImageModel());
		int glTexture = GL11.glGenTextures();
		glTextures.add(glTexture);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, pixelData.getWidth(), pixelData.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelData.getPixelsRGBA());

		Integer minFilter = textureModel.getMinFilter();
		Integer magFilter = textureModel.getMagFilter();
		Integer wrapS = textureModel.getWrapS();
		Integer wrapT = textureModel.getWrapT();

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter != null ? minFilter : GL11.GL_NEAREST_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter != null ? magFilter : GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS != null ? wrapS : GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapT != null ? wrapT : GL11.GL_REPEAT);

		DefaultRenderedTextureModel renderedTextureModel = new DefaultRenderedTextureModel();
		renderedTextureModel.glTexture = glTexture;
		return renderedTextureModel;
	}

}
