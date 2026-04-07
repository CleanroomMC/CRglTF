package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class GL21ExtRenderedMeshPrimitiveModel {

	public static final GL21ExtRenderedMeshPrimitiveModel DUMMY = new GL21ExtRenderedMeshPrimitiveModel() {

		@Override
		public void render() {
		}

	};

	static {
		DUMMY.morphing = Morphing.DUMMY;
		DUMMY.skinning = Skinning.DUMMY;
	}

	public DefaultRenderedMaterialModel renderedMaterialModel;
	public Runnable glDraw;
	public Morphing morphing;
	public Skinning skinning;
	public int count;
	public int glRenderVAO;

	public void render() {
		renderedMaterialModel.render(glDraw);
	}

	public class Morphing {
		public static final Morphing DUMMY = GL21ExtRenderedMeshPrimitiveModel.DUMMY.new Morphing() {

			@Override
			public void restoreAttributesForMorphing() {
			}

			@Override
			public void applyMorphTarget(int target) {
			}

		};

		public class AttributeBundle {
			public int glMorphBuffer0;
			public int glMorphBuffer1;
			public int glBaseVAO;
			public int[][] glMorphBaseAndTargetVAOs;

			public void restoreAttributesForMorphing() {
				GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, zeroOrOne ? glMorphBuffer1 : glMorphBuffer0);
				GL30Abstraction.glBindVertexArray(glBaseVAO);
				GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
				GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
				GL30Abstraction.glEndTransformFeedback();
			}

			public void applyMorphTarget(int target) {
				if (zeroOrOne) {
					GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glMorphBuffer0);
					GL30Abstraction.glBindVertexArray(glMorphBaseAndTargetVAOs[target][1]);
				}
				else {
					GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glMorphBuffer1);
					GL30Abstraction.glBindVertexArray(glMorphBaseAndTargetVAOs[target][0]);
				}
				GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
				GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
				GL30Abstraction.glEndTransformFeedback();
			}
		}

		public AttributeBundle[] attributeBundles;
		public boolean zeroOrOne;

		public void restoreAttributesForMorphing() {
			for (AttributeBundle bundle : attributeBundles) bundle.restoreAttributesForMorphing();
		}

		public void applyMorphTarget(int target) {
			for (AttributeBundle bundle : attributeBundles) bundle.applyMorphTarget(target);
			zeroOrOne = !zeroOrOne;
		}

	}

	public class Skinning {
		public static final Skinning DUMMY = GL21ExtRenderedMeshPrimitiveModel.DUMMY.new Skinning() {

			@Override
			public void calculateSkinMatrix(int batch) {
			}

			@Override
			public void applySkinMatrix() {
			}

		};

		public boolean isSkinMatrix1First;
		public int glSkinBuffer0;
		public int glSkinBuffer1;
		public int glBaseAndSkinMatrixVAO;
		public int[][] glTargetAndSkinMatrixVAOs;

		public void calculateSkinMatrix(int batch) {
			if(batch == 0) {
				if (isSkinMatrix1First) {
					for (int i = 0; i < glTargetAndSkinMatrixVAOs.length; i++) {
						GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, i % 2 == 0 ? glSkinBuffer0 : glSkinBuffer1);
						GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[i][0]);
						GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
						GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
						GL30Abstraction.glEndTransformFeedback();
					}
				}
				else {
					for (int i = 0; i < glTargetAndSkinMatrixVAOs.length; i++) {
						GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, i % 2 == 0 ? glSkinBuffer1 : glSkinBuffer0);
						GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[i][0]);
						GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
						GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
						GL30Abstraction.glEndTransformFeedback();
					}
				}
			}
			else {
				if (glTargetAndSkinMatrixVAOs.length % 2 == 0) {
					GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glSkinBuffer1);
					GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[0][1]);
					GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
					GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
					GL30Abstraction.glEndTransformFeedback();
					for (int i = 1; i < glTargetAndSkinMatrixVAOs.length; i++) {
						GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, i % 2 == 0 ? glSkinBuffer1 : glSkinBuffer0);
						GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[i][0]);
						GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
						GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
						GL30Abstraction.glEndTransformFeedback();
					}
				}
				else {
					if(batch % 2 == 0) {
						if (isSkinMatrix1First) {
							GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glSkinBuffer0);
							GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[0][1]);
							GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
							GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
							GL30Abstraction.glEndTransformFeedback();
							for (int i = 1; i < glTargetAndSkinMatrixVAOs.length; i++) {
								GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, i % 2 == 0 ? glSkinBuffer0 : glSkinBuffer1);
								GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[i][0]);
								GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
								GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
								GL30Abstraction.glEndTransformFeedback();
							}
						}
						else {
							GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glSkinBuffer1);
							GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[0][1]);
							GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
							GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
							GL30Abstraction.glEndTransformFeedback();
							for (int i = 1; i < glTargetAndSkinMatrixVAOs.length; i++) {
								GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, i % 2 == 0 ? glSkinBuffer1 : glSkinBuffer0);
								GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[i][0]);
								GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
								GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
								GL30Abstraction.glEndTransformFeedback();
							}
						}
					}
					else {
						if (isSkinMatrix1First) {
							GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glSkinBuffer1);
							GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[0][2]);
							GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
							GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
							GL30Abstraction.glEndTransformFeedback();
							for (int i = 1; i < glTargetAndSkinMatrixVAOs.length; i++) {
								GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, i % 2 == 0 ? glSkinBuffer1 : glSkinBuffer0);
								GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[i][1]);
								GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
								GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
								GL30Abstraction.glEndTransformFeedback();
							}
						}
						else {
							GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glSkinBuffer0);
							GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[0][2]);
							GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
							GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
							GL30Abstraction.glEndTransformFeedback();
							for (int i = 1; i < glTargetAndSkinMatrixVAOs.length; i++) {
								GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, i % 2 == 0 ? glSkinBuffer0 : glSkinBuffer1);
								GL30Abstraction.glBindVertexArray(glTargetAndSkinMatrixVAOs[i][1]);
								GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
								GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
								GL30Abstraction.glEndTransformFeedback();
							}
						}
					}
				}
			}
		}

		public void applySkinMatrix() {
			GL30Abstraction.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, glSkinBuffer1);
			GL30Abstraction.glBindVertexArray(glBaseAndSkinMatrixVAO);
			GL30Abstraction.glBeginTransformFeedback(GL11.GL_POINTS);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
			GL30Abstraction.glEndTransformFeedback();
		}
	}

}
