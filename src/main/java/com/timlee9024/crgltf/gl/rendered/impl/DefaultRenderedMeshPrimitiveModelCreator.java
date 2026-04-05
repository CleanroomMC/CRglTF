package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.gl.constants.GltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.BufferViewModel;
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
	protected int skinMatrixTargetSize;

	public DefaultRenderedMeshPrimitiveModel create(MeshPrimitiveModel meshPrimitiveModel) {
		this.meshPrimitiveModel = meshPrimitiveModel;
		attributes = meshPrimitiveModel.getAttributes();
		positionsAccessorModel = attributes.get("POSITION");
		if (positionsAccessorModel != null) {
			normalsAccessorModel = attributes.get("NORMAL");
			tangentsAccessorModel = attributes.get("TANGENT");
			morphTargets = meshPrimitiveModel.getTargets();
			skinMatrixTargetSize = getSkinMatrixTargetSize();

			renderedMeshPrimitiveModel = new DefaultRenderedMeshPrimitiveModel();
			renderedMeshPrimitiveModel.count = positionsAccessorModel.getCount();

			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			if (renderedMeshPrimitiveModel.renderedMaterialModel == null)
				renderedMeshPrimitiveModel.renderedMaterialModel = DefaultRenderedMaterialModel.DEFAULT;

			glVertexArrays.add(renderedMeshPrimitiveModel.glRenderVAO = GL30.glGenVertexArrays());

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
			return renderedMeshPrimitiveModel;
		}
		return DefaultRenderedMeshPrimitiveModel.DUMMY;
	}

	public DefaultRenderedMeshPrimitiveModel createAlias(MeshPrimitiveModel meshPrimitiveModel, DefaultRenderedMeshPrimitiveModel baseRenderedMeshPrimitiveModel) {
		if (baseRenderedMeshPrimitiveModel.morphing != DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY) {
			this.meshPrimitiveModel = meshPrimitiveModel;
			this.baseRenderedMeshPrimitiveModel = baseRenderedMeshPrimitiveModel;

			renderedMeshPrimitiveModel = new DefaultRenderedMeshPrimitiveModel();
			renderedMeshPrimitiveModel.count = baseRenderedMeshPrimitiveModel.count;
			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());

			glVertexArrays.add(renderedMeshPrimitiveModel.glRenderVAO = GL30.glGenVertexArrays());

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

			renderedMeshPrimitiveModel = new DefaultRenderedMeshPrimitiveModel();
			renderedMeshPrimitiveModel.morphing = DefaultRenderedMeshPrimitiveModel.Morphing.DUMMY;
			renderedMeshPrimitiveModel.count = baseRenderedMeshPrimitiveModel.count;
			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());

			glVertexArrays.add(renderedMeshPrimitiveModel.glRenderVAO = GL30.glGenVertexArrays());

			setupSkinningAlias();
			setupRenderPassFromSkinning();
		} else return baseRenderedMeshPrimitiveModel;
		return renderedMeshPrimitiveModel;
	}

	protected void setupMorphing() {
		renderedMeshPrimitiveModel.morphing = renderedMeshPrimitiveModel.new Morphing();
		renderedMeshPrimitiveModel.morphing.morphBufferSize = (long) renderedMeshPrimitiveModel.count * GltfMorphingPassConstants.getInstance().getMorphBufferStride();
		renderedMeshPrimitiveModel.morphing.attributeBundles = new DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle[getMorphAttributeBundleSize()];

		DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle mainBundle = renderedMeshPrimitiveModel.morphing.attributeBundles[0] = renderedMeshPrimitiveModel.morphing.new AttributeBundle();
		glVertexArrays.add(mainBundle.glBaseAttributesVAO = GL30.glGenVertexArrays());
		GL30.glBindVertexArray(mainBundle.glBaseAttributesVAO);

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, positionsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfMorphingPassConstants.getInstance().getPositionTargetAttribute(),
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getPositionTargetAttribute());

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, normalsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfMorphingPassConstants.getInstance().getNormalTargetAttribute(),
				normalsAccessorModel.getElementType().getNumComponents(),
				normalsAccessorModel.getComponentType(),
				false,
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getNormalTargetAttribute());

		int glBaseTangentBuffer = uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, tangentsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfMorphingPassConstants.getInstance().getTangentTargetAttribute(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTangentTargetAttribute());
		GL20.glVertexAttribPointer(
				GltfMorphingPassConstants.getInstance().getTangentBaseAttribute(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTangentBaseAttribute());

		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if (colorsAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, colorsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					false,
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getColorTargetAttribute());
		}

		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if (texcoordsAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					false,
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute());
		}

		mainBundle.glMorphTargetVAOs = new int[morphTargets.size()];
		for (int i = 0; i < morphTargets.size(); i++) {
			Map<String, AccessorModel> morphTarget = morphTargets.get(i);
			glVertexArrays.add(mainBundle.glMorphTargetVAOs[i] = GL30.glGenVertexArrays());
			GL30.glBindVertexArray(mainBundle.glMorphTargetVAOs[i]);

			AccessorModel targetAccessorModel;
			targetAccessorModel = morphTarget.get("POSITION");
			if (targetAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getPositionTargetAttribute(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
			} else {
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getPositionTargetAttribute(),
						3,
						GL11.GL_FLOAT,
						false,
						0,
						0);
			}
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getPositionTargetAttribute());

			targetAccessorModel = morphTarget.get("NORMAL");
			if (targetAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getNormalTargetAttribute(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
			} else {
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getNormalTargetAttribute(),
						3,
						GL11.GL_FLOAT,
						false,
						0,
						0);
			}
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getNormalTargetAttribute());

			targetAccessorModel = morphTarget.get("TANGENT");
			if (targetAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getTangentTargetAttribute(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
			} else {
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getTangentTargetAttribute(),
						3,
						GL11.GL_FLOAT,
						false,
						0,
						0);
			}
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTangentTargetAttribute());

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBaseTangentBuffer);
			GL20.glVertexAttribPointer(
					GltfMorphingPassConstants.getInstance().getTangentBaseAttribute(),
					tangentsAccessorModel.getElementType().getNumComponents(),
					tangentsAccessorModel.getComponentType(),
					false,
					tangentsAccessorModel.getByteStride(),
					tangentsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTangentBaseAttribute());

			targetAccessorModel = morphTarget.get("COLOR_0");
			if (targetAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
			} else {
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
						4,
						GL11.GL_FLOAT,
						false,
						0,
						0);
			}
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getColorTargetAttribute());

			targetAccessorModel = morphTarget.get("TEXCOORD_0");
			if (targetAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
						targetAccessorModel.getElementType().getNumComponents(),
						targetAccessorModel.getComponentType(),
						false,
						targetAccessorModel.getByteStride(),
						targetAccessorModel.getByteOffset());
			} else {
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
						2,
						GL11.GL_FLOAT,
						false,
						0,
						0);
			}
			GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute());
		}

		for (int c = 1; c < renderedMeshPrimitiveModel.morphing.attributeBundles.length; c++) {
			DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle bundle = renderedMeshPrimitiveModel.morphing.attributeBundles[c] = renderedMeshPrimitiveModel.morphing.new AttributeBundle();

			glBuffers.add(bundle.glMorphBuffer = GL15.glGenBuffers());
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bundle.glMorphBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.morphBufferSize, GL15.GL_STATIC_DRAW);

			glVertexArrays.add(bundle.glBaseAttributesVAO = GL30.glGenVertexArrays());
			GL30.glBindVertexArray(bundle.glBaseAttributesVAO);

			colorsAccessorModel = attributes.get("COLOR_" + c);
			if (colorsAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, colorsAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
						colorsAccessorModel.getElementType().getNumComponents(),
						colorsAccessorModel.getComponentType(),
						false,
						colorsAccessorModel.getByteStride(),
						colorsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getColorTargetAttribute());
			}

			texcoordsAccessorModel = attributes.get("TEXCOORD_" + c);
			if (texcoordsAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
						texcoordsAccessorModel.getElementType().getNumComponents(),
						texcoordsAccessorModel.getComponentType(),
						false,
						texcoordsAccessorModel.getByteStride(),
						texcoordsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute());
			}

			bundle.glMorphTargetVAOs = new int[morphTargets.size()];
			for (int i = 0; i < morphTargets.size(); i++) {
				Map<String, AccessorModel> morphTarget = morphTargets.get(i);
				glVertexArrays.add(bundle.glMorphTargetVAOs[i] = GL30.glGenVertexArrays());
				GL30.glBindVertexArray(bundle.glMorphTargetVAOs[i]);

				AccessorModel targetAccessorModel;
				targetAccessorModel = morphTarget.get("COLOR_" + c);
				if (targetAccessorModel != null) {
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
							targetAccessorModel.getElementType().getNumComponents(),
							targetAccessorModel.getComponentType(),
							false,
							targetAccessorModel.getByteStride(),
							targetAccessorModel.getByteOffset());
				} else {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
					GL20.glVertexAttribPointer(
							GltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
							4,
							GL11.GL_FLOAT,
							false,
							0,
							0);
				}
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getColorTargetAttribute());

				targetAccessorModel = morphTarget.get("TEXCOORD_" + c);
				if (targetAccessorModel != null) {
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
							targetAccessorModel.getElementType().getNumComponents(),
							targetAccessorModel.getComponentType(),
							false,
							targetAccessorModel.getByteStride(),
							targetAccessorModel.getByteOffset());
				} else {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
					GL20.glVertexAttribPointer(
							GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
							2,
							GL11.GL_FLOAT,
							false,
							0,
							0);
				}
				GL20.glEnableVertexAttribArray(GltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute());
			}
		}

		//This ensures that current buffer binding is always bundle 0 at the end, so it can pass to setupRenderPassFromMorphing() or setupSkinningFromMorphing()
		glBuffers.add(mainBundle.glMorphBuffer = GL15.glGenBuffers());
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mainBundle.glMorphBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.morphBufferSize, GL15.GL_STATIC_DRAW);
	}

	protected void setupSkinning() {
		renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
		renderedMeshPrimitiveModel.skinning.skinMatrixSize = (long) renderedMeshPrimitiveModel.count * GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride();
		renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs = new int[skinMatrixTargetSize];

		glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glBaseAttributesVAO = GL30.glGenVertexArrays());
		GL30.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glBaseAttributesVAO);

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, positionsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getPositionBaseAttribute(),
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getPositionBaseAttribute());

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, normalsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getNormalBaseAttribute(),
				normalsAccessorModel.getElementType().getNumComponents(),
				normalsAccessorModel.getComponentType(),
				false,
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getNormalBaseAttribute());

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, tangentsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getTangentBaseAttribute(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getTangentBaseAttribute());

		setupJointAndWeight();

		//This ensures that current buffer binding is always glSkinBuffer at the end, so it can pass to setupRenderPassFromSkinning()
		glBuffers.add(renderedMeshPrimitiveModel.skinning.glSkinBuffer = GL15.glGenBuffers());
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.skinMatrixSize, GL15.GL_STATIC_DRAW);
	}

	protected void setupSkinningFromMorphing() {
		renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
		renderedMeshPrimitiveModel.skinning.skinMatrixSize = (long) renderedMeshPrimitiveModel.count * GltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride();
		renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs = new int[skinMatrixTargetSize];

		glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glBaseAttributesVAO = GL30.glGenVertexArrays());
		GL30.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glBaseAttributesVAO);

		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getPositionBaseAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getPositionBaseAttribute());

		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getNormalBaseAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getNormalBaseAttribute());

		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getTangentBaseAttribute(),
				4,
				GL11.GL_FLOAT,
				false,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getTangentBaseAttribute());

		setupJointAndWeight();

		//This ensures that current buffer binding is always glSkinBuffer at the end, so it can pass to setupRenderPassFromSkinning()
		glBuffers.add(renderedMeshPrimitiveModel.skinning.glSkinBuffer = GL15.glGenBuffers());
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.skinMatrixSize, GL15.GL_STATIC_DRAW);
	}

	protected void setupMorphingAlias() {
		renderedMeshPrimitiveModel.morphing = renderedMeshPrimitiveModel.new Morphing();
		renderedMeshPrimitiveModel.morphing.morphBufferSize = baseRenderedMeshPrimitiveModel.morphing.morphBufferSize;
		renderedMeshPrimitiveModel.morphing.attributeBundles = new DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle[baseRenderedMeshPrimitiveModel.morphing.attributeBundles.length];

		//This ensures that current buffer binding is always bundle 0 at the end, so it can pass to setupRenderPassFromMorphing()
		for (int i = renderedMeshPrimitiveModel.morphing.attributeBundles.length - 1; i >= 0; i--) {
			DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle baseBundle = baseRenderedMeshPrimitiveModel.morphing.attributeBundles[i];
			DefaultRenderedMeshPrimitiveModel.Morphing.AttributeBundle bundle = renderedMeshPrimitiveModel.morphing.attributeBundles[i] = renderedMeshPrimitiveModel.morphing.new AttributeBundle();
			bundle.glBaseAttributesVAO = baseBundle.glBaseAttributesVAO;
			bundle.glMorphTargetVAOs = baseBundle.glMorphTargetVAOs;

			glBuffers.add(bundle.glMorphBuffer = GL15.glGenBuffers());
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bundle.glMorphBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.morphBufferSize, GL15.GL_STATIC_DRAW);
		}
	}

	protected void setupSkinningAlias() {
		renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
		renderedMeshPrimitiveModel.skinning.skinMatrixSize = baseRenderedMeshPrimitiveModel.skinning.skinMatrixSize;
		renderedMeshPrimitiveModel.skinning.glBaseAttributesVAO = baseRenderedMeshPrimitiveModel.skinning.glBaseAttributesVAO;
		renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs = baseRenderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs;

		glBuffers.add(renderedMeshPrimitiveModel.skinning.glSkinBuffer = GL15.glGenBuffers());
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.skinMatrixSize, GL15.GL_STATIC_DRAW);
	}

	protected void setupSkinningFromMorphingAlias() {
		renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
		renderedMeshPrimitiveModel.skinning.skinMatrixSize = baseRenderedMeshPrimitiveModel.skinning.skinMatrixSize;
		renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs = baseRenderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs;

		glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glBaseAttributesVAO = GL30.glGenVertexArrays());
		GL30.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glBaseAttributesVAO);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[0].glMorphBuffer);

		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getPositionBaseAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getPositionBaseAttribute());

		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getNormalBaseAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getNormalBaseAttribute());

		GL20.glVertexAttribPointer(
				GltfApplySkinMatrixPassConstants.getInstance().getTangentBaseAttribute(),
				4,
				GL11.GL_FLOAT,
				false,
				GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
		GL20.glEnableVertexAttribArray(GltfApplySkinMatrixPassConstants.getInstance().getTangentBaseAttribute());

		glBuffers.add(renderedMeshPrimitiveModel.skinning.glSkinBuffer = GL15.glGenBuffers());
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.skinMatrixSize, GL15.GL_STATIC_DRAW);
	}

	protected void setupJointAndWeight() {
		for (int i = 0; i < renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs.length; i++) {
			glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs[i] = GL30.glGenVertexArrays());
			GL30.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glSkinMatrixTargetVAOs[i]);

			AccessorModel jointsAccessorModel = attributes.get("JOINTS_" + i);
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
					jointsAccessorModel.getElementType().getNumComponents(),
					jointsAccessorModel.getComponentType(),
					false,
					jointsAccessorModel.getByteStride(),
					jointsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

			AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_" + i);
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
					weightsAccessorModel.getElementType().getNumComponents(),
					weightsAccessorModel.getComponentType(),
					false,
					weightsAccessorModel.getByteStride(),
					weightsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());
		}
	}

	protected void setupRenderPass() {
		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

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

		setupColorAttribute();
		setupTexcoordAttribute();

		setupGlDraw();
	}

	protected void setupRenderPassFromMorphing() {
		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

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

		setupColorAttributeFromMorphing();
		setupTexcoordAttributeFromMorphing();

		setupGlDraw();
	}

	protected void setupRenderPassFromSkinning() {
		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

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

		setupColorAttribute();
		setupTexcoordAttribute();

		setupGlDraw();
	}

	protected void setupRenderPassFromMorphingAndSkinning() {
		GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

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

		setupColorAttributeFromMorphing();
		setupTexcoordAttributeFromMorphing();

		setupGlDraw();
	}

	protected void setupColorAttribute() {
		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if (colorsAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, colorsAccessorModel.getBufferViewModel());
			GL11.glColorPointer(
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		}
	}

	protected void setupColorAttributeFromMorphing() {
		if (attributes.get("COLOR_0") != null) {
			GL11.glColorPointer(
					4,
					GL11.GL_FLOAT,
					GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
					GltfMorphingPassConstants.getInstance().getMorphBufferColorOffset());
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
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
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
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
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
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
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
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
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
						GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
						GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
				if (emissiveTexcoord == null) emissiveTexcoord = 0;
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[emissiveTexcoord].glMorphBuffer);
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
				GL11.glTexCoordPointer(
						2,
						GL11.GL_FLOAT,
						GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
						GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
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
				}
				Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
				if (emissiveTexcoord != null) {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[emissiveTexcoord].glMorphBuffer);
					GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
					GL11.glTexCoordPointer(
							2,
							GL11.GL_FLOAT,
							GltfMorphingPassConstants.getInstance().getMorphBufferStride(),
							GltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				}
			}
		}
	}

	protected void setupGlDraw() {
		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			int indiceCount = indicesAccessorModel.getCount();
			int mode = meshPrimitiveModel.getMode();
			int type = indicesAccessorModel.getComponentType();
			int offset = indicesAccessorModel.getByteOffset();
			uploadAndBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesAccessorModel.getBufferViewModel());

			DefaultRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = this.renderedMeshPrimitiveModel;
			renderedMeshPrimitiveModel.glDraw = () -> {
				GL30.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
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

	protected int uploadAndBindBuffer(int target, BufferViewModel bufferViewModel) {
		Integer glBuffer = glBufferLookup.get(bufferViewModel);
		if (glBuffer == null) {
			glBuffers.add(glBuffer = GL15.glGenBuffers());
			GL15.glBindBuffer(target, glBuffer);
			GL15.glBufferData(target, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			glBufferLookup.put(bufferViewModel, glBuffer);
		} else GL15.glBindBuffer(target, glBuffer);
		return glBuffer;
	}
}
