package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.gl.constants.GltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.ElementType;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.List;
import java.util.Map;

public class DefaultRenderedMeshPrimitiveModelCreator {

	public OpenGLObjectRefSet glBuffers;
	public OpenGLObjectRefSet glVertexArrays;

	public Map<BufferViewModel, Integer> glBufferLookup;
	public Map<MaterialModel, DefaultRenderedMaterialModel> renderedMaterialModelLookup;

	protected DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel;
	protected DefaultRenderedMeshPrimitiveModel baseRenderedMeshPrimitiveModel;
	protected MeshPrimitiveModel meshPrimitiveModel;
	protected Map<String, AccessorModel> attributes;
	protected AccessorModel positionsAccessorModel;
	protected AccessorModel normalsAccessorModel;
	protected AccessorModel tangentsAccessorModel;
	protected List<Map<String, AccessorModel>> morphTargets;

	public DefaultRenderedMeshPrimitiveModel create(MeshPrimitiveModel meshPrimitiveModel) {
		this.meshPrimitiveModel = meshPrimitiveModel;
		attributes = meshPrimitiveModel.getAttributes();
		positionsAccessorModel = attributes.get("POSITION");
		if (positionsAccessorModel != null) {
			renderedMeshPrimitiveModel = new DefaultRenderedMeshPrimitiveModel();

			AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
			if (indicesAccessorModel != null) {
				int indiceCount = indicesAccessorModel.getCount();
				int mode = meshPrimitiveModel.getMode();
				int type = indicesAccessorModel.getComponentType();
				int offset = indicesAccessorModel.getByteOffset();
				int glBuffer = uploadAndObtainElementArrayBuffer(indicesAccessorModel.getBufferViewModel());

				DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = this.renderedMeshPrimitiveModel;
				renderedMeshPrimitiveModel.glDraw = () -> {
					GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
					GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
					GL11.glDrawElements(mode, indiceCount, type, offset);
				};
			} else {
				int mode = meshPrimitiveModel.getMode();

				DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = this.renderedMeshPrimitiveModel;
				renderedMeshPrimitiveModel.glDraw = () -> {
					GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
					GL11.glDrawArrays(mode, 0, renderedMeshPrimitiveModel.count);
				};
			}
			renderedMeshPrimitiveModel.count = positionsAccessorModel.getCount();
			int glRenderVAO = GL30.glGenVertexArrays();
			renderedMeshPrimitiveModel.glRenderVAO = glRenderVAO;
			glVertexArrays.add(glRenderVAO);

			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			if (renderedMeshPrimitiveModel.renderedMaterialModel == null)
				renderedMeshPrimitiveModel.renderedMaterialModel = DefaultRenderedMaterialModel.DEFAULT;

			normalsAccessorModel = attributes.get("NORMAL");
			tangentsAccessorModel = attributes.get("TANGENT");

			morphTargets = meshPrimitiveModel.getTargets();
			if (morphTargets.isEmpty()) {
				renderedMeshPrimitiveModel.morphing = DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY;
				setupAttributes();
			} else {
				setupAttributesWithMorphing();
			}
			return renderedMeshPrimitiveModel;
		}
		return null;
	}

	public DefaultRenderedMeshPrimitiveModel createAlias(MeshPrimitiveModel meshPrimitiveModel, DefaultRenderedMeshPrimitiveModel baseRenderedMeshPrimitiveModel) {
		if (baseRenderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY) {
			this.meshPrimitiveModel = meshPrimitiveModel;
			this.baseRenderedMeshPrimitiveModel = baseRenderedMeshPrimitiveModel;
			setupRenderAlias();
			setupMorphingSkinningAlias();
			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
		} else if (baseRenderedMeshPrimitiveModel.skinning != DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY) {
			this.meshPrimitiveModel = meshPrimitiveModel;
			this.baseRenderedMeshPrimitiveModel = baseRenderedMeshPrimitiveModel;
			setupRenderAlias();
			setupSkinningAlias();
			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
		} else return baseRenderedMeshPrimitiveModel;
		return renderedMeshPrimitiveModel;
	}

	protected void setupAttributes() {
		int skinMatrixTargetSize = getSkinMatrixTargetSize();
		if (skinMatrixTargetSize > 0) {
			renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
			renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs = new int[skinMatrixTargetSize];
			setupJointAndWeightAttribute();

			int glVertexSrcVAO = GL30.glGenVertexArrays();
			renderedMeshPrimitiveModel.skinning.glVertexSrcVAO = glVertexSrcVAO;
			glVertexArrays.add(glVertexSrcVAO);
			GL30.glBindVertexArray(glVertexSrcVAO);

			renderedMeshPrimitiveModel.skinning.skinMatrixSize = (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.MAT4.getNumComponents();
			setupSkinMatrixtAttribute();

			uploadAndBindArrayBuffer(positionsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getPositionIn(),
					positionsAccessorModel.getElementType().getNumComponents(),
					positionsAccessorModel.getComponentType(),
					false,
					positionsAccessorModel.getByteStride(),
					positionsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getPositionIn());

			uploadAndBindArrayBuffer(normalsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getNormalIn(),
					normalsAccessorModel.getElementType().getNumComponents(),
					normalsAccessorModel.getComponentType(),
					false,
					normalsAccessorModel.getByteStride(),
					normalsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getNormalIn());

			uploadAndBindArrayBuffer(tangentsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getTangentIn(),
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getTangentIn());

			GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

			setupRequiredAttributeFromSkinning();
		} else {
			renderedMeshPrimitiveModel.skinning = DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY;

			GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

			setupRequiredAttribute();
		}
		setupColorAttribute();
		setupTexcoordAttribute();
	}

	protected void setupAttributesWithMorphing() {
		renderedMeshPrimitiveModel.morphing = renderedMeshPrimitiveModel.new Morphing();
		renderedMeshPrimitiveModel.morphing.morphBufferSize = (long) renderedMeshPrimitiveModel.count * Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferStride();
		renderedMeshPrimitiveModel.morphing.attributeBundles = new DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle[getMorphAttributeBundleSize()];

		DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle mainBundle = renderedMeshPrimitiveModel.morphing.attributeBundles[0] = renderedMeshPrimitiveModel.morphing.new AttributeBundle();
		int glMainOriginalAttributesVAO = GL30.glGenVertexArrays();
		mainBundle.glOriginalAttributesVAO = glMainOriginalAttributesVAO;
		glVertexArrays.add(glMainOriginalAttributesVAO);
		GL30.glBindVertexArray(glMainOriginalAttributesVAO);

		uploadAndBindArrayBuffer(positionsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfMorphingPassConstants.getInstance().getPositionTarget(),
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getPositionTarget());

		uploadAndBindArrayBuffer(normalsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfMorphingPassConstants.getInstance().getNormalTarget(),
				normalsAccessorModel.getElementType().getNumComponents(),
				normalsAccessorModel.getComponentType(),
				false,
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getNormalTarget());

		int glBaseTangentBuffer = uploadAndBindArrayBuffer(tangentsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfMorphingPassConstants.getInstance().getTangentTarget(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTangentTarget());
		GL20.glVertexAttribPointer(
				GltfMorphingPassConstants.getInstance().getTangentBase(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTangentBase());

		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if (colorsAccessorModel != null) {
			uploadAndBindArrayBuffer(colorsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfMorphingPassConstants.getInstance().getColorTarget(),
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					false,
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getColorTarget());
		}

		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if (texcoordsAccessorModel != null) {
			uploadAndBindArrayBuffer(texcoordsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfMorphingPassConstants.getInstance().getTexcoordTarget(),
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					false,
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTexcoordTarget());
		}

		mainBundle.glMorphTargetVAOs = new int[morphTargets.size()];
		for (int i = 0; i < morphTargets.size(); i++) {
			Map<String, AccessorModel> morphTarget = morphTargets.get(i);
			int glMorphTargetVAO = GL30.glGenVertexArrays();
			mainBundle.glMorphTargetVAOs[i] = glMorphTargetVAO;
			glVertexArrays.add(glMorphTargetVAO);
			GL30.glBindVertexArray(glMorphTargetVAO);

			AccessorModel targetAccessorModel;
			targetAccessorModel = morphTarget.get("POSITION");
			if (targetAccessorModel != null) {
				uploadAndBindArrayBuffer(targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getPositionTarget(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getPositionTarget());
			}
			targetAccessorModel = morphTarget.get("NORMAL");
			if (targetAccessorModel != null) {
				uploadAndBindArrayBuffer(targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getNormalTarget(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getNormalTarget());
			}
			targetAccessorModel = morphTarget.get("TANGENT");
			if (targetAccessorModel != null) {
				uploadAndBindArrayBuffer(targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getTangentTarget(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTangentTarget());
			}
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBaseTangentBuffer);
			GL20.glVertexAttribPointer(
					GltfMorphingPassConstants.getInstance().getTangentBase(),
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTangentBase());
			targetAccessorModel = morphTarget.get("COLOR_0");
			if (targetAccessorModel != null) {
				uploadAndBindArrayBuffer(targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getColorTarget(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getColorTarget());
			}
			targetAccessorModel = morphTarget.get("TEXCOORD_0");
			if (targetAccessorModel != null) {
				uploadAndBindArrayBuffer(targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getTexcoordTarget(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTexcoordTarget());
			}
		}

		for (int c = 1; c < renderedMeshPrimitiveModel.morphing.attributeBundles.length; c++) {
			DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle bundle = renderedMeshPrimitiveModel.morphing.attributeBundles[c] = renderedMeshPrimitiveModel.morphing.new AttributeBundle();
			int glMorphBuffer = GL15.glGenBuffers();
			bundle.glMorphBuffer = glMorphBuffer;
			glBuffers.add(glMorphBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glMorphBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.morphBufferSize, GL15.GL_STATIC_DRAW);
			int glOriginalAttributesVAO = GL30.glGenVertexArrays();
			bundle.glOriginalAttributesVAO = glOriginalAttributesVAO;
			glVertexArrays.add(glOriginalAttributesVAO);
			GL30.glBindVertexArray(glOriginalAttributesVAO);

			colorsAccessorModel = attributes.get("COLOR_" + c);
			if (colorsAccessorModel != null) {
				uploadAndBindArrayBuffer(colorsAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getColorTarget(),
						colorsAccessorModel.getElementType().getNumComponents(),
						colorsAccessorModel.getComponentType(),
						false,
						colorsAccessorModel.getByteStride(),
						colorsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getColorTarget());
			}

			texcoordsAccessorModel = attributes.get("TEXCOORD_" + c);
			if (texcoordsAccessorModel != null) {
				uploadAndBindArrayBuffer(texcoordsAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getTexcoordTarget(),
						texcoordsAccessorModel.getElementType().getNumComponents(),
						texcoordsAccessorModel.getComponentType(),
						false,
						texcoordsAccessorModel.getByteStride(),
						texcoordsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTexcoordTarget());
			}

			bundle.glMorphTargetVAOs = new int[morphTargets.size()];
			for (int i = 0; i < morphTargets.size(); i++) {
				Map<String, AccessorModel> morphTarget = morphTargets.get(i);
				int glMorphTargetVAO = GL30.glGenVertexArrays();
				bundle.glMorphTargetVAOs[i] = glMorphTargetVAO;
				glVertexArrays.add(glMorphTargetVAO);
				GL30.glBindVertexArray(glMorphTargetVAO);

				AccessorModel targetAccessorModel;
				targetAccessorModel = morphTarget.get("COLOR_" + c);
				if (targetAccessorModel != null) {
					uploadAndBindArrayBuffer(targetAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GltfMorphingPassConstants.getInstance().getColorTarget(),
							targetAccessorModel.getElementType().getNumComponents(),
							targetAccessorModel.getComponentType(),
							false,
							targetAccessorModel.getByteStride(),
							targetAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getColorTarget());
				}
				targetAccessorModel = morphTarget.get("TEXCOORD_" + c);
				if (targetAccessorModel != null) {
					uploadAndBindArrayBuffer(targetAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GltfMorphingPassConstants.getInstance().getTexcoordTarget(),
							targetAccessorModel.getElementType().getNumComponents(),
							targetAccessorModel.getComponentType(),
							false,
							targetAccessorModel.getByteStride(),
							targetAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTexcoordTarget());
				}
			}
		}

		int skinMatrixTargetSize = getSkinMatrixTargetSize();
		if (skinMatrixTargetSize > 0) {
			renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
			renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs = new int[skinMatrixTargetSize];
			setupJointAndWeightAttribute();

			int glVertexSrcVAO = GL30.glGenVertexArrays();
			renderedMeshPrimitiveModel.skinning.glVertexSrcVAO = glVertexSrcVAO;
			glVertexArrays.add(glVertexSrcVAO);
			GL30.glBindVertexArray(glVertexSrcVAO);

			renderedMeshPrimitiveModel.skinning.skinMatrixSize = (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.MAT4.getNumComponents();
			setupSkinMatrixtAttribute();

			int glMorphBuffer = GL15.glGenBuffers();
			mainBundle.glMorphBuffer = glMorphBuffer;
			glBuffers.add(glMorphBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glMorphBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.morphBufferSize, GL15.GL_STATIC_DRAW);

			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getPositionIn(),
					3,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getPositionIn());

			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getNormalIn(),
					3,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getNormalIn());

			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getTangentIn(),
					4,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getTangentIn());

			GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

			setupColorAttributeFromMorphing(); //Ensure color attribute can use glMorphBuffer directly.
			setupRequiredAttributeFromSkinning();
		} else {
			renderedMeshPrimitiveModel.skinning = DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY;

			int glMorphBuffer = GL15.glGenBuffers();
			mainBundle.glMorphBuffer = glMorphBuffer;
			glBuffers.add(glMorphBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glMorphBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.morphBufferSize, GL15.GL_STATIC_DRAW);

			GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

			setupColorAttributeFromMorphing();
			setupRequiredAttributeFromMorphing();
		}
		setupTexcoordAttributeFromMorphing();
	}

	protected void setupRenderAlias() {
		renderedMeshPrimitiveModel = new DefaultRenderedMeshPrimitiveModel();

		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			int indiceCount = indicesAccessorModel.getCount();
			int mode = meshPrimitiveModel.getMode();
			int type = indicesAccessorModel.getComponentType();
			int offset = indicesAccessorModel.getByteOffset();
			int glBuffer = glBufferLookup.get(indicesAccessorModel.getBufferViewModel());

			DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = this.renderedMeshPrimitiveModel;
			renderedMeshPrimitiveModel.glDraw = () -> {
				GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
				GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
				GL11.glDrawElements(mode, indiceCount, type, offset);
			};
		} else {
			int mode = meshPrimitiveModel.getMode();

			DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = this.renderedMeshPrimitiveModel;
			renderedMeshPrimitiveModel.glDraw = () -> {
				GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
				GL11.glDrawArrays(mode, 0, renderedMeshPrimitiveModel.count);
			};
		}
		renderedMeshPrimitiveModel.count = baseRenderedMeshPrimitiveModel.count;
		int glRenderVAO = GL30.glGenVertexArrays();
		renderedMeshPrimitiveModel.glRenderVAO = glRenderVAO;
		glVertexArrays.add(glRenderVAO);
	}

	protected void setupMorphingSkinningAlias() {
		renderedMeshPrimitiveModel.morphing = renderedMeshPrimitiveModel.new Morphing();
		renderedMeshPrimitiveModel.morphing.morphBufferSize = baseRenderedMeshPrimitiveModel.morphing.morphBufferSize;
		renderedMeshPrimitiveModel.morphing.attributeBundles = new DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle[baseRenderedMeshPrimitiveModel.morphing.attributeBundles.length];

		for (int c = 0; c < renderedMeshPrimitiveModel.morphing.attributeBundles.length; c++) {
			DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle baseBundle = baseRenderedMeshPrimitiveModel.morphing.attributeBundles[c];
			DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle bundle = renderedMeshPrimitiveModel.morphing.attributeBundles[c] = renderedMeshPrimitiveModel.morphing.new AttributeBundle();
			int glMorphBuffer = GL15.glGenBuffers();
			bundle.glMorphBuffer = glMorphBuffer;
			glBuffers.add(glMorphBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glMorphBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.morphBufferSize, GL15.GL_STATIC_DRAW);
			bundle.glOriginalAttributesVAO = baseBundle.glOriginalAttributesVAO;
			bundle.glMorphTargetVAOs = baseBundle.glMorphTargetVAOs;
		}

		if (baseRenderedMeshPrimitiveModel.skinning != null) {
			renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
			renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs = baseRenderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs;

			int glVertexSrcVAO = GL30.glGenVertexArrays();
			renderedMeshPrimitiveModel.skinning.glVertexSrcVAO = glVertexSrcVAO;
			glVertexArrays.add(glVertexSrcVAO);
			GL30.glBindVertexArray(glVertexSrcVAO);

			renderedMeshPrimitiveModel.skinning.skinMatrixSize = baseRenderedMeshPrimitiveModel.skinning.skinMatrixSize;
			setupSkinMatrixtAttribute();

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[0].glMorphBuffer);

			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getPositionIn(),
					3,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getPositionIn());

			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getNormalIn(),
					3,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getNormalIn());

			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getTangentIn(),
					4,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getTangentIn());

			GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

			setupColorAttributeFromMorphing(); //Ensure color attribute can use glMorphBuffer directly.
			setupRequiredAttributeFromSkinning();
		} else {
			renderedMeshPrimitiveModel.skinning = DefaultRenderedMeshPrimitiveModel.Skinning.DUMMY;

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[0].glMorphBuffer);

			GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

			setupColorAttributeFromMorphing();
			setupRequiredAttributeFromMorphing();
		}
		setupTexcoordAttributeFromMorphing();
	}

	protected void setupSkinningAlias() {
		renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
		renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs = baseRenderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs;

		int glVertexSrcVAO = GL30.glGenVertexArrays();
		renderedMeshPrimitiveModel.skinning.glVertexSrcVAO = glVertexSrcVAO;
		glVertexArrays.add(glVertexSrcVAO);
		GL30.glBindVertexArray(glVertexSrcVAO);

		renderedMeshPrimitiveModel.skinning.skinMatrixSize = baseRenderedMeshPrimitiveModel.skinning.skinMatrixSize;
		setupSkinMatrixtAttribute();

		positionsAccessorModel = attributes.get("POSITION");
		uploadAndBindArrayBuffer(positionsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getPositionIn(),
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getPositionIn());

		normalsAccessorModel = attributes.get("NORMAL");
		uploadAndBindArrayBuffer(normalsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getNormalIn(),
				normalsAccessorModel.getElementType().getNumComponents(),
				normalsAccessorModel.getComponentType(),
				false,
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getNormalIn());

		tangentsAccessorModel = attributes.get("TANGENT");
		uploadAndBindArrayBuffer(tangentsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getTangentIn(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getTangentIn());

		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

		setupRequiredAttributeFromSkinning();
		setupColorAttribute();
		setupTexcoordAttribute();
	}

	protected void setupJointAndWeightAttribute() {
		for (int i = 0; i < renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs.length; i++) {
			int glSkinMatrixTargetVAO = GL30.glGenVertexArrays();
			renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs[i] = glSkinMatrixTargetVAO;
			glVertexArrays.add(glSkinMatrixTargetVAO);
			GL30.glBindVertexArray(glSkinMatrixTargetVAO);

			AccessorModel jointsAccessorModel = attributes.get("JOINTS_" + i);
			uploadAndBindArrayBuffer(jointsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfCalcSkinMatrixPassConstants.getInstance().getJointIn(),
					jointsAccessorModel.getElementType().getNumComponents(),
					jointsAccessorModel.getComponentType(),
					false,
					jointsAccessorModel.getByteStride(),
					jointsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfCalcSkinMatrixPassConstants.getInstance().getJointIn());

			AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_" + i);
			uploadAndBindArrayBuffer(weightsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfCalcSkinMatrixPassConstants.getInstance().getWeightIn(),
					weightsAccessorModel.getElementType().getNumComponents(),
					weightsAccessorModel.getComponentType(),
					false,
					weightsAccessorModel.getByteStride(),
					weightsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfCalcSkinMatrixPassConstants.getInstance().getWeightIn());
		}
	}

	protected void setupSkinMatrixtAttribute() {
		int glSkinMatrixBuffer = GL15.glGenBuffers();
		renderedMeshPrimitiveModel.skinning.glSkinMatrixBuffer = glSkinMatrixBuffer;
		glBuffers.add(glSkinMatrixBuffer);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinMatrixBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.skinMatrixSize, GL15.GL_STATIC_DRAW);
		for (int i = 0; i < 4; i++) {
			GL20.glVertexAttribPointer(
					GltfApplySkinMatrixPassConstants.getInstance().getSkinMatrixIn() + i,
					4,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * ElementType.VEC4.getNumComponents() * i);
			GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getSkinMatrixIn() + i);
		}
	}

	protected void setupRequiredAttribute() {
		uploadAndBindArrayBuffer(positionsAccessorModel.getBufferViewModel());
		GL11.glVertexPointer(
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		uploadAndBindArrayBuffer(normalsAccessorModel.getBufferViewModel());
		GL11.glNormalPointer(
				normalsAccessorModel.getComponentType(),
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
	}

	protected void setupRequiredAttributeFromMorphing() {
		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				Float.BYTES * ElementType.MAT4.getNumComponents(),
				(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				Float.BYTES * ElementType.MAT4.getNumComponents(),
				(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
	}

	protected void setupRequiredAttributeFromSkinning() {
		renderedMeshPrimitiveModel.skinning.glAttributesBuffer = GL15.glGenBuffers();
		glBuffers.add(renderedMeshPrimitiveModel.skinning.glAttributesBuffer);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glAttributesBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.VEC4.getNumComponents() * 3, GL15.GL_STATIC_DRAW);

		GL11.glVertexPointer(
				3,
				GL11.GL_FLOAT,
				Float.BYTES * ElementType.VEC4.getNumComponents() * 3,
				0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		GL11.glNormalPointer(
				GL11.GL_FLOAT,
				Float.BYTES * ElementType.VEC4.getNumComponents() * 3,
				(long) Float.BYTES * ElementType.VEC4.getNumComponents());
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
	}

	protected void setupColorAttribute() {
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if (colorsAccessorModel != null) {
			uploadAndBindArrayBuffer(colorsAccessorModel.getBufferViewModel());
			GL11.glColorPointer(
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		}
	}

	protected void setupTexcoordAttribute() {
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

				Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
				if (emissiveTexcoord != null) texcoordsAccessorModel = attributes.get("TEXCOORD_" + emissiveTexcoord);
				else texcoordsAccessorModel = accessorModel;
				uploadAndBindArrayBuffer(texcoordsAccessorModel.getBufferViewModel());
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
				GL11.glTexCoordPointer(
						texcoordsAccessorModel.getElementType().getNumComponents(),
						texcoordsAccessorModel.getComponentType(),
						texcoordsAccessorModel.getByteStride(),
						texcoordsAccessorModel.getByteOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
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
				}

				Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
				if (emissiveTexcoord != null) {
					AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_" + emissiveTexcoord);
					uploadAndBindArrayBuffer(texcoordsAccessorModel.getBufferViewModel());
					GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
					GL11.glTexCoordPointer(
							texcoordsAccessorModel.getElementType().getNumComponents(),
							texcoordsAccessorModel.getComponentType(),
							texcoordsAccessorModel.getByteStride(),
							texcoordsAccessorModel.getByteOffset());
					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				}
			}
		}
	}

	protected void setupColorAttributeFromMorphing() {
		if (attributes.get("COLOR_0") != null) {
			GL11.glColorPointer(
					4,
					GL11.GL_FLOAT,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferColorOffset());
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		}
	}

	protected void setupTexcoordAttributeFromMorphing() {
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
				Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
				if (emissiveTexcoord == null) emissiveTexcoord = 0;
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[emissiveTexcoord].glMorphBuffer);
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
				GL11.glTexCoordPointer(
						2,
						GL11.GL_FLOAT,
						Float.BYTES * ElementType.MAT4.getNumComponents(),
						(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
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
				}
				Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
				if (emissiveTexcoord != null) {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[emissiveTexcoord].glMorphBuffer);
					GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
					GL11.glTexCoordPointer(
							2,
							GL11.GL_FLOAT,
							Float.BYTES * ElementType.MAT4.getNumComponents(),
							(long) Float.BYTES * GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				}
			}
		}
	}

	protected int getSkinMatrixTargetSize() {
		int size = 0;
		while (attributes.containsKey("JOINTS_" + size) && attributes.containsKey("WEIGHTS_" + size)) ++size;
		return size;
	}

	protected int getMorphAttributeBundleSize() {
		int size = 0;
		for (Map.Entry<String, AccessorModel> attribute : attributes.entrySet()) {
			String name = attribute.getKey();
			if (name.startsWith("COLOR_")) {
				size = Math.max(size, Integer.parseInt(name.substring("COLOR_".length())));
			} else if (name.startsWith("TEXCOORD_")) {
				size = Math.max(size, Integer.parseInt(name.substring("TEXCOORD_".length())));
			}
		}
		return size + 1;
	}

	protected int uploadAndBindArrayBuffer(BufferViewModel bufferViewModel) {
		Integer glBuffer = glBufferLookup.get(bufferViewModel);
		if (glBuffer == null) {
			glBuffer = GL15.glGenBuffers();
			glBuffers.add(glBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			glBufferLookup.put(bufferViewModel, glBuffer);
		} else GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBuffer);
		return glBuffer;
	}

	protected int uploadAndObtainElementArrayBuffer(BufferViewModel bufferViewModel) {
		Integer glBuffer = glBufferLookup.get(bufferViewModel);
		if (glBuffer == null) {
			glBuffer = GL15.glGenBuffers();
			glBuffers.add(glBuffer);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
			GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			glBufferLookup.put(bufferViewModel, glBuffer);
		}
		return glBuffer;
	}
}
