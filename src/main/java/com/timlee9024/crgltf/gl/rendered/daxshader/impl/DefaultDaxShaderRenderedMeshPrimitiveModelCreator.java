package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.constants.DaxShaderRenderConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshPrimitiveModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMeshPrimitiveModelCreator;
import com.timlee9024.crgltf.property.GltfMaterialExtra;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.ElementType;
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
			renderedMeshPrimitiveModel = daxShaderRenderedMeshPrimitiveModel = new DefaultDaxShaderRenderedMeshPrimitiveModel();

			AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
			if (indicesAccessorModel != null) {
				int indiceCount = indicesAccessorModel.getCount();
				int mode = meshPrimitiveModel.getMode();
				int type = indicesAccessorModel.getComponentType();
				int offset = indicesAccessorModel.getByteOffset();
				int glBuffer = uploadAndObtainElementArrayBuffer(indicesAccessorModel.getBufferViewModel());

				DefaultDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel = this.daxShaderRenderedMeshPrimitiveModel;
				daxShaderRenderedMeshPrimitiveModel.glDraw = () -> {
					GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glRenderVAO);
					GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
					GL11.glDrawElements(mode, indiceCount, type, offset);
				};
				daxShaderRenderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
					GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
					GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
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
			renderedMeshPrimitiveModel.count = positionsAccessorModel.getCount();
			int glRenderVAO = GL30.glGenVertexArrays();
			renderedMeshPrimitiveModel.glRenderVAO = glRenderVAO;
			glVertexArrays.add(glRenderVAO);
			int glDaxShaderRenderVAO = GL30.glGenVertexArrays();
			daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO = glDaxShaderRenderVAO;
			glVertexArrays.add(glDaxShaderRenderVAO);

			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			if (renderedMeshPrimitiveModel.renderedMaterialModel == null)
				renderedMeshPrimitiveModel.renderedMaterialModel = DefaultDaxShaderRenderedMaterialModel.DEFAULT;
			daxShaderRenderedMeshPrimitiveModel.daxShaderRenderedMaterialModel = (DefaultDaxShaderRenderedMaterialModel) renderedMeshPrimitiveModel.renderedMaterialModel;

			normalsAccessorModel = attributes.get("NORMAL");
			tangentsAccessorModel = attributes.get("TANGENT");

			morphTargets = meshPrimitiveModel.getTargets();
			if (morphTargets.isEmpty()) {
				renderedMeshPrimitiveModel.morphing = DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY;
				setupAttributes();
			} else {
				setupAttributesWithMorphing();
			}
			return daxShaderRenderedMeshPrimitiveModel;
		}
		return null;
	}

	@Override
	public DefaultRenderedMeshPrimitiveModel createAlias(MeshPrimitiveModel meshPrimitiveModel, DefaultRenderedMeshPrimitiveModel baseRenderedMeshPrimitiveModel) {
		if (baseRenderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY) {
			this.meshPrimitiveModel = meshPrimitiveModel;
			this.baseRenderedMeshPrimitiveModel = baseRenderedMeshPrimitiveModel;
			setupRenderAlias();
			setupMorphingSkinningAlias();
			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			daxShaderRenderedMeshPrimitiveModel.daxShaderRenderedMaterialModel = (DefaultDaxShaderRenderedMaterialModel) renderedMeshPrimitiveModel.renderedMaterialModel;
		} else if (baseRenderedMeshPrimitiveModel.skinning != DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY) {
			this.meshPrimitiveModel = meshPrimitiveModel;
			this.baseRenderedMeshPrimitiveModel = baseRenderedMeshPrimitiveModel;
			setupRenderAlias();
			setupSkinningAlias();
			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			daxShaderRenderedMeshPrimitiveModel.daxShaderRenderedMaterialModel = (DefaultDaxShaderRenderedMaterialModel) renderedMeshPrimitiveModel.renderedMaterialModel;
		} else return baseRenderedMeshPrimitiveModel;
		return renderedMeshPrimitiveModel;
	}

	@Override
	protected void setupRenderAlias() {
		renderedMeshPrimitiveModel = daxShaderRenderedMeshPrimitiveModel = new DefaultDaxShaderRenderedMeshPrimitiveModel();

		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			int indiceCount = indicesAccessorModel.getCount();
			int mode = meshPrimitiveModel.getMode();
			int type = indicesAccessorModel.getComponentType();
			int offset = indicesAccessorModel.getByteOffset();
			int glBuffer = glBufferLookup.get(indicesAccessorModel.getBufferViewModel());

			DefaultDaxShaderRenderedMeshPrimitiveModel daxShaderRenderedMeshPrimitiveModel = this.daxShaderRenderedMeshPrimitiveModel;
			daxShaderRenderedMeshPrimitiveModel.glDraw = () -> {
				GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glRenderVAO);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
				GL11.glDrawElements(mode, indiceCount, type, offset);
			};
			daxShaderRenderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
				GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
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
		renderedMeshPrimitiveModel.count = baseRenderedMeshPrimitiveModel.count;
		int glRenderVAO = GL30.glGenVertexArrays();
		renderedMeshPrimitiveModel.glRenderVAO = glRenderVAO;
		glVertexArrays.add(glRenderVAO);
		int glDaxShaderRenderVAO = GL30.glGenVertexArrays();
		daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO = glDaxShaderRenderVAO;
		glVertexArrays.add(glDaxShaderRenderVAO);
	}

	@Override
	protected void setupRequiredAttribute() {
		super.setupRequiredAttribute();

		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		super.setupRequiredAttribute();

		uploadAndBindArrayBuffer(tangentsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());

		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
	}

	@Override
	protected void setupRequiredAttributeFromMorphing() {
		super.setupRequiredAttributeFromMorphing();

		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		super.setupRequiredAttributeFromMorphing();

		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				4,
				GL11.GL_FLOAT,
				false,
				Float.BYTES * ElementType.MAT4.getNumComponents(),
				(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
		GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());

		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
	}

	@Override
	protected void setupRequiredAttributeFromSkinning() {
		super.setupRequiredAttributeFromSkinning();

		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glPositionDestBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.VEC4.getNumComponents(), GL15.GL_STATIC_DRAW);
		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				Float.BYTES * ElementType.VEC4.getNumComponents(),
				0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glNormalDestBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.VEC4.getNumComponents(), GL15.GL_STATIC_DRAW);
		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				Float.BYTES * ElementType.VEC4.getNumComponents(),
				0);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		int glTangentDestBuffer = GL15.glGenBuffers();
		renderedMeshPrimitiveModel.skinning.glTangentDestBuffer = glTangentDestBuffer;
		glBuffers.add(glTangentDestBuffer);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glTangentDestBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.VEC4.getNumComponents(), GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(
				DaxShaderRenderConstants.getInstance().getTangentAttributeIndex(),
				4,
				GL11.GL_FLOAT,
				false,
				Float.BYTES * ElementType.VEC4.getNumComponents(),
				0);
		GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getTangentAttributeIndex());

		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
	}

	@Override
	protected void setupColorAttribute() {
		super.setupColorAttribute();

		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		super.setupColorAttribute();

		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
	}

	@Override
	protected void setupTexcoordAttribute() {
		super.setupTexcoordAttribute();

		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
		if (materialModel instanceof MaterialModelV2 materialModelV2) {
			AccessorModel accessorModel = attributes.get("TEXCOORD_0");
			if (accessorModel != null) {
				AccessorModel texcoordsAccessorModel;

				Integer baseColorTexcoord = materialModelV2.getBaseColorTexcoord();
				if (baseColorTexcoord != null) texcoordsAccessorModel = attributes.get("TEXCOORD_" + baseColorTexcoord);
				else texcoordsAccessorModel = accessorModel;
				uploadAndBindArrayBuffer(texcoordsAccessorModel.getBufferViewModel());
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
					uploadAndBindArrayBuffer(texcoordsAccessorModel.getBufferViewModel());
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
					uploadAndBindArrayBuffer(texcoordsAccessorModel.getBufferViewModel());
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
						uploadAndBindArrayBuffer(texcoordsAccessorModel.getBufferViewModel());
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

	@Override
	protected void setupColorAttributeFromMorphing() {
		super.setupColorAttributeFromMorphing();

		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
		super.setupColorAttributeFromMorphing();

		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
	}

	@Override
	protected void setupTexcoordAttributeFromMorphing() {
		super.setupTexcoordAttributeFromMorphing();

		GL30.glBindVertexArray(daxShaderRenderedMeshPrimitiveModel.glDaxShaderRenderVAO);
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
						Float.BYTES * ElementType.MAT4.getNumComponents(),
						(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
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
						Float.BYTES * ElementType.MAT4.getNumComponents(),
						(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
			} else {
				Integer baseColorTexcoord = materialModelV2.getBaseColorTexcoord();
				if (baseColorTexcoord != null) {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[baseColorTexcoord].glMorphBuffer);
					GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
					GL11.glTexCoordPointer(
							2,
							GL11.GL_FLOAT,
							Float.BYTES * ElementType.MAT4.getNumComponents(),
							(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
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
							Float.BYTES * ElementType.MAT4.getNumComponents(),
							(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
					GL20.glEnableVertexAttribArray(DaxShaderRenderConstants.getInstance().getMcMidTexCoordAttributeIndex());
				}
			}
		}
	}
}
