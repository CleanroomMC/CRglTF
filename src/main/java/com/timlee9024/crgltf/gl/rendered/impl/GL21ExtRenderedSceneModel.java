package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import com.timlee9024.crgltf.gl.GL31Abstraction;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfCalcJointMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfMorphingPassConstants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class GL21ExtRenderedSceneModel {

	public static int currentGlProgram;

	public GL21ExtRenderedNodeModel[] renderedNodeModels;

	public GL21ExtNodeSkin[] nodeSkins;

	public boolean hasMorphing;

	public boolean hasInverseBindMatrices;

	public void renderNodeModels() {
		if (hasMorphing) {
			GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);

			if (nodeSkins != null) {
				if (hasInverseBindMatrices) {
					GL20.glUseProgram(GL21ExtGltfCalcJointMatrixPassConstants.getInstance().getGlProgram());
				}
				for (GL21ExtNodeSkin nodeSkin : nodeSkins)
					nodeSkin.runCalcJointMatrixPass();

				GL20.glUseProgram(GL21ExtGltfMorphingPassConstants.getInstance().getGlProgram());
				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.morphing.runMorphingPass();

				GL20.glUseProgram(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getGlProgram());
				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.nodeSkin.runCalcSkinMatrixPass(renderedNodeModel.renderedMeshModels);
				GL31Abstraction.glBindUniformBuffer(0);

				GL20.glUseProgram(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getGlProgram());
				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.nodeSkin.runApplySkinMatrixPass(renderedNodeModel.renderedMeshModels);

				GL20.glUseProgram(currentGlProgram);
				GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.renderMeshModels();

				for (GL21ExtNodeSkin nodeSkin : nodeSkins)
					nodeSkin.isAllJointZeroMatrix = true;
			} else {
				GL20.glUseProgram(GL21ExtGltfMorphingPassConstants.getInstance().getGlProgram());
				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.morphing.runMorphingPass();

				GL20.glUseProgram(currentGlProgram);
				GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.renderMeshModels();
			}
		} else {
			if (nodeSkins != null) {
				GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);

				if (hasInverseBindMatrices) {
					GL20.glUseProgram(GL21ExtGltfCalcJointMatrixPassConstants.getInstance().getGlProgram());
				}
				for (GL21ExtNodeSkin nodeSkin : nodeSkins)
					nodeSkin.runCalcJointMatrixPass();

				GL20.glUseProgram(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getGlProgram());
				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.nodeSkin.runCalcSkinMatrixPass(renderedNodeModel.renderedMeshModels);
				GL31Abstraction.glBindUniformBuffer(0);

				GL20.glUseProgram(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getGlProgram());
				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.nodeSkin.runApplySkinMatrixPass(renderedNodeModel.renderedMeshModels);

				GL20.glUseProgram(currentGlProgram);
				GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.renderMeshModels();

				for (GL21ExtNodeSkin nodeSkin : nodeSkins)
					nodeSkin.isAllJointZeroMatrix = true;
			} else {
				for (GL21ExtRenderedNodeModel renderedNodeModel : renderedNodeModels)
					renderedNodeModel.renderMeshModels();
			}
		}

		GL30Abstraction.glBindVertexArray(0);
	}

}
