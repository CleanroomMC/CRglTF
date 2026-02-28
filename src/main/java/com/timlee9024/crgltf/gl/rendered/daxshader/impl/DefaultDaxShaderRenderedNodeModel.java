package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedNodeModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class DefaultDaxShaderRenderedNodeModel extends DefaultRenderedNodeModel {

	public DefaultDaxShaderRenderedMeshModel[] daxShaderRenderedMeshModels;

	public void renderMeshModelsForDaxShader() {
		if (skinning == DefaultRenderedNodeModel.Skinning.DUMMY) {
			if (isGlobalTransformZeroMatrix()) return;
			//To match glTF spec requirement for NODE_SKINNED_MESH_LOCAL_TRANSFORMS.
			GL11.glPushMatrix();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				GL11.glMultMatrixf(getGlobalTransformMatrix().get(stack.mallocFloat(16)));
			}
			for (DefaultDaxShaderRenderedMeshModel renderedMeshModel : daxShaderRenderedMeshModels) {
				renderedMeshModel.renderMeshPrimitiveModelsForDaxShader();
			}
			GL11.glPopMatrix();
		} else {
			skinning.isAllJointZeroMatrixChecked = false;
			if (skinning.isAllJointZeroMatrix) return;
			for (DefaultDaxShaderRenderedMeshModel renderedMeshModel : daxShaderRenderedMeshModels) {
				renderedMeshModel.renderMeshPrimitiveModelsForDaxShader();
			}
		}
	}
}
