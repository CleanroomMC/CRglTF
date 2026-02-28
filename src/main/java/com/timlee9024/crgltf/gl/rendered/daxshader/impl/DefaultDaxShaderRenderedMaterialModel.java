package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.constants.DaxShaderRenderConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMaterialModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedTextureModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class DefaultDaxShaderRenderedMaterialModel extends DefaultRenderedMaterialModel {

	public static final DefaultDaxShaderRenderedMaterialModel DEFAULT = new DefaultDaxShaderRenderedMaterialModel();

	public DefaultRenderedTextureModel baseColorTextureForDaxShader = DefaultRenderedTextureModel.WHITE_TEXTURE;

	public DefaultRenderedTextureModel normalTexture = DefaultRenderedTextureModel.FLAT_NORMAL_TEXTURE;

	public DefaultRenderedTextureModel specularTexture = DefaultRenderedTextureModel.UNBIND_TEXTURE;

	public void renderForDaxShader(Runnable glDaxShaderDraw) {
		GL13.glActiveTexture(DaxShaderRenderConstants.getInstance().getSpecularTextureIndex());
		specularTexture.bindTexture();
		GL13.glActiveTexture(DaxShaderRenderConstants.getInstance().getNormalTextureIndex());
		normalTexture.bindTexture();
		GL13.glActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
		baseColorTextureForDaxShader.bindTexture();
		if (doubleSided) GL11.glDisable(GL11.GL_CULL_FACE);
		else GL11.glEnable(GL11.GL_CULL_FACE);
		switch (alphaMode) {
			case OPAQUE:
				glDaxShaderDraw.run();
				break;
			case MASK:
				GL11.glEnable(GL11.GL_ALPHA_TEST);
				GL11.glAlphaFunc(GL11.GL_GEQUAL, alphaCutoff);
				glDaxShaderDraw.run();
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				break;
			case BLEND:
				GL11.glEnable(GL11.GL_BLEND);
				glDaxShaderDraw.run();
				GL11.glDisable(GL11.GL_BLEND);
				break;
		}
	}
}
