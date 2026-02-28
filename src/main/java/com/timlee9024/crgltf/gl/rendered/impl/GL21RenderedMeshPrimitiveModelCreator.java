package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.util.AccessorDataFloatGetter;
import com.timlee9024.crgltf.util.AccessorDataIntGetter;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GL21RenderedMeshPrimitiveModelCreator {

	public int allZeroWeightsLength;
	public int dynamicFloatBufferSize;

	public OpenGLObjectRefSet glBuffers;

	public Map<BufferViewModel, Integer> glBufferLookup;
	public Map<MaterialModel, DefaultRenderedMaterialModel> renderedMaterialModelLookup;

	protected MeshPrimitiveModel meshPrimitiveModel;
	protected Map<String, AccessorModel> attributes;
	protected List<Map<String, AccessorModel>> morphTargets;

	protected int skinMatrixTargetSize;
	protected int attributeOffest;

	protected AccessorModel positionsAccessorModel;
	protected int[] positionTargetTable;
	protected int positionTargetTableSize;
	protected AccessorFloatData[] positionTargetAccessorDatas;
	protected Runnable[] positionAttributeUpdates;

	protected AccessorModel normalsAccessorModel;
	protected int[] normalTargetTable;
	protected int normalTargetTableSize;
	protected AccessorFloatData[] normalTargetAccessorDatas;
	protected Runnable[] normalAttributeUpdates;

	protected AccessorModel tangentsAccessorModel;
	protected int[] tangentTargetTable;
	protected int tangentTargetTableSize;
	protected AccessorFloatData[] tangentTargetAccessorDatas;
	protected Runnable[] tangentAttributeUpdates;

	public static class AttributeBundle {
		public AccessorModel accessorModel;
		public int[] targetTable;
		public int targetTableSize;
		public AccessorDataFloatGetter[] targetAccessorDatas;
		protected Runnable[] attributeUpdates;
	}

	protected final List<AttributeBundle> colorAttributeBundles = new ArrayList<>();
	protected final List<AttributeBundle> texcoordAttributeBundles = new ArrayList<>();

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

			attributeOffest = 0;
			skinMatrixTargetSize = 0;
			while (attributes.containsKey("JOINTS_" + skinMatrixTargetSize) && attributes.containsKey("WEIGHTS_" + skinMatrixTargetSize))
				++skinMatrixTargetSize;

			positionAttributeUpdates = null;
			normalAttributeUpdates = null;
			tangentAttributeUpdates = null;
			initOptionalAttributesAndMorphTargets();

			GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel = new GL21RenderedMeshPrimitiveModel();
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
				} else {
					int mode = meshPrimitiveModel.getMode();
					int count = positionsAccessorModel.getCount();

					renderedMeshPrimitiveModel.glDraw = () -> {
						renderedMeshPrimitiveModel.updateMorphingAndSkinning();
						renderedMeshPrimitiveModel.bindDynamicAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
						renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
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
					} else {
						renderedMeshPrimitiveModel.glDraw = () -> {
							renderedMeshPrimitiveModel.updateMorphing();
							renderedMeshPrimitiveModel.bindDynamicAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
							renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
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
					} else {
						renderedMeshPrimitiveModel.glDraw = () -> {
							renderedMeshPrimitiveModel.updateMorphing();
							renderedMeshPrimitiveModel.bindDynamicAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
							renderedMeshPrimitiveModel.bindStaticAttributes(colorIndex, texcoordIndex, emissiveTexcoordIndex);
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
				renderedMeshPrimitiveModel.renderedMaterialModel = DefaultRenderedMaterialModel.DEFAULT;
			if (attributeOffest > dynamicFloatBufferSize) dynamicFloatBufferSize = attributeOffest;
			return renderedMeshPrimitiveModel;
		}
		return null;
	}

	protected void initOptionalAttributesAndMorphTargets() {
		morphTargets = meshPrimitiveModel.getTargets();
		int morphTargetSize = morphTargets.size();
		if (morphTargetSize > allZeroWeightsLength) allZeroWeightsLength = morphTargetSize;

		positionTargetTable = new int[morphTargetSize];
		normalTargetTable = new int[morphTargetSize];
		tangentTargetTable = new int[morphTargetSize];

		positionTargetAccessorDatas = new AccessorFloatData[morphTargetSize];
		normalTargetAccessorDatas = new AccessorFloatData[morphTargetSize];
		tangentTargetAccessorDatas = new AccessorFloatData[morphTargetSize];

		positionTargetTableSize = 0;
		normalTargetTableSize = 0;
		tangentTargetTableSize = 0;

		colorAttributeBundles.clear();
		int index;
		for (AccessorModel accessorModel = attributes.get("COLOR_" + (index = 0)); accessorModel != null; accessorModel = attributes.get("COLOR_" + (++index))) {
			AttributeBundle attributeBundle = new AttributeBundle();
			attributeBundle.accessorModel = accessorModel;
			attributeBundle.targetTable = new int[morphTargetSize];
			attributeBundle.targetAccessorDatas = new AccessorDataFloatGetter[morphTargetSize];
			colorAttributeBundles.add(attributeBundle);
		}

		texcoordAttributeBundles.clear();
		for (AccessorModel accessorModel = attributes.get("TEXCOORD_" + (index = 0)); accessorModel != null; accessorModel = attributes.get("TEXCOORD_" + (++index))) {
			AttributeBundle attributeBundle = new AttributeBundle();
			attributeBundle.accessorModel = accessorModel;
			attributeBundle.targetTable = new int[morphTargetSize];
			attributeBundle.targetAccessorDatas = new AccessorDataFloatGetter[morphTargetSize];
			texcoordAttributeBundles.add(attributeBundle);
		}

		for (int i = 0; i < morphTargetSize; i++) {
			Map<String, AccessorModel> morphTarget = morphTargets.get(i);
			AccessorModel targetAccessorModel = morphTarget.get("POSITION");
			if (targetAccessorModel != null) {
				positionTargetAccessorDatas[positionTargetTableSize] = (AccessorFloatData) targetAccessorModel.getAccessorData();
				positionTargetTable[positionTargetTableSize++] = i;
			}
			targetAccessorModel = morphTarget.get("NORMAL");
			if (targetAccessorModel != null) {
				normalTargetAccessorDatas[normalTargetTableSize] = (AccessorFloatData) targetAccessorModel.getAccessorData();
				normalTargetTable[normalTargetTableSize++] = i;
			}
			targetAccessorModel = morphTarget.get("TANGENT");
			if (targetAccessorModel != null) {
				tangentTargetAccessorDatas[tangentTargetTableSize] = (AccessorFloatData) targetAccessorModel.getAccessorData();
				tangentTargetTable[tangentTargetTableSize++] = i;
			}
			for (int b = 0; b < colorAttributeBundles.size(); b++) {
				targetAccessorModel = morphTarget.get("COLOR_" + b);
				if (targetAccessorModel != null) {
					AttributeBundle attributeBundle = colorAttributeBundles.get(b);
					attributeBundle.targetAccessorDatas[attributeBundle.targetTableSize] = AccessorDataFloatGetter.createDequantized(targetAccessorModel.getAccessorData());
					attributeBundle.targetTable[attributeBundle.targetTableSize++] = i;
				}
			}
			for (int b = 0; b < texcoordAttributeBundles.size(); b++) {
				targetAccessorModel = morphTarget.get("TEXCOORD_" + b);
				if (targetAccessorModel != null) {
					AttributeBundle attributeBundle = texcoordAttributeBundles.get(b);
					attributeBundle.targetAccessorDatas[attributeBundle.targetTableSize] = AccessorDataFloatGetter.createDequantized(targetAccessorModel.getAccessorData());
					attributeBundle.targetTable[attributeBundle.targetTableSize++] = i;
				}
			}
		}
	}

	protected void processJointAndWeight(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		AccessorDataIntGetter[] jointAccessorDatas = new AccessorDataIntGetter[skinMatrixTargetSize];
		AccessorDataFloatGetter[] weightAccessorDatas = new AccessorDataFloatGetter[skinMatrixTargetSize];
		int[] numComponentsPerElements = new int[skinMatrixTargetSize];
		int bonePerVertex = 0;
		for (int i = 0; i < skinMatrixTargetSize; i++) {
			AccessorData accessorData = attributes.get("JOINTS_" + i).getAccessorData();
			bonePerVertex += numComponentsPerElements[i] = accessorData.getNumComponentsPerElement();
			jointAccessorDatas[i] = AccessorDataIntGetter.createDequantized(accessorData);
			weightAccessorDatas[i] = AccessorDataFloatGetter.createDequantized(attributes.get("WEIGHTS_" + i).getAccessorData());
		}
		for (int e = 0; e < renderedMeshPrimitiveModel.skinMatrices.length; e++) {
			Matrix4f skinMatrix = renderedMeshPrimitiveModel.skinMatrices[e];
			List<Runnable> jointAndWeightBundle = new ArrayList<>(bonePerVertex);
			for (int i = 0; i < jointAccessorDatas.length; i++) {
				AccessorDataIntGetter jointAccessorData = jointAccessorDatas[i];
				AccessorDataFloatGetter weightAccessorData = weightAccessorDatas[i];
				for (int c = 0; c < numComponentsPerElements[i]; c++) {
					float weight = weightAccessorData.getFloat(e, c);
					if (weight != 0) {
						int joint = jointAccessorData.getInt(e, c);
						jointAndWeightBundle.add(() -> {
							Matrix4fc jointMatrix = renderedMeshPrimitiveModel.jointMatrices[joint];
							skinMatrix.m00(Math.fma(jointMatrix.m00(), weight, skinMatrix.m00()));
							skinMatrix.m01(Math.fma(jointMatrix.m01(), weight, skinMatrix.m01()));
							skinMatrix.m02(Math.fma(jointMatrix.m02(), weight, skinMatrix.m02()));
							skinMatrix.m03(Math.fma(jointMatrix.m03(), weight, skinMatrix.m03()));
							skinMatrix.m10(Math.fma(jointMatrix.m10(), weight, skinMatrix.m10()));
							skinMatrix.m11(Math.fma(jointMatrix.m11(), weight, skinMatrix.m11()));
							skinMatrix.m12(Math.fma(jointMatrix.m12(), weight, skinMatrix.m12()));
							skinMatrix.m13(Math.fma(jointMatrix.m13(), weight, skinMatrix.m13()));
							skinMatrix.m20(Math.fma(jointMatrix.m20(), weight, skinMatrix.m20()));
							skinMatrix.m21(Math.fma(jointMatrix.m21(), weight, skinMatrix.m21()));
							skinMatrix.m22(Math.fma(jointMatrix.m22(), weight, skinMatrix.m22()));
							skinMatrix.m23(Math.fma(jointMatrix.m23(), weight, skinMatrix.m23()));
							skinMatrix.m30(Math.fma(jointMatrix.m30(), weight, skinMatrix.m30()));
							skinMatrix.m31(Math.fma(jointMatrix.m31(), weight, skinMatrix.m31()));
							skinMatrix.m32(Math.fma(jointMatrix.m32(), weight, skinMatrix.m32()));
							skinMatrix.m33(Math.fma(jointMatrix.m33(), weight, skinMatrix.m33()));
						});
					}
				}
			}
			if (!jointAndWeightBundle.isEmpty()) {
				Runnable[] jointAndWeights = jointAndWeightBundle.toArray(new Runnable[0]);
				renderedMeshPrimitiveModel.skinMatrixCalculations.add(() -> {
					skinMatrix.zero();
					for (Runnable jointAndWeight : jointAndWeights) {
						jointAndWeight.run();
					}
				});
			}
		}
	}

	protected void processPositionAttribute(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		renderedMeshPrimitiveModel.positionComponentNum = positionsAccessorModel.getElementType().getNumComponents();
		renderedMeshPrimitiveModel.positionComponentType = positionsAccessorModel.getComponentType();

		if (positionTargetTableSize > 0) {
			int[] table = Arrays.copyOf(positionTargetTable, positionTargetTableSize);
			AccessorFloatData baseAccessorData = (AccessorFloatData) positionsAccessorModel.getAccessorData();
			int totalNumComponents = baseAccessorData.getTotalNumComponents();
			positionAttributeUpdates = new Runnable[totalNumComponents];
			int numElements = baseAccessorData.getNumElements();
			int numComponent = baseAccessorData.getNumComponentsPerElement();
			renderedMeshPrimitiveModel.positionByteOffset = attributeOffest * 4L;
			for (int e = 0; e < numElements; e++) {
				int elementIndex = e * numComponent;
				for (int c = 0; c < numComponent; c++) {
					float base = baseAccessorData.get(e, c);
					float[] targets = new float[positionTargetTableSize];
					for (int i = 0; i < positionTargetTableSize; i++) {
						targets[i] = positionTargetAccessorDatas[i].get(e, c);
					}
					int componentIndex = elementIndex + c;
					int absoluteIndex = attributeOffest + componentIndex;
					positionAttributeUpdates[componentIndex] = () -> {
						float result = base;
						for (int i = 0; i < targets.length; i++) {
							result = Math.fma(targets[i], renderedMeshPrimitiveModel.weights[table[i]], result);
						}
						renderedMeshPrimitiveModel.dynamicFloatBuffer.put(absoluteIndex, result);
					};
				}
			}
			attributeOffest += totalNumComponents;
		} else {
			renderedMeshPrimitiveModel.glStaticPositionBuffer = uploadAndObtainGLBuffer(positionsAccessorModel.getBufferViewModel());
			renderedMeshPrimitiveModel.positionByteStride = positionsAccessorModel.getByteStride();
			renderedMeshPrimitiveModel.positionByteOffset = positionsAccessorModel.getByteOffset();
		}
	}

	protected void processPositionSkinningAttribute(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		renderedMeshPrimitiveModel.positionComponentNum = positionsAccessorModel.getElementType().getNumComponents();
		renderedMeshPrimitiveModel.positionComponentType = positionsAccessorModel.getComponentType();

		AccessorFloatData baseAccessorData = (AccessorFloatData) positionsAccessorModel.getAccessorData();
		int numElements = baseAccessorData.getNumElements();
		positionAttributeUpdates = new Runnable[numElements];

		renderedMeshPrimitiveModel.skinMatrices = new Matrix4f[numElements];

		renderedMeshPrimitiveModel.positionByteOffset = attributeOffest * 4L;
		if (positionTargetTableSize > 0) {
			int[] table = Arrays.copyOf(positionTargetTable, positionTargetTableSize);
			for (int e = 0; e < numElements; e++) {
				float x = baseAccessorData.get(e, 0);
				float y = baseAccessorData.get(e, 1);
				float z = baseAccessorData.get(e, 2);
				float[] targetXs = new float[positionTargetTableSize];
				float[] targetYs = new float[positionTargetTableSize];
				float[] targetZs = new float[positionTargetTableSize];
				for (int i = 0; i < positionTargetTableSize; i++) {
					AccessorFloatData targetAccessorData = positionTargetAccessorDatas[i];
					targetXs[i] = targetAccessorData.get(e, 0);
					targetYs[i] = targetAccessorData.get(e, 1);
					targetZs[i] = targetAccessorData.get(e, 2);
				}
				int absoluteIndex = attributeOffest + e * 3;
				Matrix4f skinMatrix = renderedMeshPrimitiveModel.skinMatrices[e] = new Matrix4f();
				Vector3f vector3f = new Vector3f();
				positionAttributeUpdates[e] = () -> {
					vector3f.x = x;
					vector3f.y = y;
					vector3f.z = z;
					for (int i = 0; i < targetXs.length; i++) {
						float weight = renderedMeshPrimitiveModel.weights[table[i]];
						vector3f.x = Math.fma(targetXs[i], weight, vector3f.x);
						vector3f.y = Math.fma(targetYs[i], weight, vector3f.y);
						vector3f.z = Math.fma(targetZs[i], weight, vector3f.z);
					}
					vector3f.mulPosition(skinMatrix).get(absoluteIndex, renderedMeshPrimitiveModel.dynamicFloatBuffer);
				};
			}
		} else {
			for (int e = 0; e < numElements; e++) {
				float x = baseAccessorData.get(e, 0);
				float y = baseAccessorData.get(e, 1);
				float z = baseAccessorData.get(e, 2);
				int absoluteIndex = attributeOffest + e * 3;
				Matrix4f skinMatrix = renderedMeshPrimitiveModel.skinMatrices[e] = new Matrix4f();
				Vector3f vector3f = new Vector3f();
				positionAttributeUpdates[e] = () -> {
					vector3f.x = x;
					vector3f.y = y;
					vector3f.z = z;
					vector3f.mulPosition(skinMatrix).get(absoluteIndex, renderedMeshPrimitiveModel.dynamicFloatBuffer);
				};
			}
		}
		attributeOffest += baseAccessorData.getTotalNumComponents();
		renderedMeshPrimitiveModel.skinMatrixCalculations = new ArrayList<>(numElements);
	}

	protected void processNormalAttribute(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		renderedMeshPrimitiveModel.normalComponentType = normalsAccessorModel.getComponentType();

		if (normalTargetTableSize > 0) {
			int[] table = Arrays.copyOf(normalTargetTable, normalTargetTableSize);
			AccessorFloatData baseAccessorData = (AccessorFloatData) normalsAccessorModel.getAccessorData();
			int totalNumComponents = baseAccessorData.getTotalNumComponents();
			normalAttributeUpdates = new Runnable[totalNumComponents];
			int numElements = baseAccessorData.getNumElements();
			int numComponent = baseAccessorData.getNumComponentsPerElement();
			renderedMeshPrimitiveModel.normalByteOffset = attributeOffest * 4L;
			for (int e = 0; e < numElements; e++) {
				int elementIndex = e * numComponent;
				for (int c = 0; c < numComponent; c++) {
					float base = baseAccessorData.get(e, c);
					float[] targets = new float[normalTargetTableSize];
					for (int i = 0; i < normalTargetTableSize; i++) {
						targets[i] = normalTargetAccessorDatas[i].get(e, c);
					}
					int componentIndex = elementIndex + c;
					int absoluteIndex = attributeOffest + componentIndex;
					normalAttributeUpdates[componentIndex] = () -> {
						float result = base;
						for (int i = 0; i < targets.length; i++) {
							result = Math.fma(targets[i], renderedMeshPrimitiveModel.weights[table[i]], result);
						}
						renderedMeshPrimitiveModel.dynamicFloatBuffer.put(absoluteIndex, result);
					};
				}
			}
			attributeOffest += totalNumComponents;
		} else {
			renderedMeshPrimitiveModel.glStaticNormalBuffer = uploadAndObtainGLBuffer(normalsAccessorModel.getBufferViewModel());
			renderedMeshPrimitiveModel.normalByteStride = normalsAccessorModel.getByteStride();
			renderedMeshPrimitiveModel.normalByteOffset = normalsAccessorModel.getByteOffset();
		}
	}

	protected void processNormalSkinningAttribute(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		renderedMeshPrimitiveModel.normalComponentType = normalsAccessorModel.getComponentType();

		AccessorFloatData baseAccessorData = (AccessorFloatData) normalsAccessorModel.getAccessorData();
		int numElements = baseAccessorData.getNumElements();
		normalAttributeUpdates = new Runnable[numElements];
		renderedMeshPrimitiveModel.normalByteOffset = attributeOffest * 4L;
		if (normalTargetTableSize > 0) {
			int[] table = Arrays.copyOf(normalTargetTable, normalTargetTableSize);
			for (int e = 0; e < numElements; e++) {
				float x = baseAccessorData.get(e, 0);
				float y = baseAccessorData.get(e, 1);
				float z = baseAccessorData.get(e, 2);
				float[] targetXs = new float[normalTargetTableSize];
				float[] targetYs = new float[normalTargetTableSize];
				float[] targetZs = new float[normalTargetTableSize];
				for (int i = 0; i < normalTargetTableSize; i++) {
					AccessorFloatData targetAccessorData = normalTargetAccessorDatas[i];
					targetXs[i] = targetAccessorData.get(e, 0);
					targetYs[i] = targetAccessorData.get(e, 1);
					targetZs[i] = targetAccessorData.get(e, 2);
				}
				int absoluteIndex = attributeOffest + e * 3;
				Matrix4f skinMatrix = renderedMeshPrimitiveModel.skinMatrices[e];
				Vector3f vector3f = new Vector3f();
				normalAttributeUpdates[e] = () -> {
					vector3f.x = x;
					vector3f.y = y;
					vector3f.z = z;
					for (int i = 0; i < targetXs.length; i++) {
						float weight = renderedMeshPrimitiveModel.weights[table[i]];
						vector3f.x = Math.fma(targetXs[i], weight, vector3f.x);
						vector3f.y = Math.fma(targetYs[i], weight, vector3f.y);
						vector3f.z = Math.fma(targetZs[i], weight, vector3f.z);
					}
					vector3f.mulDirection(skinMatrix).get(absoluteIndex, renderedMeshPrimitiveModel.dynamicFloatBuffer);
				};
			}
		} else {
			for (int e = 0; e < numElements; e++) {
				float x = baseAccessorData.get(e, 0);
				float y = baseAccessorData.get(e, 1);
				float z = baseAccessorData.get(e, 2);
				int absoluteIndex = attributeOffest + e * 3;
				Matrix4f skinMatrix = renderedMeshPrimitiveModel.skinMatrices[e];
				Vector3f vector3f = new Vector3f();
				normalAttributeUpdates[e] = () -> {
					vector3f.x = x;
					vector3f.y = y;
					vector3f.z = z;
					vector3f.mulDirection(skinMatrix).get(absoluteIndex, renderedMeshPrimitiveModel.dynamicFloatBuffer);
				};
			}
		}
		attributeOffest += baseAccessorData.getTotalNumComponents();
	}

	protected void processTangentAttribute(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		renderedMeshPrimitiveModel.tangentComponentNum = tangentsAccessorModel.getElementType().getNumComponents();
		renderedMeshPrimitiveModel.tangentComponentType = tangentsAccessorModel.getComponentType();

		if (tangentTargetTableSize > 0) {
			int[] table = Arrays.copyOf(tangentTargetTable, tangentTargetTableSize);
			AccessorFloatData baseAccessorData = (AccessorFloatData) tangentsAccessorModel.getAccessorData();
			int totalNumComponents = baseAccessorData.getTotalNumComponents();
			tangentAttributeUpdates = new Runnable[totalNumComponents];
			int numElements = baseAccessorData.getNumElements();
			int numComponent = baseAccessorData.getNumComponentsPerElement();
			renderedMeshPrimitiveModel.tangentByteOffset = attributeOffest * 4L;
			for (int e = 0; e < numElements; e++) {
				int elementIndex = e * numComponent;
				for (int c = 0; c < 3; c++) {
					float base = baseAccessorData.get(e, c);
					float[] targets = new float[tangentTargetTableSize];
					for (int i = 0; i < tangentTargetTableSize; i++) {
						targets[i] = tangentTargetAccessorDatas[i].get(e, c);
					}
					int componentIndex = elementIndex + c;
					int absoluteIndex = attributeOffest + componentIndex;
					tangentAttributeUpdates[componentIndex] = () -> {
						float result = base;
						for (int i = 0; i < targets.length; i++) {
							result = Math.fma(targets[i], renderedMeshPrimitiveModel.weights[table[i]], result);
						}
						renderedMeshPrimitiveModel.dynamicFloatBuffer.put(absoluteIndex, result);
					};
				}
				float sign = baseAccessorData.get(e, 3);
				int componentIndex = elementIndex + 3;
				int absoluteIndex = attributeOffest + componentIndex;
				renderedMeshPrimitiveModel.attributeUpdates.add(() -> renderedMeshPrimitiveModel.dynamicFloatBuffer.put(absoluteIndex, sign));
			}
			attributeOffest += totalNumComponents;
		} else {
			renderedMeshPrimitiveModel.glStaticTangentBuffer = uploadAndObtainGLBuffer(tangentsAccessorModel.getBufferViewModel());
			renderedMeshPrimitiveModel.tangentByteStride = tangentsAccessorModel.getByteStride();
			renderedMeshPrimitiveModel.tangentByteOffset = tangentsAccessorModel.getByteOffset();
		}
	}

	protected void processTangentSkinningAttribute(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		renderedMeshPrimitiveModel.tangentComponentNum = tangentsAccessorModel.getElementType().getNumComponents();
		renderedMeshPrimitiveModel.tangentComponentType = tangentsAccessorModel.getComponentType();

		AccessorFloatData baseAccessorData = (AccessorFloatData) tangentsAccessorModel.getAccessorData();
		int numElements = baseAccessorData.getNumElements();
		tangentAttributeUpdates = new Runnable[numElements];
		renderedMeshPrimitiveModel.tangentByteOffset = attributeOffest * 4L;
		if (tangentTargetTableSize > 0) {
			int[] table = Arrays.copyOf(tangentTargetTable, tangentTargetTableSize);
			for (int e = 0; e < numElements; e++) {
				float x = baseAccessorData.get(e, 0);
				float y = baseAccessorData.get(e, 1);
				float z = baseAccessorData.get(e, 2);
				float sign = baseAccessorData.get(e, 3);
				float[] targetXs = new float[tangentTargetTableSize];
				float[] targetYs = new float[tangentTargetTableSize];
				float[] targetZs = new float[tangentTargetTableSize];
				for (int i = 0; i < tangentTargetTableSize; i++) {
					AccessorFloatData targetAccessorData = tangentTargetAccessorDatas[i];
					targetXs[i] = targetAccessorData.get(e, 0);
					targetYs[i] = targetAccessorData.get(e, 1);
					targetZs[i] = targetAccessorData.get(e, 2);
				}
				int absoluteIndex = attributeOffest + e * 4;
				int signIndex = absoluteIndex + 3;
				Matrix4f skinMatrix = renderedMeshPrimitiveModel.skinMatrices[e];
				Vector3f vector3f = new Vector3f();
				tangentAttributeUpdates[e] = () -> {
					vector3f.x = x;
					vector3f.y = y;
					vector3f.z = z;
					for (int i = 0; i < targetXs.length; i++) {
						float weight = renderedMeshPrimitiveModel.weights[table[i]];
						vector3f.x = Math.fma(targetXs[i], weight, vector3f.x);
						vector3f.y = Math.fma(targetYs[i], weight, vector3f.y);
						vector3f.z = Math.fma(targetZs[i], weight, vector3f.z);
					}
					vector3f.mulDirection(skinMatrix).get(absoluteIndex, renderedMeshPrimitiveModel.dynamicFloatBuffer);
					renderedMeshPrimitiveModel.dynamicFloatBuffer.put(signIndex, sign);
				};
			}
		} else {
			for (int e = 0; e < numElements; e++) {
				float x = baseAccessorData.get(e, 0);
				float y = baseAccessorData.get(e, 1);
				float z = baseAccessorData.get(e, 2);
				float sign = baseAccessorData.get(e, 3);
				int absoluteIndex = attributeOffest + e * 4;
				int signIndex = absoluteIndex + 3;
				Matrix4f skinMatrix = renderedMeshPrimitiveModel.skinMatrices[e];
				Vector3f vector3f = new Vector3f();
				tangentAttributeUpdates[e] = () -> {
					vector3f.x = x;
					vector3f.y = y;
					vector3f.z = z;
					vector3f.mulDirection(skinMatrix).get(absoluteIndex, renderedMeshPrimitiveModel.dynamicFloatBuffer);
					renderedMeshPrimitiveModel.dynamicFloatBuffer.put(signIndex, sign);
				};
			}
		}
		attributeOffest += baseAccessorData.getTotalNumComponents();
	}

	protected void processColorAttributes(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		renderedMeshPrimitiveModel.colorAttributes = new GL21RenderedMeshPrimitiveModel.AttributeBundle[colorAttributeBundles.size()];
		for (int b = 0; b < colorAttributeBundles.size(); b++) {
			processAttributeBundles(renderedMeshPrimitiveModel, colorAttributeBundles.get(b), renderedMeshPrimitiveModel.colorAttributes[b] = new GL21RenderedMeshPrimitiveModel.AttributeBundle());
		}
	}

	protected void processTexcoordAttributes(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		renderedMeshPrimitiveModel.texcoordAttributes = new GL21RenderedMeshPrimitiveModel.AttributeBundle[texcoordAttributeBundles.size()];
		for (int b = 0; b < texcoordAttributeBundles.size(); b++) {
			processAttributeBundles(renderedMeshPrimitiveModel, texcoordAttributeBundles.get(b), renderedMeshPrimitiveModel.texcoordAttributes[b] = new GL21RenderedMeshPrimitiveModel.AttributeBundle());
		}
	}

	protected void processAttributeBundles(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel, AttributeBundle creatorAttributeBundle, GL21RenderedMeshPrimitiveModel.AttributeBundle modelAttributeBundle) {
		modelAttributeBundle.componentNum = creatorAttributeBundle.accessorModel.getElementType().getNumComponents();
		modelAttributeBundle.componentType = creatorAttributeBundle.accessorModel.getComponentType();
		modelAttributeBundle.byteStride = creatorAttributeBundle.accessorModel.getByteStride();

		if (creatorAttributeBundle.targetTableSize > 0) {
			int[] table = Arrays.copyOf(creatorAttributeBundle.targetTable, creatorAttributeBundle.targetTableSize);
			AccessorData baseAccessorData = creatorAttributeBundle.accessorModel.getAccessorData();
			int totalNumComponents = baseAccessorData.getTotalNumComponents();
			creatorAttributeBundle.attributeUpdates = new Runnable[totalNumComponents];
			int numElements = baseAccessorData.getNumElements();
			int numComponent = baseAccessorData.getNumComponentsPerElement();
			modelAttributeBundle.byteOffset = attributeOffest * 4L;
			AccessorDataFloatGetter accessorDataFloatGetter = AccessorDataFloatGetter.createDequantized(baseAccessorData);
			for (int e = 0; e < numElements; e++) {
				int elementIndex = e * numComponent;
				for (int c = 0; c < numComponent; c++) {
					float base = accessorDataFloatGetter.getFloat(e, c);
					float[] targets = new float[creatorAttributeBundle.targetTableSize];
					for (int i = 0; i < creatorAttributeBundle.targetTableSize; i++) {
						targets[i] = creatorAttributeBundle.targetAccessorDatas[i].getFloat(e, c);
					}
					int componentIndex = elementIndex + c;
					int absoluteIndex = attributeOffest + componentIndex;
					creatorAttributeBundle.attributeUpdates[componentIndex] = () -> {
						float result = base;
						for (int i = 0; i < targets.length; i++) {
							result = Math.fma(targets[i], renderedMeshPrimitiveModel.weights[table[i]], result);
						}
						renderedMeshPrimitiveModel.dynamicFloatBuffer.put(absoluteIndex, result);
					};
				}
			}
			attributeOffest += totalNumComponents;
		} else {
			modelAttributeBundle.glStaticBuffer = uploadAndObtainGLBuffer(creatorAttributeBundle.accessorModel.getBufferViewModel());
			modelAttributeBundle.byteOffset = creatorAttributeBundle.accessorModel.getByteOffset();
		}
	}

	protected void initAttributeUpdates(GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel) {
		if (attributeOffest > 0) {
			Runnable[] runnables = null;
			if (positionAttributeUpdates != null) {
				runnables = positionAttributeUpdates;
			}
			if (normalAttributeUpdates != null) {
				if (runnables != null) {
					runnables = Arrays.copyOf(runnables, runnables.length + normalAttributeUpdates.length);
					System.arraycopy(normalAttributeUpdates, 0, runnables, runnables.length - normalAttributeUpdates.length, normalAttributeUpdates.length);
				} else runnables = normalAttributeUpdates;
			}
			if (tangentAttributeUpdates != null) {
				if (runnables != null) {
					runnables = Arrays.copyOf(runnables, runnables.length + tangentAttributeUpdates.length);
					System.arraycopy(tangentAttributeUpdates, 0, runnables, runnables.length - tangentAttributeUpdates.length, tangentAttributeUpdates.length);
				} else runnables = tangentAttributeUpdates;
			}
			for (AttributeBundle attributeBundle : colorAttributeBundles) {
				if (attributeBundle.attributeUpdates != null) {
					if (runnables != null) {
						runnables = Arrays.copyOf(runnables, runnables.length + attributeBundle.attributeUpdates.length);
						System.arraycopy(attributeBundle.attributeUpdates, 0, runnables, runnables.length - attributeBundle.attributeUpdates.length, attributeBundle.attributeUpdates.length);
					} else runnables = attributeBundle.attributeUpdates;
				}
			}
			for (AttributeBundle attributeBundle : texcoordAttributeBundles) {
				if (attributeBundle.attributeUpdates != null) {
					if (runnables != null) {
						runnables = Arrays.copyOf(runnables, runnables.length + attributeBundle.attributeUpdates.length);
						System.arraycopy(attributeBundle.attributeUpdates, 0, runnables, runnables.length - attributeBundle.attributeUpdates.length, attributeBundle.attributeUpdates.length);
					} else runnables = attributeBundle.attributeUpdates;
				}
			}
			if (runnables != null) {
				renderedMeshPrimitiveModel.attributeUpdates = Arrays.asList(runnables);
			}
		}
	}

	protected int uploadAndObtainGLBuffer(BufferViewModel bufferViewModel) {
		Integer glBuffer = glBufferLookup.get(bufferViewModel);
		if (glBuffer == null) {
			glBuffer = GL15.glGenBuffers();
			glBuffers.add(glBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			glBufferLookup.put(bufferViewModel, glBuffer);
		}
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
