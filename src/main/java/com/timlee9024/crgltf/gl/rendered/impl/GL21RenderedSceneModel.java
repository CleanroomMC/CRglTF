package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

public class GL21RenderedSceneModel {

	public GL21RenderedNodeModel[] renderedNodeModels;

	public void renderNodeModels() {
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		for (GL21RenderedNodeModel renderedNodeModel : renderedNodeModels) renderedNodeModel.renderMeshModels();

		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
	}
}
