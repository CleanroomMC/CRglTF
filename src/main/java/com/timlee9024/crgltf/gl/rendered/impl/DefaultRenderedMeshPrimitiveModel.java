package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
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
		public static final Morphing DUMMY = DefaultRenderedMeshPrimitiveModel.DUMMY.new Morphing() {

			@Override
			public void restoreAttributesForMorphing() {
			}

			@Override
			public void applyMorphTarget(int target) {
			}

		};

		public class AttributeBundle {
			public int glMorphBuffer;
			public int glBaseAttributesVAO;
			public int[] glMorphTargetVAOs;

			public void restoreAttributesForMorphing() {
				GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glMorphBuffer);
				GL43.glClearBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, GL30.GL_R32F, 0, morphBufferSize, GL11.GL_RED, GL11.GL_FLOAT, (ByteBuffer) null);
				GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfMorphingPassConstants.getInstance().getMorphBufferBinding(), glMorphBuffer);
				GL30.glBindVertexArray(glBaseAttributesVAO);
				GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
			}

			public void applyMorphTarget(int target) {
				GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfMorphingPassConstants.getInstance().getMorphBufferBinding(), glMorphBuffer);
				GL30.glBindVertexArray(glMorphTargetVAOs[target]);
				GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
			}
		}

		public long morphBufferSize;
		public AttributeBundle[] attributeBundles;

		public void restoreAttributesForMorphing() {
			for (AttributeBundle bundle : attributeBundles) bundle.restoreAttributesForMorphing();
		}

		public void applyMorphTarget(int target) {
			for (AttributeBundle bundle : attributeBundles) bundle.applyMorphTarget(target);
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
		public int glSkinBuffer;
		public int glBaseAttributesVAO;
		public int[] glSkinMatrixTargetVAOs;

		public void calculateSkinMatrix() {
			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glSkinBuffer);
			GL43.glClearBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, GL30.GL_R32F, 0, skinMatrixSize, GL11.GL_RED, GL11.GL_FLOAT, (ByteBuffer) null);
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfCalcSkinMatrixPassConstants.getInstance().getSkinBufferBinding(), glSkinBuffer);
			for (int vao : glSkinMatrixTargetVAOs) {
				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT); //For more than 4 bones
				GL30.glBindVertexArray(vao);
				GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
			}
		}

		public void applySkinMatrix() {
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferBinding(), glSkinBuffer);
			GL30.glBindVertexArray(glBaseAttributesVAO);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
		}
	}

}
