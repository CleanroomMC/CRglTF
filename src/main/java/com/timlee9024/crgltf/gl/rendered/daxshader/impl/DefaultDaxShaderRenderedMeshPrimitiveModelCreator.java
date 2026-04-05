package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.constants.DaxShaderRenderConstants;
import com.timlee9024.crgltf.gl.constants.GltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshPrimitiveModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshPrimitiveModelCreator;
import com.timlee9024.crgltf.property.GltfMaterialExtra;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.Map;

public class DefaultDaxShaderRenderedMeshPrimitiveModelCreator extends DefaultRenderedMeshPrimitiveModelCreator {

	public Map<MaterialModel, GltfMaterialExtra> gltfMaterialExtraLookup;

	protected DefaultDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel;

	@Override
	public DefaultDaxShaderRenderedMeshPrimitiveModel create(MeshPrimitiveModel meshPrimitiveModel) {
		this.meshPrimitiveModel = meshPrimitiveModel;
		attributes = meshPrimitiveModel.getAttributes();
		positionsAccessorModel = attributes.get("POSITION");
		if (positionsAccessorModel != null) {
			normalsAccessorModel = attributes.get("NORMAL");
			tangentsAccessorModel = attributes.get("TANGENT");
			morphTargets = meshPrimitiveModel.getTargets();
			skinMatrixTargetSize = getSkinMatrixTargetSize();

			renderedMeshPrimitiveModel = daxShaderRenderedMeshPrimitiveModel = new DefaultDaxShaderRenderedMeshPrimitiveModel();
			renderedMeshPrimitiveModel.count = positionsAccessorModel.getCount();

			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			if (renderedMeshPrimitiveModel.renderedMaterialModel == null)
				renderedMeshPrimitiveModel.renderedMaterialModel = DefaultDaxShaderRenderedMaterialModel.DEFAULT;
			daxShaderRenderedMeshPrimitiveModel.daxShaderRenderedMaterialModel = (DefaultDaxShaderRenderedMaterialModel) renderedMeshPrimitiveModel.renderedMaterialModel;

			glVertexArrays.add(renderedMeshPrimitiveModel.glRenderVAO = GL30.glGenVertexArrays());
			glVertexArrays.add(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO = GL30.glGenVertexArrays());

			if (morphTargets.isEmpty()) {
				renderedMeshPrimitiveModel.morphing = DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY;
				if (skinMatrixTargetSize == 0) {
					renderedMeshPrimitiveModel.skinning = DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY;
					setupRenderPass();
				} else {
					setupSkinning();
					setupRenderPassFromSkinning();
				}
			} else {
				setupMorphing();
				if (skinMatrixTargetSize == 0) {
					renderedMeshPrimitiveModel.skinning = DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY;
					setupRenderPassFromMorphing();
				} else {
					setupSkinningFromMorphing();
					setupRenderPassFromMorphingAndSkinning();
				}
			}
			return daxShaderRenderedMeshPrimitiveModel;
		}
		return DefaultDaxShaderRenderedMeshPrimitiveModel.DUMMY;
	}

	@Override
	public DefaultRenderedMeshPrimitiveModel createAlias(MeshPrimitiveModel meshPrimitiveModel, DefaultRenderedMeshPrimitiveModel baseRenderedMeshPrimitiveModel) {
		if (baseRenderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY) {
			this.meshPrimitiveModel = meshPrimitiveModel;
			this.baseRenderedMeshPrimitiveModel = baseRenderedMeshPrimitiveModel;

			renderedMeshPrimitiveModel = daxShaderRenderedMeshPrimitiveModel = new DefaultDaxShaderRenderedMeshPrimitiveModel();
			renderedMeshPrimitiveModel.count = baseRenderedMeshPrimitiveModel.count;
			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			daxShaderRenderedMeshPrimitiveModel.daxShaderRenderedMaterialModel = (DefaultDaxShaderRenderedMaterialModel) renderedMeshPrimitiveModel.renderedMaterialModel;

			glVertexArrays.add(renderedMeshPrimitiveModel.glRenderVAO = GL30.glGenVertexArrays());
			glVertexArrays.add(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO = GL30.glGenVertexArrays());

			setupMorphingAlias();
			if (baseRenderedMeshPrimitiveModel.skinning != DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY) {
				setupSkinningFromMorphingAlias();
				setupRenderPassFromMorphingAndSkinning();
			} else {
				renderedMeshPrimitiveModel.skinning = DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY;
				setupRenderPassFromMorphing();
			}
		} else if (baseRenderedMeshPrimitiveModel.skinning != DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY) {
			this.meshPrimitiveModel = meshPrimitiveModel;
			this.baseRenderedMeshPrimitiveModel = baseRenderedMeshPrimitiveModel;

			renderedMeshPrimitiveModel = daxShaderRenderedMeshPrimitiveModel = new DefaultDaxShaderRenderedMeshPrimitiveModel();
			renderedMeshPrimitiveModel.morphing = DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY;
			renderedMeshPrimitiveModel.count = baseRenderedMeshPrimitiveModel.count;
			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			daxShaderRenderedMeshPrimitiveModel.daxShaderRenderedMaterialModel = (DefaultDaxShaderRenderedMaterialModel) renderedMeshPrimitiveModel.renderedMaterialModel;

			glVertexArrays.add(renderedMeshPrimitiveModel.glRenderVAO = GL30.glGenVertexArrays());
			glVertexArrays.add(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO = GL30.glGenVertexArrays());

			setupSkinningAlias();
			setupRenderPassFromSkinning();
		} else return baseRenderedMeshPrimitiveModel;
		return renderedMeshPrimitiveModel;
	}

	@Override
	protected void setupRenderPass() {
		super.setupRenderPass();
		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);

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
		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[0].glMorphBuffer);

		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				4,
				GL11.GL_FLOAT,
				false,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
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
		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer);

		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferPositionOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferNormalOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				4,
				GL11.GL_FLOAT,
				false,
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferTangentOffset());
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
		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer);

		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferPositionOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferNormalOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				4,
				GL11.GL_FLOAT,
				false,
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(),
				GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferTangentOffset());
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
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[baseColorTexcoord].glMorphBuffer);
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
				GL11.glTexCoordPointer(
						2,
						GL11.GL_FLOAT,
						GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
						GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

				GltfMaterialExtra gltfMaterialExtra = gltfMaterialExtraLookup.get(materialModel);
				if (gltfMaterialExtra != null && gltfMaterialExtra.crgltf != null && gltfMaterialExtra.crgltf.daxShader != null && gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord != null) {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord].glMorphBuffer);
				}
				GL20.glVertexAttribPointer(
						DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex(),
						2,
						GL11.GL_FLOAT,
						false,
						GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
						GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
			} else {
				Integer baseColorTexcoord = materialModelV2.getBaseColorTexcoord();
				if (baseColorTexcoord != null) {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[baseColorTexcoord].glMorphBuffer);
					GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
					GL11.glTexCoordPointer(
							2,
							GL11.GL_FLOAT,
							GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
							GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

					GltfMaterialExtra gltfMaterialExtra = gltfMaterialExtraLookup.get(materialModel);
					if (gltfMaterialExtra != null && gltfMaterialExtra.crgltf != null && gltfMaterialExtra.crgltf.daxShader != null && gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord != null) {
						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord].glMorphBuffer);
					}
					GL20.glVertexAttribPointer(
							DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex(),
							2,
							GL11.GL_FLOAT,
							false,
							GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
							GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
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

			DefaultDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel = this.daxShaderRenderedMeshPrimitiveModel;
			daxShaderRenderedMeshPrimitiveModel.glDraw = () -> {
				GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glRenderVAO);
				GL11.glDrawElements(mode, indiceCount, type, offset);
			};
			daxShaderRenderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
				GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
				GL11.glDrawElements(mode, indiceCount, type, offset);
			};
		} else {
			int mode = meshPrimitiveModel.getMode();

			DefaultDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel = this.daxShaderRenderedMeshPrimitiveModel;
			daxShaderRenderedMeshPrimitiveModel.glDraw = () -> {
				GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glRenderVAO);
				GL11.glDrawArrays(mode, 0, daxShaderRenderedMeshPrimitiveModel.count);
			};
			daxShaderRenderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
				GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
				GL11.glDrawArrays(mode, 0, daxShaderRenderedMeshPrimitiveModel.count);
			};
		}
	}

}
