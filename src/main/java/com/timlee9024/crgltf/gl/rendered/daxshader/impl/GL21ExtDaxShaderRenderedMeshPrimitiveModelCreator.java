package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import com.timlee9024.crgltf.gl.constants.DaxShaderRenderConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfMorphingPassConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedMeshPrimitiveModelCreator;
import com.timlee9024.crgltf.property.GltfMaterialExtra;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.util.Map;

public class GL21ExtDaxShaderRenderedMeshPrimitiveModelCreator extends GL21ExtRenderedMeshPrimitiveModelCreator {

	public Map<MaterialModel, GltfMaterialExtra> gltfMaterialExtraLookup;

	protected GL21ExtDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel;

	@Override
	public GL21ExtDaxShaderRenderedMeshPrimitiveModel create(MeshPrimitiveModel meshPrimitiveModel) {
		this.meshPrimitiveModel = meshPrimitiveModel;
		attributes = meshPrimitiveModel.getAttributes();
		positionsAccessorModel = attributes.get("POSITION");
		if (positionsAccessorModel != null) {
			normalsAccessorModel = attributes.get("NORMAL");
			tangentsAccessorModel = attributes.get("TANGENT");
			morphTargets = meshPrimitiveModel.getTargets();
			skinMatrixTargetSize = getSkinMatrixTargetSize();

			renderedMeshPrimitiveModel = daxShaderRenderedMeshPrimitiveModel = new GL21ExtDaxShaderRenderedMeshPrimitiveModel();
			renderedMeshPrimitiveModel.count = positionsAccessorModel.getCount();

			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			if (renderedMeshPrimitiveModel.renderedMaterialModel == null)
				renderedMeshPrimitiveModel.renderedMaterialModel = DefaultDaxShaderRenderedMaterialModel.DEFAULT;
			daxShaderRenderedMeshPrimitiveModel.daxShaderRenderedMaterialModel = (DefaultDaxShaderRenderedMaterialModel) renderedMeshPrimitiveModel.renderedMaterialModel;

			glVertexArrays.add(renderedMeshPrimitiveModel.glRenderVAO = GL30Abstraction.glGenVertexArrays());
			glVertexArrays.add(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO = GL30Abstraction.glGenVertexArrays());

			if (morphTargets.isEmpty()) {
				renderedMeshPrimitiveModel.morphing = GL21ExtDaxShaderRenderedMeshPrimitiveModel.Morphing.DUMMY;
				if (skinMatrixTargetSize == 0) {
					renderedMeshPrimitiveModel.skinning = GL21ExtDaxShaderRenderedMeshPrimitiveModel.Skinning.DUMMY;
					setupRenderPass();
				} else {
					setupSkinning();
					setupRenderPassFromSkinning();
				}
			} else {
				setupMorphing();
				if (skinMatrixTargetSize == 0) {
					renderedMeshPrimitiveModel.skinning = GL21ExtDaxShaderRenderedMeshPrimitiveModel.Skinning.DUMMY;
					setupRenderPassFromMorphing();
				} else {
					setupSkinningFromMorphing();
					setupRenderPassFromMorphingAndSkinning();
				}
			}
			return daxShaderRenderedMeshPrimitiveModel;
		}
		return GL21ExtDaxShaderRenderedMeshPrimitiveModel.DUMMY;
	}

	@Override
	protected void setupRenderPass() {
		super.setupRenderPass();
		GL30Abstraction.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, positionsAccessorModel.getBufferViewModel());
		GL11.glVertexPointer(
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, normalsAccessorModel.getBufferViewModel());
		GL11.glNormalPointer(
				normalsAccessorModel.getComponentType(),
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, tangentsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());

		setupColorAttribute();
		setupTexcoordAttributeForDaxShader();

		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesAccessorModel.getBufferViewModel());
		}
	}

	@Override
	protected void setupRenderPassFromMorphing() {
		super.setupRenderPassFromMorphing();
		GL30Abstraction.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[0].glMorphBuffer0);

		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				4,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
		GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());

		setupColorAttributeFromMorphing();
		setupTexcoordAttributeFromMorphingForDaxShader();

		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesAccessorModel.getBufferViewModel());
		}
	}

	@Override
	protected void setupRenderPassFromSkinning() {
		super.setupRenderPassFromSkinning();
		GL30Abstraction.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer1);

		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferPositionOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferNormalOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				4,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferTangentOffset());
		GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());

		setupColorAttribute();
		setupTexcoordAttributeForDaxShader();

		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesAccessorModel.getBufferViewModel());
		}
	}

	@Override
	protected void setupRenderPassFromMorphingAndSkinning() {
		super.setupRenderPassFromMorphingAndSkinning();
		GL30Abstraction.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer1);

		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferPositionOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferNormalOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				4,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferTangentOffset());
		GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());

		setupColorAttributeFromMorphing();
		setupTexcoordAttributeFromMorphingForDaxShader();

		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesAccessorModel.getBufferViewModel());
		}
	}

	protected void setupTexcoordAttributeForDaxShader() {
		MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
		if (materialModel instanceof MaterialModelV2 materialModelV2) {
			AccessorModel accessorModel = attributes.get("TEXCOORD_0");
			if (accessorModel != null) {
				AccessorModel texcoordsAccessorModel;

				Integer baseColorTexcoord = materialModelV2.getBaseColorTexcoord();
				if (baseColorTexcoord != null) texcoordsAccessorModel = attributes.get("TEXCOORD_" + baseColorTexcoord);
				else texcoordsAccessorModel = accessorModel;
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
				GL11.glTexCoordPointer(
						texcoordsAccessorModel.getElementType().getNumComponents(),
						texcoordsAccessorModel.getComponentType(),
						texcoordsAccessorModel.getByteStride(),
						texcoordsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

				GltfMaterialExtra gltfMaterialExtra = gltfMaterialExtraLookup.get(materialModel);
				if (gltfMaterialExtra != null && gltfMaterialExtra.crgltf != null && gltfMaterialExtra.crgltf.daxShader != null && gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord != null) {
					texcoordsAccessorModel = attributes.get("TEXCOORD_" + gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord);
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
				}
				GL20.glVertexAttribPointer(
						DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex(),
						texcoordsAccessorModel.getElementType().getNumComponents(),
						texcoordsAccessorModel.getComponentType(),
						false,
						texcoordsAccessorModel.getByteStride(),
						texcoordsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
			} else {
				Integer baseColorTexcoord = materialModelV2.getBaseColorTexcoord();
				if (baseColorTexcoord != null) {
					AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_" + baseColorTexcoord);
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
					GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
					GL11.glTexCoordPointer(
							texcoordsAccessorModel.getElementType().getNumComponents(),
							texcoordsAccessorModel.getComponentType(),
							texcoordsAccessorModel.getByteStride(),
							texcoordsAccessorModel.getByteOffset());
					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

					GltfMaterialExtra gltfMaterialExtra = gltfMaterialExtraLookup.get(materialModel);
					if (gltfMaterialExtra != null && gltfMaterialExtra.crgltf != null && gltfMaterialExtra.crgltf.daxShader != null && gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord != null) {
						texcoordsAccessorModel = attributes.get("TEXCOORD_" + gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord);
						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
					}
					GL20.glVertexAttribPointer(
							DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex(),
							texcoordsAccessorModel.getElementType().getNumComponents(),
							texcoordsAccessorModel.getComponentType(),
							false,
							texcoordsAccessorModel.getByteStride(),
							texcoordsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
				}
			}
		}
	}

	protected void setupTexcoordAttributeFromMorphingForDaxShader() {
		MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
		if (materialModel instanceof MaterialModelV2 materialModelV2) {
			if (attributes.get("TEXCOORD_0") != null) {
				Integer baseColorTexcoord = materialModelV2.getBaseColorTexcoord();
				if (baseColorTexcoord == null) baseColorTexcoord = 0;
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[baseColorTexcoord].glMorphBuffer0);
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
				GL11.glTexCoordPointer(
						2,
						GL11.GL_FLOAT,
						GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
						GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

				GltfMaterialExtra gltfMaterialExtra = gltfMaterialExtraLookup.get(materialModel);
				if (gltfMaterialExtra != null && gltfMaterialExtra.crgltf != null && gltfMaterialExtra.crgltf.daxShader != null && gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord != null) {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord].glMorphBuffer0);
				}
				GL20.glVertexAttribPointer(
						DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex(),
						2,
						GL11.GL_FLOAT,
						false,
						GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
						GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
			} else {
				Integer baseColorTexcoord = materialModelV2.getBaseColorTexcoord();
				if (baseColorTexcoord != null) {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[baseColorTexcoord].glMorphBuffer0);
					GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
					GL11.glTexCoordPointer(
							2,
							GL11.GL_FLOAT,
							GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
							GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

					GltfMaterialExtra gltfMaterialExtra = gltfMaterialExtraLookup.get(materialModel);
					if (gltfMaterialExtra != null && gltfMaterialExtra.crgltf != null && gltfMaterialExtra.crgltf.daxShader != null && gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord != null) {
						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord].glMorphBuffer0);
					}
					GL20.glVertexAttribPointer(
							DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex(),
							2,
							GL11.GL_FLOAT,
							false,
							GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
							GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
					GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
				}
			}
		}
	}

	@Override
	protected void setupGlDraw() {
		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			int indiceCount = indicesAccessorModel.getCount();
			int mode = meshPrimitiveModel.getMode();
			int type = indicesAccessorModel.getComponentType();
			int offset = indicesAccessorModel.getByteOffset();
			uploadAndBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesAccessorModel.getBufferViewModel());

			GL21ExtDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel = this.daxShaderRenderedMeshPrimitiveModel;
			daxShaderRenderedMeshPrimitiveModel.glDraw = () -> {
				GL30Abstraction.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glRenderVAO);
				GL11.glDrawElements(mode, indiceCount, type, offset);
			};
			daxShaderRenderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
				GL30Abstraction.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
				GL11.glDrawElements(mode, indiceCount, type, offset);
			};
		} else {
			int mode = meshPrimitiveModel.getMode();

			GL21ExtDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel = this.daxShaderRenderedMeshPrimitiveModel;
			daxShaderRenderedMeshPrimitiveModel.glDraw = () -> {
				GL30Abstraction.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glRenderVAO);
				GL11.glDrawArrays(mode, 0, daxShaderRenderedMeshPrimitiveModel.count);
			};
			daxShaderRenderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
				GL30Abstraction.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
				GL11.glDrawArrays(mode, 0, daxShaderRenderedMeshPrimitiveModel.count);
			};
		}
	}

}
