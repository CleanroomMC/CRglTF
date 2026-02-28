package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.constants.GltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcJointMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedNodeModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedSceneModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

public class DefaultDaxShaderRenderedSceneModel extends DefaultRenderedSceneModel {

	public DefaultDaxShaderRenderedNodeModel[] daxShaderRenderedNodeModels;

	public void renderNodeModelsForDaxShader() {
		if (hasMorphing) {
			GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);

			GL20.glUseProgram(GltfMorphingPassConstants.getInstance().getGlProgram());
			for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
				renderedNodeModel.morphing.runMorphingPass();

			if (hasSkinning) {
				if (hasInverseBindMatrices) {
					GL20.glUseProgram(GltfCalcJointMatrixPassConstants.getInstance().getGlProgram());
					for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
						renderedNodeModel.skinning.runCalcJointMatrixPass();
				}

				GL20.glUseProgram(GltfCalcSkinMatrixPassConstants.getInstance().getGlProgram());
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.skinning.runCalcSkinMatrixPass();

				GL20.glUseProgram(GltfApplySkinMatrixPassConstants.getInstance().getGlProgram());
				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.skinning.runApplySkinMatrixPass();
			}

			GL20.glUseProgram(currentGlProgram);
			GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

			GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
			for (DefaultDaxShaderRenderedNodeModel renderedNodeModel : daxShaderRenderedNodeModels)
				renderedNodeModel.renderMeshModelsForDaxShader();
			GL42.glMemoryBarrier(0);

			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
		} else {
			if (hasSkinning) {
				GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);

				if (hasInverseBindMatrices) {
					GL20.glUseProgram(GltfCalcJointMatrixPassConstants.getInstance().getGlProgram());
					for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
						renderedNodeModel.skinning.runCalcJointMatrixPass();
				}

				GL20.glUseProgram(GltfCalcSkinMatrixPassConstants.getInstance().getGlProgram());
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.skinning.runCalcSkinMatrixPass();

				GL20.glUseProgram(GltfApplySkinMatrixPassConstants.getInstance().getGlProgram());
				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.skinning.runApplySkinMatrixPass();

				GL20.glUseProgram(currentGlProgram);
				GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				for (DefaultDaxShaderRenderedNodeModel renderedNodeModel : daxShaderRenderedNodeModels)
					renderedNodeModel.renderMeshModelsForDaxShader();
				GL42.glMemoryBarrier(0);

				GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
			} else {
				for (DefaultDaxShaderRenderedNodeModel renderedNodeModel : daxShaderRenderedNodeModels)
					renderedNodeModel.renderMeshModelsForDaxShader();
			}
		}

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
	}

}
