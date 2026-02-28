package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class DefaultRenderedMaterialModel {

	public static final DefaultRenderedMaterialModel DEFAULT = new DefaultRenderedMaterialModel();

	public DefaultRenderedTextureModel baseColorTexture = DefaultRenderedTextureModel.WHITE_TEXTURE;

	public DefaultRenderedTextureModel emissiveTexture = DefaultRenderedTextureModel.UNBIND_TEXTURE;

	public MaterialModelV2.AlphaMode alphaMode = MaterialModelV2.AlphaMode.OPAQUE;

	public float alphaCutoff;

	public boolean doubleSided;

	public void render(Runnable glDraw) {
		GL13.glActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
		emissiveTexture.bindTexture();
		GL13.glActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
		baseColorTexture.bindTexture();
		if (doubleSided) GL11.glDisable(GL11.GL_CULL_FACE);
		else GL11.glEnable(GL11.GL_CULL_FACE);
		switch (alphaMode) {
			case OPAQUE:
				glDraw.run();
				break;
			case MASK:
				GL11.glEnable(GL11.GL_ALPHA_TEST);
				GL11.glAlphaFunc(GL11.GL_GEQUAL, alphaCutoff);
				glDraw.run();
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				break;
			case BLEND:
				GL11.glEnable(GL11.GL_BLEND);
				glDraw.run();
				GL11.glDisable(GL11.GL_BLEND);
				break;
		}
	}

}
