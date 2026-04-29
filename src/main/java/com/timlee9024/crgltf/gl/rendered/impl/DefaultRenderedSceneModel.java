package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcJointMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

public class DefaultRenderedSceneModel {

	public static int currentGlProgram;

	public DefaultRenderedNodeModel[] renderedNodeModels;

	public DefaultNodeSkin[] nodeSkins;

	public boolean hasMorphing;

	public boolean hasInverseBindMatrices;

	public void renderNodeModels() {
		if (hasMorphing) {
			GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);

			if (nodeSkins != null) {
				if (hasInverseBindMatrices) {
					GL20.glUseProgram(GltfCalcJointMatrixPassConstants.getInstance().getGlProgram());
				}
				for (DefaultNodeSkin nodeSkin : nodeSkins)
					nodeSkin.runCalcJointMatrixPass();

				GL20.glUseProgram(GltfMorphingPassConstants.getInstance().getGlProgram());
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.morphing.runMorphingPass();

				GL20.glUseProgram(GltfCalcSkinMatrixPassConstants.getInstance().getGlProgram());
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.nodeSkin.runCalcSkinMatrixPass(renderedNodeModel.renderedMeshModels);

				GL20.glUseProgram(GltfApplySkinMatrixPassConstants.getInstance().getGlProgram());
				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.nodeSkin.runApplySkinMatrixPass(renderedNodeModel.renderedMeshModels);

				GL20.glUseProgram(currentGlProgram);
				GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.renderMeshModels();
				GL42.glMemoryBarrier(0);

				for (DefaultNodeSkin nodeSkin : nodeSkins)
					nodeSkin.isAllJointZeroMatrix = true;
			} else {
				GL20.glUseProgram(GltfMorphingPassConstants.getInstance().getGlProgram());
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.morphing.runMorphingPass();

				GL20.glUseProgram(currentGlProgram);
				GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.renderMeshModels();
				GL42.glMemoryBarrier(0);
			}
			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
		} else {
			if (nodeSkins != null) {
				GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);

				if (hasInverseBindMatrices) {
					GL20.glUseProgram(GltfCalcJointMatrixPassConstants.getInstance().getGlProgram());
				}
				for (DefaultNodeSkin nodeSkin : nodeSkins)
					nodeSkin.runCalcJointMatrixPass();

				GL20.glUseProgram(GltfCalcSkinMatrixPassConstants.getInstance().getGlProgram());
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.nodeSkin.runCalcSkinMatrixPass(renderedNodeModel.renderedMeshModels);

				GL20.glUseProgram(GltfApplySkinMatrixPassConstants.getInstance().getGlProgram());
				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.nodeSkin.runApplySkinMatrixPass(renderedNodeModel.renderedMeshModels);

				GL20.glUseProgram(currentGlProgram);
				GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.renderMeshModels();
				GL42.glMemoryBarrier(0);

				for (DefaultNodeSkin nodeSkin : nodeSkins)
					nodeSkin.isAllJointZeroMatrix = true;

				GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
			} else {
				for (DefaultRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.renderMeshModels();
			}
		}

		GL30.glBindVertexArray(0);
	}

}
