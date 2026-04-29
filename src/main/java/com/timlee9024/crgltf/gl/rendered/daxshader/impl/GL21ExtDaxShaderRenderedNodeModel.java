package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtNodeSkin;
import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedNodeModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class GL21ExtDaxShaderRenderedNodeModel extends GL21ExtRenderedNodeModel {

	public GL21ExtDaxShaderRenderedMeshModel[] daxShaderRenderedMeshModels;

	public void renderMeshModelsForDaxShader() {
		if (nodeSkin == GL21ExtNodeSkin.DUMMY) {
			if (isGlobalTransformZeroMatrix()) return;
			//To match glTF spec requirement for NODE_SKINNED_MESH_LOCAL_TRANSFORMS.
			GL11.glPushMatrix();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				GL11.glMultMatrixf(getGlobalTransformMatrix().get(stack.mallocFloat(16)));
			}
			for (GL21ExtDaxShaderRenderedMeshModel renderedMeshModel : daxShaderRenderedMeshModels) {
				renderedMeshModel.renderMeshPrimitiveModelsForDaxShader();
			}
			GL11.glPopMatrix();
		} else {
			if (nodeSkin.isAllJointZeroMatrix) return;
			for (GL21ExtDaxShaderRenderedMeshModel renderedMeshModel : daxShaderRenderedMeshModels) {
				renderedMeshModel.renderMeshPrimitiveModelsForDaxShader();
			}
		}
	}
}
