package com.timlee9024.crgltf.gl.rendered.impl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryStack;

public class DefaultRenderedTextureModel {
	public static final DefaultRenderedTextureModel UNBIND_TEXTURE = new DefaultRenderedTextureModel();
	public static final DefaultRenderedTextureModel WHITE_TEXTURE = new DefaultRenderedTextureModel();
	public static final DefaultRenderedTextureModel FLAT_NORMAL_TEXTURE = new DefaultRenderedTextureModel();

	public static void initTextureConstants() {
		GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);

		WHITE_TEXTURE.glTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, WHITE_TEXTURE.glTexture);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, stack.bytes(new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}));
		}
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);

		FLAT_NORMAL_TEXTURE.glTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, FLAT_NORMAL_TEXTURE.glTexture);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, stack.bytes(new byte[]{-128, -128, -1, -1, -128, -128, -1, -1, -128, -128, -1, -1, -128, -128, -1, -1}));
		}
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);

		GL11.glPopAttrib();
	}

	public int glTexture;

	public void bindTexture() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture);
	}

}
