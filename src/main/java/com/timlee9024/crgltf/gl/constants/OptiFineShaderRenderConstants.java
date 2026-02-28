package com.timlee9024.crgltf.gl.constants;

import org.lwjgl.opengl.GL13;

/**
 * https://github.com/sp614x/optifine/blob/master/OptiFineDoc/doc/shaders.txt
 */
public class OptiFineShaderRenderConstants extends DaxShaderRenderConstants {

	@Override
	public int getMcMidTexCoordAttributeIndex() {
		return 11;
	}

	@Override
	public int getTangentAttributeIndex() {
		return 12;
	}

	@Override
	public int getNormalTextureIndex() {
		return GL13.GL_TEXTURE2;
	}

	@Override
	public int getSpecularTextureIndex() {
		return GL13.GL_TEXTURE3;
	}

}
