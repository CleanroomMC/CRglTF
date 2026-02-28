package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.List;

public class GL21RenderedMeshPrimitiveModel {

	public static class AttributeBundle {
		public int glStaticBuffer = -1;
		public int componentNum;
		public int componentType;
		public int byteStride;
		public long byteOffset;
	}

	public int glStaticPositionBuffer = -1;
	public int positionComponentNum;
	public int positionComponentType;
	public int positionByteStride;
	public long positionByteOffset;

	public int glStaticNormalBuffer = -1;
	public int normalComponentType;
	public int normalByteStride;
	public long normalByteOffset;

	public int glStaticTangentBuffer = -1;
	public int tangentComponentNum;
	public int tangentComponentType;
	public int tangentByteStride;
	public long tangentByteOffset;

	public AttributeBundle[] colorAttributes;
	public AttributeBundle[] texcoordAttributes;

	public DefaultRenderedMaterialModel renderedMaterialModel;
	public Runnable glDraw;


	public float[] weights;
	public Matrix4fc[] jointMatrices;
	public Matrix4f[] skinMatrices;
	public FloatBuffer dynamicFloatBuffer;
	public int glDynamicBuffer;

	public List<Runnable> skinMatrixCalculations;
	public List<Runnable> attributeUpdates;

	public void render() {
		renderedMaterialModel.render(glDraw);
	}

	public void updateMorphing() {
		attributeUpdates.parallelStream().forEach(Runnable::run);
	}

	public void updateMorphingAndSkinning() {
		skinMatrixCalculations.parallelStream().forEach(Runnable::run);
		attributeUpdates.parallelStream().forEach(Runnable::run);
	}

	public void bindDynamicAttributes(int colorIndex, int texcoordIndex, int emissiveTexcoordIndex) {
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glDynamicBuffer);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, dynamicFloatBuffer);

		if (glStaticPositionBuffer < 0) {
			GL11.glVertexPointer(
					positionComponentNum,
					positionComponentType,
					positionByteStride,
					positionByteOffset);
		}

		if (glStaticNormalBuffer < 0) {
			GL11.glNormalPointer(
					normalComponentType,
					normalByteStride,
					normalByteOffset);
		}

		if (colorIndex >= 0) {
			AttributeBundle attribute = colorAttributes[colorIndex];
			if (attribute.glStaticBuffer < 0) {
				GL11.glColorPointer(
						attribute.componentNum,
						attribute.componentType,
						attribute.byteStride,
						attribute.byteOffset);
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			}
		}

		if (texcoordIndex >= 0) {
			AttributeBundle attribute = texcoordAttributes[texcoordIndex];
			if (attribute.glStaticBuffer < 0) {
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
				GL11.glTexCoordPointer(
						attribute.componentNum,
						attribute.componentType,
						attribute.byteStride,
						attribute.byteOffset);
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			}
		}

		if (emissiveTexcoordIndex >= 0) {
			AttributeBundle attribute = texcoordAttributes[emissiveTexcoordIndex];
			if (attribute.glStaticBuffer < 0) {
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
				GL11.glTexCoordPointer(
						attribute.componentNum,
						attribute.componentType,
						attribute.byteStride,
						attribute.byteOffset);
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			}
		}
	}

	public void bindStaticAttributes(int colorIndex, int texcoordIndex, int emissiveTexcoordIndex) {
		if (glStaticPositionBuffer >= 0) {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glStaticPositionBuffer);
			GL11.glVertexPointer(
					positionComponentNum,
					positionComponentType,
					positionByteStride,
					positionByteOffset);
		}

		if (glStaticNormalBuffer >= 0) {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glStaticNormalBuffer);
			GL11.glNormalPointer(
					normalComponentType,
					normalByteStride,
					normalByteOffset);
		}

		if (colorIndex >= 0) {
			AttributeBundle attribute = colorAttributes[colorIndex];
			if (attribute.glStaticBuffer >= 0) {
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, attribute.glStaticBuffer);
				GL11.glColorPointer(
						attribute.componentNum,
						attribute.componentType,
						attribute.byteStride,
						attribute.byteOffset);
				GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			}
		} else {
			GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		}

		if (texcoordIndex >= 0) {
			AttributeBundle attribute = texcoordAttributes[texcoordIndex];
			if (attribute.glStaticBuffer >= 0) {
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, attribute.glStaticBuffer);
				GL11.glTexCoordPointer(
						attribute.componentNum,
						attribute.componentType,
						attribute.byteStride,
						attribute.byteOffset);
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			}
		} else {
			GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		}

		if (emissiveTexcoordIndex >= 0) {
			AttributeBundle attribute = texcoordAttributes[emissiveTexcoordIndex];
			if (attribute.glStaticBuffer >= 0) {
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, attribute.glStaticBuffer);
				GL11.glTexCoordPointer(
						attribute.componentNum,
						attribute.componentType,
						attribute.byteStride,
						attribute.byteOffset);
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			}
		} else {
			GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		}
	}
}
