package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfMorphingPassConstants;
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

import java.util.List;
import java.util.Map;

public class GL21ExtRenderedMeshPrimitiveModelCreator {

	public OpenGLObjectRefSet glBuffers;
	public OpenGLObjectRefSet glVertexArrays;

	public Map<BufferViewModel, Integer> glBufferLookup;
	public Map<MaterialModel, DefaultRenderedMaterialModel> renderedMaterialModelLookup;
	public int parentNodeJointCount;

	protected GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel;
	protected MeshPrimitiveModel meshPrimitiveModel;
	protected Map<String, AccessorModel> attributes;
	protected AccessorModel positionsAccessorModel;
	protected AccessorModel normalsAccessorModel;
	protected AccessorModel tangentsAccessorModel;
	protected List<Map<String, AccessorModel>> morphTargets;
	protected int skinMatrixTargetSize;

	public GL21ExtRenderedMeshPrimitiveModel create(MeshPrimitiveModel meshPrimitiveModel) {
		this.meshPrimitiveModel = meshPrimitiveModel;
		attributes = meshPrimitiveModel.getAttributes();
		positionsAccessorModel = attributes.get("POSITION");
		if (positionsAccessorModel != null) {
			normalsAccessorModel = attributes.get("NORMAL");
			tangentsAccessorModel = attributes.get("TANGENT");
			morphTargets = meshPrimitiveModel.getTargets();
			skinMatrixTargetSize = getSkinMatrixTargetSize();

			renderedMeshPrimitiveModel = new GL21ExtRenderedMeshPrimitiveModel();
			renderedMeshPrimitiveModel.count = positionsAccessorModel.getCount();

			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			if (renderedMeshPrimitiveModel.renderedMaterialModel == null)
				renderedMeshPrimitiveModel.renderedMaterialModel = DefaultRenderedMaterialModel.DEFAULT;

			glVertexArrays.add(renderedMeshPrimitiveModel.glRenderVAO = GL30Abstraction.glGenVertexArrays());

			if (morphTargets.isEmpty()) {
				renderedMeshPrimitiveModel.morphing = GL21ExtRenderedMeshPrimitiveModel.Morphing.DUMMY;
				if (skinMatrixTargetSize == 0) {
					renderedMeshPrimitiveModel.skinning = GL21ExtRenderedMeshPrimitiveModel.Skinning.DUMMY;
					setupRenderPass();
				} else {
					setupSkinning();
					setupRenderPassFromSkinning();
				}
			} else {
				setupMorphing();
				if (skinMatrixTargetSize == 0) {
					renderedMeshPrimitiveModel.skinning = GL21ExtRenderedMeshPrimitiveModel.Skinning.DUMMY;
					setupRenderPassFromMorphing();
				} else {
					setupSkinningFromMorphing();
					setupRenderPassFromMorphingAndSkinning();
				}
			}
			return renderedMeshPrimitiveModel;
		}
		return GL21ExtRenderedMeshPrimitiveModel.DUMMY;
	}

	public GL21ExtRenderedMeshPrimitiveModel createAlias(MeshPrimitiveModel meshPrimitiveModel, GL21ExtRenderedMeshPrimitiveModel baseRenderedMeshPrimitiveModel) {
		if (baseRenderedMeshPrimitiveModel.morphing == GL21ExtRenderedMeshPrimitiveModel.Morphing.DUMMY && baseRenderedMeshPrimitiveModel.skinning == GL21ExtRenderedMeshPrimitiveModel.Skinning.DUMMY) {
			return baseRenderedMeshPrimitiveModel;
		}
		else {
			return create(meshPrimitiveModel);
		}
	}

	protected void setupMorphing() {
		renderedMeshPrimitiveModel.morphing = renderedMeshPrimitiveModel.new Morphing();
		renderedMeshPrimitiveModel.morphing.attributeBundles = new GL21ExtRenderedMeshPrimitiveModel.Morphing.AttributeBundle[getMorphAttributeBundleSize()];

		long morphBufferSize = (long) renderedMeshPrimitiveModel.count * GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride();

		GL21ExtRenderedMeshPrimitiveModel.Morphing.AttributeBundle mainBundle = renderedMeshPrimitiveModel.morphing.attributeBundles[0] = renderedMeshPrimitiveModel.morphing.new AttributeBundle();
		glBuffers.add(mainBundle.glMorphBuffer0 = GL15.glGenBuffers());
		glBuffers.add(mainBundle.glMorphBuffer1 = GL15.glGenBuffers());

		glVertexArrays.add(mainBundle.glBaseVAO = GL30Abstraction.glGenVertexArrays());
		GL30Abstraction.glBindVertexArray(mainBundle.glBaseVAO);

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, positionsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getPositionAttribute(),
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getPositionAttribute());

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, normalsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getNormalAttribute(),
				normalsAccessorModel.getElementType().getNumComponents(),
				normalsAccessorModel.getComponentType(),
				false,
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getNormalAttribute());

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, tangentsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getTangentAttribute(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTangentAttribute());

		AccessorModel colorsAccessorModel = attributes.get("COLOR_0");
		if (colorsAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, colorsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getColorAttribute(),
					colorsAccessorModel.getElementType().getNumComponents(),
					colorsAccessorModel.getComponentType(),
					false,
					colorsAccessorModel.getByteStride(),
					colorsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getColorAttribute());
		}

		AccessorModel texcoordsAccessorModel = attributes.get("TEXCOORD_0");
		if (texcoordsAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordAttribute(),
					texcoordsAccessorModel.getElementType().getNumComponents(),
					texcoordsAccessorModel.getComponentType(),
					false,
					texcoordsAccessorModel.getByteStride(),
					texcoordsAccessorModel.getByteOffset());
			GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordAttribute());
		}

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getPositionTargetAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getPositionTargetAttribute());
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getNormalTargetAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getNormalTargetAttribute());
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getTangentTargetAttribute(),
				4,
				GL11.GL_FLOAT,
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTangentTargetAttribute());
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
				4,
				GL11.GL_FLOAT,
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute());
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
				2,
				GL11.GL_FLOAT,
				false,
				0,
				0);
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute());

		mainBundle.glMorphBaseAndTargetVAOs = new int[morphTargets.size()][];
		for (int i = 0; i < morphTargets.size(); i++) {
			Map<String, AccessorModel> morphTarget = morphTargets.get(i);
			mainBundle.glMorphBaseAndTargetVAOs[i] = new int[2];

			glVertexArrays.add(mainBundle.glMorphBaseAndTargetVAOs[i][0] = GL30Abstraction.glGenVertexArrays());
			GL30Abstraction.glBindVertexArray(mainBundle.glMorphBaseAndTargetVAOs[i][0]);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mainBundle.glMorphBuffer0);
			setupMorphingBundle0Attributes(morphTarget);

			glVertexArrays.add(mainBundle.glMorphBaseAndTargetVAOs[i][1] = GL30Abstraction.glGenVertexArrays());
			GL30Abstraction.glBindVertexArray(mainBundle.glMorphBaseAndTargetVAOs[i][1]);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mainBundle.glMorphBuffer1);
			setupMorphingBundle0Attributes(morphTarget);
		}

		for (int b = 1; b < renderedMeshPrimitiveModel.morphing.attributeBundles.length; b++) {
			GL21ExtRenderedMeshPrimitiveModel.Morphing.AttributeBundle bundle = renderedMeshPrimitiveModel.morphing.attributeBundles[b] = renderedMeshPrimitiveModel.morphing.new AttributeBundle();
			glBuffers.add(bundle.glMorphBuffer0 = GL15.glGenBuffers());
			glBuffers.add(bundle.glMorphBuffer1 = GL15.glGenBuffers());

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bundle.glMorphBuffer0);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, morphBufferSize, GL15.GL_STATIC_DRAW);

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bundle.glMorphBuffer1);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, morphBufferSize, GL15.GL_STATIC_DRAW);

			glVertexArrays.add(bundle.glBaseVAO = GL30Abstraction.glGenVertexArrays());
			GL30Abstraction.glBindVertexArray(bundle.glBaseVAO);

			colorsAccessorModel = attributes.get("COLOR_" + b);
			if (colorsAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, colorsAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GL21ExtGltfMorphingPassConstants.getInstance().getColorAttribute(),
						colorsAccessorModel.getElementType().getNumComponents(),
						colorsAccessorModel.getComponentType(),
						false,
						colorsAccessorModel.getByteStride(),
						colorsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getColorAttribute());
			}

			texcoordsAccessorModel = attributes.get("TEXCOORD_" + b);
			if (texcoordsAccessorModel != null) {
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, texcoordsAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordAttribute(),
						texcoordsAccessorModel.getElementType().getNumComponents(),
						texcoordsAccessorModel.getComponentType(),
						false,
						texcoordsAccessorModel.getByteStride(),
						texcoordsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordAttribute());
			}

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
					4,
					GL11.GL_FLOAT,
					false,
					0,
					0);
			GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
					2,
					GL11.GL_FLOAT,
					false,
					0,
					0);
			GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute());

			bundle.glMorphBaseAndTargetVAOs = new int[morphTargets.size()][];
			for (int i = 0; i < morphTargets.size(); i++) {
				Map<String, AccessorModel> morphTarget = morphTargets.get(i);
				bundle.glMorphBaseAndTargetVAOs[i] = new int[2];

				glVertexArrays.add(bundle.glMorphBaseAndTargetVAOs[i][0] = GL30Abstraction.glGenVertexArrays());
				GL30Abstraction.glBindVertexArray(bundle.glMorphBaseAndTargetVAOs[i][0]);
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bundle.glMorphBuffer0);
				setupMorphingBundleNAttributes(morphTarget, b);

				glVertexArrays.add(bundle.glMorphBaseAndTargetVAOs[i][1] = GL30Abstraction.glGenVertexArrays());
				GL30Abstraction.glBindVertexArray(bundle.glMorphBaseAndTargetVAOs[i][1]);
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bundle.glMorphBuffer1);
				setupMorphingBundleNAttributes(morphTarget, b);
			}
		}

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mainBundle.glMorphBuffer1);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, morphBufferSize, GL15.GL_STATIC_DRAW);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mainBundle.glMorphBuffer0);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, morphBufferSize, GL15.GL_STATIC_DRAW);
	}

	protected void setupSkinning() {
		renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
		renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs = new int[skinMatrixTargetSize][];

		glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glBaseAndSkinMatrixVAO = GL30Abstraction.glGenVertexArrays());
		GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glBaseAndSkinMatrixVAO);

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, positionsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getPositionAttribute(),
				positionsAccessorModel.getElementType().getNumComponents(),
				positionsAccessorModel.getComponentType(),
				false,
				positionsAccessorModel.getByteStride(),
				positionsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getPositionAttribute());

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, normalsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getNormalAttribute(),
				normalsAccessorModel.getElementType().getNumComponents(),
				normalsAccessorModel.getComponentType(),
				false,
				normalsAccessorModel.getByteStride(),
				normalsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getNormalAttribute());

		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, tangentsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getTangentAttribute(),
				tangentsAccessorModel.getElementType().getNumComponents(),
				tangentsAccessorModel.getComponentType(),
				false,
				tangentsAccessorModel.getByteStride(),
				tangentsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getTangentAttribute());

		glBuffers.add(renderedMeshPrimitiveModel.skinning.glSkinBuffer0 = GL15.glGenBuffers());
		glBuffers.add(renderedMeshPrimitiveModel.skinning.glSkinBuffer1 = GL15.glGenBuffers());

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer0);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.MAT4.getNumComponents(), GL15.GL_STATIC_DRAW);

		for (int a = 0; a < 4; a++) {
			GL20.glVertexAttribPointer(
					GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
					4,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
			GL20.glEnableVertexAttribArray(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
		}

		setupJointAndWeightAttribute();

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer1);
		if (skinMatrixTargetSize > 1 || parentNodeJointCount > GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize()) {
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.MAT4.getNumComponents(), GL15.GL_STATIC_DRAW);
		} else {
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(), GL15.GL_STATIC_DRAW);
		}
	}

	protected void setupSkinningFromMorphing() {
		renderedMeshPrimitiveModel.skinning = renderedMeshPrimitiveModel.new Skinning();
		renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs = new int[skinMatrixTargetSize][];

		glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glBaseAndSkinMatrixVAO = GL30Abstraction.glGenVertexArrays());
		GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glBaseAndSkinMatrixVAO);

		GL20.glVertexAttribPointer(
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getPositionAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getPositionAttribute());

		GL20.glVertexAttribPointer(
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getNormalAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getNormalAttribute());

		GL20.glVertexAttribPointer(
				GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getTangentAttribute(),
				4,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getTangentAttribute());

		glBuffers.add(renderedMeshPrimitiveModel.skinning.glSkinBuffer0 = GL15.glGenBuffers());
		glBuffers.add(renderedMeshPrimitiveModel.skinning.glSkinBuffer1 = GL15.glGenBuffers());

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer0);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.MAT4.getNumComponents(), GL15.GL_STATIC_DRAW);

		for (int a = 0; a < 4; a++) {
			GL20.glVertexAttribPointer(
					GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
					4,
					GL11.GL_FLOAT,
					false,
					Float.BYTES * ElementType.MAT4.getNumComponents(),
					(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
			GL20.glEnableVertexAttribArray(GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
		}

		setupJointAndWeightAttribute();

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer1);
		if (skinMatrixTargetSize > 1 || parentNodeJointCount > GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize()) {
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * Float.BYTES * ElementType.MAT4.getNumComponents(), GL15.GL_STATIC_DRAW);
		} else {
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) renderedMeshPrimitiveModel.count * GL21ExtGltfApplySkinMatrixPassConstants.getInstance().getSkinBufferStride(), GL15.GL_STATIC_DRAW);
		}
	}

	protected void setupMorphingBundle0Attributes(Map<String, AccessorModel> morphTarget) {
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getPositionAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferPositionOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getPositionAttribute());

		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getNormalAttribute(),
				3,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferNormalOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getNormalAttribute());

		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getTangentAttribute(),
				4,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTangentOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTangentAttribute());

		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getColorAttribute(),
				4,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferColorOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getColorAttribute());

		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordAttribute(),
				2,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordAttribute());

		AccessorModel targetAccessorModel;
		targetAccessorModel = morphTarget.get("POSITION");
		if (targetAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getPositionTargetAttribute(),
					targetAccessorModel.getElementType().getNumComponents(),
					targetAccessorModel.getComponentType(),
					false,
					targetAccessorModel.getByteStride(),
					targetAccessorModel.getByteOffset());
		}
		else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getPositionTargetAttribute(),
					3,
					GL11.GL_FLOAT,
					false,
					0,
					0);
		}
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getPositionTargetAttribute());

		targetAccessorModel = morphTarget.get("NORMAL");
		if (targetAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getNormalTargetAttribute(),
					targetAccessorModel.getElementType().getNumComponents(),
					targetAccessorModel.getComponentType(),
					false,
					targetAccessorModel.getByteStride(),
					targetAccessorModel.getByteOffset());
		}
		else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getNormalTargetAttribute(),
					3,
					GL11.GL_FLOAT,
					false,
					0,
					0);
		}
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getNormalTargetAttribute());

		targetAccessorModel = morphTarget.get("TANGENT");
		if (targetAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getTangentTargetAttribute(),
					targetAccessorModel.getElementType().getNumComponents(),
					targetAccessorModel.getComponentType(),
					false,
					targetAccessorModel.getByteStride(),
					targetAccessorModel.getByteOffset());
		}
		else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getTangentTargetAttribute(),
					4,
					GL11.GL_FLOAT,
					false,
					0,
					0);
		}
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTangentTargetAttribute());

		targetAccessorModel = morphTarget.get("COLOR_0");
		if (targetAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
					targetAccessorModel.getElementType().getNumComponents(),
					targetAccessorModel.getComponentType(),
					false,
					targetAccessorModel.getByteStride(),
					targetAccessorModel.getByteOffset());
		}
		else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
					4,
					GL11.GL_FLOAT,
					false,
					0,
					0);
		}
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute());

		targetAccessorModel = morphTarget.get("TEXCOORD_0");
		if (targetAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
					targetAccessorModel.getElementType().getNumComponents(),
					targetAccessorModel.getComponentType(),
					false,
					targetAccessorModel.getByteStride(),
					targetAccessorModel.getByteOffset());
		}
		else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
					2,
					GL11.GL_FLOAT,
					false,
					0,
					0);
		}
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute());
	}

	protected void setupMorphingBundleNAttributes(Map<String, AccessorModel> morphTarget, int b) {
		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getColorAttribute(),
				4,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferColorOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getColorAttribute());

		GL20.glVertexAttribPointer(
				GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordAttribute(),
				2,
				GL11.GL_FLOAT,
				false,
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
				GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordAttribute());

		AccessorModel targetAccessorModel;
		targetAccessorModel = morphTarget.get("COLOR_" + b);
		if (targetAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
					targetAccessorModel.getElementType().getNumComponents(),
					targetAccessorModel.getComponentType(),
					false,
					targetAccessorModel.getByteStride(),
					targetAccessorModel.getByteOffset());
		}
		else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute(),
					4,
					GL11.GL_FLOAT,
					false,
					0,
					0);
		}
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getColorTargetAttribute());

		targetAccessorModel = morphTarget.get("TEXCOORD_" + b);
		if (targetAccessorModel != null) {
			uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, targetAccessorModel.getBufferViewModel());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
					targetAccessorModel.getElementType().getNumComponents(),
					targetAccessorModel.getComponentType(),
					false,
					targetAccessorModel.getByteStride(),
					targetAccessorModel.getByteOffset());
		}
		else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
			GL20.glVertexAttribPointer(
					GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute(),
					2,
					GL11.GL_FLOAT,
					false,
					0,
					0);
		}
		GL20.glEnableVertexAttribArray(GL21ExtGltfMorphingPassConstants.getInstance().getTexcoordTargetAttribute());
	}

	protected void setupJointAndWeightAttribute() {
		if (parentNodeJointCount <= GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize()) {
			renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0] = new int[1];
			setupJoint0AndWeight0Attribute();
			renderedMeshPrimitiveModel.skinning.isSkinMatrix1First = renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs.length % 2 == 1;
			if(renderedMeshPrimitiveModel.skinning.isSkinMatrix1First) {
				for (int i = 1; i < renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs.length; i++) {
					renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i] = new int[1];
					glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0] = GL30Abstraction.glGenVertexArrays());
					GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0]);

					AccessorModel jointsAccessorModel = attributes.get("JOINTS_" + i);
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
							jointsAccessorModel.getElementType().getNumComponents(),
							jointsAccessorModel.getComponentType(),
							false,
							jointsAccessorModel.getByteStride(),
							jointsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

					AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_" + i);
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
							weightsAccessorModel.getElementType().getNumComponents(),
							weightsAccessorModel.getComponentType(),
							false,
							weightsAccessorModel.getByteStride(),
							weightsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, i % 2 == 0 ? renderedMeshPrimitiveModel.skinning.glSkinBuffer1 : renderedMeshPrimitiveModel.skinning.glSkinBuffer0);
					for (int a = 0; a < 4; a++) {
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
								4,
								GL11.GL_FLOAT,
								false,
								Float.BYTES * ElementType.MAT4.getNumComponents(),
								(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
					}
				}
			}
			else {
				for (int i = 1; i < renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs.length; i++) {
					renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i] = new int[1];
					glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0] = GL30Abstraction.glGenVertexArrays());
					GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0]);

					AccessorModel jointsAccessorModel = attributes.get("JOINTS_" + i);
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
							jointsAccessorModel.getElementType().getNumComponents(),
							jointsAccessorModel.getComponentType(),
							false,
							jointsAccessorModel.getByteStride(),
							jointsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

					AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_" + i);
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
							weightsAccessorModel.getElementType().getNumComponents(),
							weightsAccessorModel.getComponentType(),
							false,
							weightsAccessorModel.getByteStride(),
							weightsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, i % 2 == 0 ? renderedMeshPrimitiveModel.skinning.glSkinBuffer0 : renderedMeshPrimitiveModel.skinning.glSkinBuffer1);
					for (int a = 0; a < 4; a++) {
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
								4,
								GL11.GL_FLOAT,
								false,
								Float.BYTES * ElementType.MAT4.getNumComponents(),
								(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
					}
				}
			}
		}
		else {
			if(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs.length % 2 == 0) {
				renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0] = new int[2];
				setupJoint0AndWeight0Attribute();

				glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][1] = GL30Abstraction.glGenVertexArrays());
				GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][1]);

				AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
						jointsAccessorModel.getElementType().getNumComponents(),
						jointsAccessorModel.getComponentType(),
						false,
						jointsAccessorModel.getByteStride(),
						jointsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

				AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
				uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
				GL20.glVertexAttribPointer(
						GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
						weightsAccessorModel.getElementType().getNumComponents(),
						weightsAccessorModel.getComponentType(),
						false,
						weightsAccessorModel.getByteStride(),
						weightsAccessorModel.getByteOffset());
				GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer0);
				for (int a = 0; a < 4; a++) {
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
							4,
							GL11.GL_FLOAT,
							false,
							Float.BYTES * ElementType.MAT4.getNumComponents(),
							(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
				}

				for (int i = 1; i < renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs.length; i++) {
					renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i] = new int[1];
					glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0] = GL30Abstraction.glGenVertexArrays());
					GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0]);

					jointsAccessorModel = attributes.get("JOINTS_" + i);
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
							jointsAccessorModel.getElementType().getNumComponents(),
							jointsAccessorModel.getComponentType(),
							false,
							jointsAccessorModel.getByteStride(),
							jointsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

					weightsAccessorModel = attributes.get("WEIGHTS_" + i);
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
							weightsAccessorModel.getElementType().getNumComponents(),
							weightsAccessorModel.getComponentType(),
							false,
							weightsAccessorModel.getByteStride(),
							weightsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, i % 2 == 0 ? renderedMeshPrimitiveModel.skinning.glSkinBuffer0 : renderedMeshPrimitiveModel.skinning.glSkinBuffer1);
					for (int a = 0; a < 4; a++) {
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
								4,
								GL11.GL_FLOAT,
								false,
								Float.BYTES * ElementType.MAT4.getNumComponents(),
								(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
					}
				}
			}
			else {
				renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0] = new int[3];
				setupJoint0AndWeight0Attribute();
				if(parentNodeJointCount % GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize() == 0) {
					renderedMeshPrimitiveModel.skinning.isSkinMatrix1First = parentNodeJointCount / GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize() % 2 == 1;
				}
				else {
					renderedMeshPrimitiveModel.skinning.isSkinMatrix1First = parentNodeJointCount / GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getMaxDrawJointSize() % 2 == 0;
				}
				if(renderedMeshPrimitiveModel.skinning.isSkinMatrix1First) {
					glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][1] = GL30Abstraction.glGenVertexArrays());
					GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][1]);

					AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
							jointsAccessorModel.getElementType().getNumComponents(),
							jointsAccessorModel.getComponentType(),
							false,
							jointsAccessorModel.getByteStride(),
							jointsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

					AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
							weightsAccessorModel.getElementType().getNumComponents(),
							weightsAccessorModel.getComponentType(),
							false,
							weightsAccessorModel.getByteStride(),
							weightsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer1);
					for (int a = 0; a < 4; a++) {
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
								4,
								GL11.GL_FLOAT,
								false,
								Float.BYTES * ElementType.MAT4.getNumComponents(),
								(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
					}

					glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][2] = GL30Abstraction.glGenVertexArrays());
					GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][2]);

					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
							jointsAccessorModel.getElementType().getNumComponents(),
							jointsAccessorModel.getComponentType(),
							false,
							jointsAccessorModel.getByteStride(),
							jointsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
							weightsAccessorModel.getElementType().getNumComponents(),
							weightsAccessorModel.getComponentType(),
							false,
							weightsAccessorModel.getByteStride(),
							weightsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer0);
					for (int a = 0; a < 4; a++) {
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
								4,
								GL11.GL_FLOAT,
								false,
								Float.BYTES * ElementType.MAT4.getNumComponents(),
								(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
					}

					for (int i = 1; i < renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs.length; i++) {
						renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i] = new int[2];
						glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0] = GL30Abstraction.glGenVertexArrays());
						GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0]);

						jointsAccessorModel = attributes.get("JOINTS_" + i);
						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
								jointsAccessorModel.getElementType().getNumComponents(),
								jointsAccessorModel.getComponentType(),
								false,
								jointsAccessorModel.getByteStride(),
								jointsAccessorModel.getByteOffset());
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

						weightsAccessorModel = attributes.get("WEIGHTS_" + i);
						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
								weightsAccessorModel.getElementType().getNumComponents(),
								weightsAccessorModel.getComponentType(),
								false,
								weightsAccessorModel.getByteStride(),
								weightsAccessorModel.getByteOffset());
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, i % 2 == 0 ? renderedMeshPrimitiveModel.skinning.glSkinBuffer1 : renderedMeshPrimitiveModel.skinning.glSkinBuffer0);
						for (int a = 0; a < 4; a++) {
							GL20.glVertexAttribPointer(
									GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
									4,
									GL11.GL_FLOAT,
									false,
									Float.BYTES * ElementType.MAT4.getNumComponents(),
									(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
							GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
						}

						glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][1] = GL30Abstraction.glGenVertexArrays());
						GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][1]);

						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
								jointsAccessorModel.getElementType().getNumComponents(),
								jointsAccessorModel.getComponentType(),
								false,
								jointsAccessorModel.getByteStride(),
								jointsAccessorModel.getByteOffset());
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
								weightsAccessorModel.getElementType().getNumComponents(),
								weightsAccessorModel.getComponentType(),
								false,
								weightsAccessorModel.getByteStride(),
								weightsAccessorModel.getByteOffset());
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, i % 2 == 0 ? renderedMeshPrimitiveModel.skinning.glSkinBuffer0 : renderedMeshPrimitiveModel.skinning.glSkinBuffer1);
						for (int a = 0; a < 4; a++) {
							GL20.glVertexAttribPointer(
									GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
									4,
									GL11.GL_FLOAT,
									false,
									Float.BYTES * ElementType.MAT4.getNumComponents(),
									(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
							GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
						}
					}
				}
				else {
					glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][1] = GL30Abstraction.glGenVertexArrays());
					GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][1]);

					AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
							jointsAccessorModel.getElementType().getNumComponents(),
							jointsAccessorModel.getComponentType(),
							false,
							jointsAccessorModel.getByteStride(),
							jointsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

					AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
							weightsAccessorModel.getElementType().getNumComponents(),
							weightsAccessorModel.getComponentType(),
							false,
							weightsAccessorModel.getByteStride(),
							weightsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer0);
					for (int a = 0; a < 4; a++) {
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
								4,
								GL11.GL_FLOAT,
								false,
								Float.BYTES * ElementType.MAT4.getNumComponents(),
								(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
					}

					glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][2] = GL30Abstraction.glGenVertexArrays());
					GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][2]);

					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
							jointsAccessorModel.getElementType().getNumComponents(),
							jointsAccessorModel.getComponentType(),
							false,
							jointsAccessorModel.getByteStride(),
							jointsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

					uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
					GL20.glVertexAttribPointer(
							GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
							weightsAccessorModel.getElementType().getNumComponents(),
							weightsAccessorModel.getComponentType(),
							false,
							weightsAccessorModel.getByteStride(),
							weightsAccessorModel.getByteOffset());
					GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.skinning.glSkinBuffer1);
					for (int a = 0; a < 4; a++) {
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
								4,
								GL11.GL_FLOAT,
								false,
								Float.BYTES * ElementType.MAT4.getNumComponents(),
								(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
					}

					for (int i = 1; i < renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs.length; i++) {
						renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i] = new int[2];
						glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0] = GL30Abstraction.glGenVertexArrays());
						GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][0]);

						jointsAccessorModel = attributes.get("JOINTS_" + i);
						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
								jointsAccessorModel.getElementType().getNumComponents(),
								jointsAccessorModel.getComponentType(),
								false,
								jointsAccessorModel.getByteStride(),
								jointsAccessorModel.getByteOffset());
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

						weightsAccessorModel = attributes.get("WEIGHTS_" + i);
						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
								weightsAccessorModel.getElementType().getNumComponents(),
								weightsAccessorModel.getComponentType(),
								false,
								weightsAccessorModel.getByteStride(),
								weightsAccessorModel.getByteOffset());
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, i % 2 == 0 ? renderedMeshPrimitiveModel.skinning.glSkinBuffer0 : renderedMeshPrimitiveModel.skinning.glSkinBuffer1);
						for (int a = 0; a < 4; a++) {
							GL20.glVertexAttribPointer(
									GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
									4,
									GL11.GL_FLOAT,
									false,
									Float.BYTES * ElementType.MAT4.getNumComponents(),
									(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
							GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
						}

						glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][1] = GL30Abstraction.glGenVertexArrays());
						GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[i][1]);

						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
								jointsAccessorModel.getElementType().getNumComponents(),
								jointsAccessorModel.getComponentType(),
								false,
								jointsAccessorModel.getByteStride(),
								jointsAccessorModel.getByteOffset());
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

						uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
						GL20.glVertexAttribPointer(
								GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
								weightsAccessorModel.getElementType().getNumComponents(),
								weightsAccessorModel.getComponentType(),
								false,
								weightsAccessorModel.getByteStride(),
								weightsAccessorModel.getByteOffset());
						GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, i % 2 == 0 ? renderedMeshPrimitiveModel.skinning.glSkinBuffer1 : renderedMeshPrimitiveModel.skinning.glSkinBuffer0);
						for (int a = 0; a < 4; a++) {
							GL20.glVertexAttribPointer(
									GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
									4,
									GL11.GL_FLOAT,
									false,
									Float.BYTES * ElementType.MAT4.getNumComponents(),
									(long) Float.BYTES * ElementType.VEC4.getNumComponents() * a);
							GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
						}
					}
				}
			}
		}
	}

	protected void setupJoint0AndWeight0Attribute() {
		glVertexArrays.add(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][0] = GL30Abstraction.glGenVertexArrays());
		GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.skinning.glTargetAndSkinMatrixVAOs[0][0]);

		AccessorModel jointsAccessorModel = attributes.get("JOINTS_0");
		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, jointsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute(),
				jointsAccessorModel.getElementType().getNumComponents(),
				jointsAccessorModel.getComponentType(),
				false,
				jointsAccessorModel.getByteStride(),
				jointsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getJointAttribute());

		AccessorModel weightsAccessorModel = attributes.get("WEIGHTS_0");
		uploadAndBindBuffer(GL15.GL_ARRAY_BUFFER, weightsAccessorModel.getBufferViewModel());
		GL20.glVertexAttribPointer(
				GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute(),
				weightsAccessorModel.getElementType().getNumComponents(),
				weightsAccessorModel.getComponentType(),
				false,
				weightsAccessorModel.getByteStride(),
				weightsAccessorModel.getByteOffset());
		GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getWeightAttribute());

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL21ExtGltfMorphingPassConstants.getInstance().getGlZeroVec4Buffer());
		for (int a = 0; a < 4; a++) {
			GL20.glVertexAttribPointer(
					GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a,
					4,
					GL11.GL_FLOAT,
					false,
					0,
					0);
			GL20.glEnableVertexAttribArray(GL21ExtGltfCalcSkinMatrixPassConstants.getInstance().getSkinMatrixAttribute() + a);
		}
	}

	protected void setupRenderPass() {
		GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

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
		GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

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

		setupColorAttributeFromMorphing();
		setupTexcoordAttributeFromMorphing();

		setupGlDraw();
	}

	protected void setupRenderPassFromSkinning() {
		GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

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

		setupColorAttribute();
		setupTexcoordAttribute();

		setupGlDraw();
	}

	protected void setupRenderPassFromMorphingAndSkinning() {
		GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);

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
					GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
					GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferColorOffset());
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
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[baseColorTexcoord].glMorphBuffer0);
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getColorTextureIndex());
				GL11.glTexCoordPointer(
						2,
						GL11.GL_FLOAT,
						GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
						GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
				if (emissiveTexcoord == null) emissiveTexcoord = 0;
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[emissiveTexcoord].glMorphBuffer0);
				GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
				GL11.glTexCoordPointer(
						2,
						GL11.GL_FLOAT,
						GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
						GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
				GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
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
				}
				Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
				if (emissiveTexcoord != null) {
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderedMeshPrimitiveModel.morphing.attributeBundles[emissiveTexcoord].glMorphBuffer0);
					GL13.glClientActiveTexture(VanillaRenderConstants.getInstance().getEmissiveTextureIndex());
					GL11.glTexCoordPointer(
							2,
							GL11.GL_FLOAT,
							GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferStride(),
							GL21ExtGltfMorphingPassConstants.getInstance().getMorphBufferTexcoordOffset());
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

			GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = this.renderedMeshPrimitiveModel;
			renderedMeshPrimitiveModel.glDraw = () -> {
				GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
				GL11.glDrawElements(mode, indiceCount, type, offset);
			};
		} else {
			int mode = meshPrimitiveModel.getMode();

			GL21ExtRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = this.renderedMeshPrimitiveModel;
			renderedMeshPrimitiveModel.glDraw = () -> {
				GL30Abstraction.glBindVertexArray(renderedMeshPrimitiveModel.glRenderVAO);
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

	protected void uploadAndBindBuffer(int target, BufferViewModel bufferViewModel) {
		Integer glBuffer = glBufferLookup.get(bufferViewModel);
		if (glBuffer == null) {
			glBuffers.add(glBuffer = GL15.glGenBuffers());
			GL15.glBindBuffer(target, glBuffer);
			GL15.glBufferData(target, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			glBufferLookup.put(bufferViewModel, glBuffer);
		} else GL15.glBindBuffer(target, glBuffer);
	}
}
