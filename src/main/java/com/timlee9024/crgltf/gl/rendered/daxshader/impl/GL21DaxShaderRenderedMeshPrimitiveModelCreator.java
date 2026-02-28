package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshPrimitiveModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshPrimitiveModelCreator;
import com.timlee9024.crgltf.property.GltfMaterialExtra;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.Map;

public class GL21DaxShaderRenderedMeshPrimitiveModelCreator extends GL21RenderedMeshPrimitiveModelCreator {

	public Map<MaterialModel, GltfMaterialExtra> gltfMaterialExtraLookup;

	@Override
	public GL21RenderedMeshPrimitiveModel create(MeshPrimitiveModel meshPrimitiveModel) {
		this.meshPrimitiveModel = meshPrimitiveModel;
		attributes = meshPrimitiveModel.getAttributes();
		positionsAccessorModel = attributes.get("POSITION");
		if (positionsAccessorModel != null) {
			normalsAccessorModel = attributes.get("NORMAL");
			tangentsAccessorModel = attributes.get("TANGENT");

			int texcoordIndex, emissiveTexcoordIndex;
			MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
			if (materialModel instanceof MaterialModelV2 materialModelV2) {
				if (attributes.containsKey("TEXCOORD_0")) {
					Integer baseColorTexcoord = materialModelV2.getBaseColorTexcoord();
					if (baseColorTexcoord != null) texcoordIndex = baseColorTexcoord;
					else texcoordIndex = 0;
					Integer emissiveTexcoord = materialModelV2.getEmissiveTexcoord();
					if (emissiveTexcoord != null) emissiveTexcoordIndex = emissiveTexcoord;
					else emissiveTexcoordIndex = 0;
				} else {
					texcoordIndex = -1;
					emissiveTexcoordIndex = -1;
				}
			} else {
				texcoordIndex = -1;
				emissiveTexcoordIndex = -1;
			}
			int colorIndex;
			if (attributes.containsKey("COLOR_0")) colorIndex = 0;
			else colorIndex = -1;

			int mcMidTexcoordIndex;
			GltfMaterialExtra gltfMaterialExtra = gltfMaterialExtraLookup.get(materialModel);
			if (gltfMaterialExtra != null && gltfMaterialExtra.crgltf != null && gltfMaterialExtra.crgltf.daxShader != null && gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord != null)
				mcMidTexcoordIndex = gltfMaterialExtra.crgltf.daxShader.mc_midTexCoord;
			else mcMidTexcoordIndex = texcoordIndex;

			attributeOffest = 0;
			skinMatrixTargetSize = 0;
			while (attributes.containsKey("JOINTS_" + skinMatrixTargetSize) && attributes.containsKey("WEIGHTS_" + skinMatrixTargetSize))
				++skinMatrixTargetSize;

			positionAttributeUpdates = null;
			normalAttributeUpdates = null;
			tangentAttributeUpdates = null;
			initOptionalAttributesAndMorphTargets();

			GL21DaxShaderRenderedMeshPrimitiveModel renderedMeshPrimitiveModel = new GL21DaxShaderRenderedMeshPrimitiveModel();
			if (skinMatrixTargetSize > 0) {
				AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
				if (indicesAccessorModel != null) {
					int indiceCount = indicesAccessorModel.getCount();
					int mode = meshPrimitiveModel.getMode();
					int type = indicesAccessorModel.getComponentType();
					int offset = indicesAccessorModel.getByteOffset();
					int glBuffer = uploadAndObtainElementArrayBuffer(indicesAccessorModel.getBufferViewModel());

					renderedMeshPrimitiveModel.glDraw = () -> {
						renderedMeshPrimitiveModel.updateMorphingAndSkinning();
						renderedMeshPrimitiveModel.bindDynamicAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
						renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
						GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
						GL11.glDrawElements(mode, indiceCount, type, offset);
					};
					renderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
						renderedMeshPrimitiveModel.updateMorphingAndSkinning();
						renderedMeshPrimitiveModel.bindDynamicAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
						renderedMeshPrimitiveModel.bindStaticAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
						GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
						GL11.glDrawElements(mode, indiceCount, type, offset);
					};
				} else {
					int mode = meshPrimitiveModel.getMode();
					int count = positionsAccessorModel.getCount();

					renderedMeshPrimitiveModel.glDraw = () -> {
						renderedMeshPrimitiveModel.updateMorphingAndSkinning();
						renderedMeshPrimitiveModel.bindDynamicAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
						renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
						GL11.glDrawArrays(mode, 0, count);
					};
					renderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
						renderedMeshPrimitiveModel.updateMorphingAndSkinning();
						renderedMeshPrimitiveModel.bindDynamicAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
						renderedMeshPrimitiveModel.bindStaticAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
						GL11.glDrawArrays(mode, 0, count);
					};
				}

				processPositionSkinningAttribute(renderedMeshPrimitiveModel);
				processNormalSkinningAttribute(renderedMeshPrimitiveModel);
				processTangentSkinningAttribute(renderedMeshPrimitiveModel);

				processJointAndWeight(renderedMeshPrimitiveModel); //Must process before processPositionSkinningAttribute()
			} else {
				AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
				if (indicesAccessorModel != null) {
					int indiceCount = indicesAccessorModel.getCount();
					int mode = meshPrimitiveModel.getMode();
					int type = indicesAccessorModel.getComponentType();
					int offset = indicesAccessorModel.getByteOffset();
					int glBuffer = uploadAndObtainElementArrayBuffer(indicesAccessorModel.getBufferViewModel());

					if (morphTargets.isEmpty()) {
						renderedMeshPrimitiveModel.glDraw = () -> {
							renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
							GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
							GL11.glDrawElements(mode, indiceCount, type, offset);
						};
						renderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
							renderedMeshPrimitiveModel.bindStaticAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
							GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
							GL11.glDrawElements(mode, indiceCount, type, offset);
						};
					} else {
						renderedMeshPrimitiveModel.glDraw = () -> {
							renderedMeshPrimitiveModel.updateMorphing();
							renderedMeshPrimitiveModel.bindDynamicAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
							renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
							GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
							GL11.glDrawElements(mode, indiceCount, type, offset);
						};
						renderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
							renderedMeshPrimitiveModel.updateMorphing();
							renderedMeshPrimitiveModel.bindDynamicAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
							renderedMeshPrimitiveModel.bindStaticAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
							GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer);
							GL11.glDrawElements(mode, indiceCount, type, offset);
						};
					}
				} else {
					int mode = meshPrimitiveModel.getMode();
					int count = positionsAccessorModel.getCount();

					if (morphTargets.isEmpty()) {
						renderedMeshPrimitiveModel.glDraw = () -> {
							renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
							GL11.glDrawArrays(mode, 0, count);
						};
						renderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
							renderedMeshPrimitiveModel.bindStaticAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
							GL11.glDrawArrays(mode, 0, count);
						};
					} else {
						renderedMeshPrimitiveModel.glDraw = () -> {
							renderedMeshPrimitiveModel.updateMorphing();
							renderedMeshPrimitiveModel.bindDynamicAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
							renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
							GL11.glDrawArrays(mode, 0, count);
						};
						renderedMeshPrimitiveModel.glDaxShaderDraw = () -> {
							renderedMeshPrimitiveModel.updateMorphing();
							renderedMeshPrimitiveModel.bindDynamicAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
							renderedMeshPrimitiveModel.bindStaticAttributesForDaxShader(colorIndex, texcoordIndex, mcMidTexcoordIndex);
							GL11.glDrawArrays(mode, 0, count);
						};
					}
				}

				processPositionAttribute(renderedMeshPrimitiveModel);
				processNormalAttribute(renderedMeshPrimitiveModel);
				processTangentAttribute(renderedMeshPrimitiveModel);
			}

			processColorAttributes(renderedMeshPrimitiveModel);
			processTexcoordAttributes(renderedMeshPrimitiveModel);

			initAttributeUpdates(renderedMeshPrimitiveModel);

			renderedMeshPrimitiveModel.renderedMaterialModel = renderedMaterialModelLookup.get(meshPrimitiveModel.getMaterialModel());
			if (renderedMeshPrimitiveModel.renderedMaterialModel == null)
				renderedMeshPrimitiveModel.renderedMaterialModel = DefaultDaxShaderRenderedMaterialModel.DEFAULT;
			renderedMeshPrimitiveModel.daxShaderRenderedMaterialModel = (DefaultDaxShaderRenderedMaterialModel) renderedMeshPrimitiveModel.renderedMaterialModel;

			if (attributeOffest > dynamicFloatBufferSize) dynamicFloatBufferSize = attributeOffest;
			return renderedMeshPrimitiveModel;
		}
		return null;
	}
}
