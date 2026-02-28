package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.constants.DaxShaderRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshPrimitiveModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class GL21DaxShaderRenderedMeshPrimitiveModel extends GL21RenderedMeshPrimitiveModel {

	public DefaultDaxShaderRenderedMaterialModel daxShaderRenderedMaterialModel;
	public Runnable glDaxShaderDraw;

	public void renderForDaxShader() {
		daxShaderRenderedMaterialModel.renderForDaxShader(glDaxShaderDraw);
	}

	public void bindDynamicAttributesForDaxShader(int colorIndex, int texcoordIndex, int mcMidTexcoordIndex) {
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

		if (glStaticTangentBuffer < 0) {
			GL20.glVertexAttribPointer(
					DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
					tangentComponentNum,
					tangentComponentType,
					false,
					tangentByteStride,
					tangentByteOffset);
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
				GL11.glTexCoordPointer(
						attribute.componentNum,
						attribute.componentType,
						attribute.byteStride,
						attribute.byteOffset);
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			}
		}

		if (mcMidTexcoordIndex >= 0) {
			AttributeBundle attribute = texcoordAttributes[mcMidTexcoordIndex];
			if (attribute.glStaticBuffer < 0) {
				GL20.glVertexAttribPointer(
						DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex(),
						attribute.componentNum,
						attribute.componentType,
						false,
						attribute.byteStride,
						attribute.byteOffset);
				GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
			}
		}
	}

	public void bindStaticAttributesForDaxShader(int colorIndex, int texcoordIndex, int mcMidTexcoordIndex) {
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

		if (glStaticTangentBuffer >= 0) {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glStaticTangentBuffer);
			GL20.glVertexAttribPointer(
					DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
					tangentComponentNum,
					tangentComponentType,
					false,
					tangentByteStride,
					tangentByteOffset);
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
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, attribute.glStaticBuffer);
				GL11.glTexCoordPointer(
						attribute.componentNum,
						attribute.componentType,
						attribute.byteStride,
						attribute.byteOffset);
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
			}
		} else {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		}

		if (mcMidTexcoordIndex >= 0) {
			AttributeBundle attribute = texcoordAttributes[mcMidTexcoordIndex];
			if (attribute.glStaticBuffer >= 0) {
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, attribute.glStaticBuffer);
				GL20.glVertexAttribPointer(
						DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex(),
						attribute.componentNum,
						attribute.componentType,
						false,
						attribute.byteStride,
						attribute.byteOffset);
				GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
			}
		} else {
			GL20.glDisableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
		}
	}
}
