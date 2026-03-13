package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;

public class DefaultRenderedMeshPrimitiveModel {

	public static final DefaultRenderedMeshPrimitiveModel DUMMY = new DefaultRenderedMeshPrimitiveModel() {

		@Override
		public void render() {
		}

	};

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
		public static final Morphing DUMMY = DefaultRenderedMeshPrimitiveModel.DUMMY.new Morphing() {

			@Override
			public void restoreAttributesForMorphing() {
			}

			@Override
			public void applyMorphWeight(int target, float weight) {
			}

		};

		public class AttributeBundle {
			public int glOriginalAttributesVAO;
			public int glMorphBuffer;
			public int[] glMorphTargetVAOs;

			public void restoreAttributesForMorphing() {
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glMorphBuffer);
				GL43.glClearBufferSubData(GL15.GL_ARRAY_BUFFER, GL30.GL_R32F, 0, morphBufferSize, GL11.GL_RED, GL11.GL_FLOAT, (ByteBuffer) null);
				GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfMorphingPassConstants.getInstance().getMorphBuffer(), glMorphBuffer);
				GL30.glBindVertexArray(glOriginalAttributesVAO);
				GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
			}

			public void applyMorphWeight(int target) {
				GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfMorphingPassConstants.getInstance().getMorphBuffer(), glMorphBuffer);
				GL30.glBindVertexArray(glMorphTargetVAOs[target]);
				GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
			}
		}

		public long morphBufferSize;
		public AttributeBundle[] attributeBundles;

		public void restoreAttributesForMorphing() {
			GL20.glUniform1f(GltfMorphingPassConstants.getInstance().getWeight(), 1);
			for (AttributeBundle bundle : attributeBundles) bundle.restoreAttributesForMorphing();
		}

		public void applyMorphWeight(int target, float weight) {
			GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
			GL20.glUniform1f(GltfMorphingPassConstants.getInstance().getWeight(), weight);
			for (AttributeBundle bundle : attributeBundles) bundle.applyMorphWeight(target);
		}

	}

	public class Skinning {
		public static final Skinning DUMMY = DefaultRenderedMeshPrimitiveModel.DUMMY.new Skinning() {

			@Override
			public void calculateSkinMatrix() {
			}

			@Override
			public void applySkinMatrix() {
			}

		};

		public long skinMatrixSize;
		public int glSkinMatrixBuffer;
		public int[] glSkinMatrixTargetVAOs;

		public int glVertexSrcVAO;
		public int glAttributesBuffer;

		public void calculateSkinMatrix() {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glSkinMatrixBuffer);
			GL43.glClearBufferSubData(GL15.GL_ARRAY_BUFFER, GL30.GL_R32F, 0, skinMatrixSize, GL11.GL_RED, GL11.GL_FLOAT, (ByteBuffer) null);
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrices(), glSkinMatrixBuffer);
			for (int vao : glSkinMatrixTargetVAOs) {
				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT); //For more than 4 bones
				GL30.glBindVertexArray(vao);
				GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
			}
		}

		public void applySkinMatrix() {
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfApplySkinMatrixPassConstants.getInstance().getAttributes(), glAttributesBuffer);
			GL30.glBindVertexArray(glVertexSrcVAO);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
		}
	}

}
