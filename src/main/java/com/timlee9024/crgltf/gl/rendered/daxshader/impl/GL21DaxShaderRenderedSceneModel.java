package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.constants.DaxShaderRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedSceneModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class GL21DaxShaderRenderedSceneModel extends GL21RenderedSceneModel {

	public GL21DaxShaderRenderedNodeModel[] daxShaderRenderedNodeModels;

	public void renderNodeModelsForDaxShader() {
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());

		for (GL21DaxShaderRenderedNodeModel renderedNodeModel : daxShaderRenderedNodeModels)
			renderedNodeModel.renderMeshModelsForDaxShader();

		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL20.glDisableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());
		GL20.glDisableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
	}
}
