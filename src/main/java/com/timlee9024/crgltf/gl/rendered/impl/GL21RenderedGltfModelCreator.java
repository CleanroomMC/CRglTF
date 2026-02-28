package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class GL21RenderedGltfModelCreator {

	public int jointMatricesLength;
	public int calculatedJointMatricesLength;

	public DefaultRenderedMaterialModelCreator renderedMaterialModelCreator;
	public GL21RenderedNodeModelCreator renderedNodeModelCreator;
	public GL21RenderedSceneModelCreator renderedSceneModelCreator;

	protected List<NodeModel> nodeModels;

	protected Map<NodeModel, CommonNodeAccessor> nodeAccessorLookup;
	protected Map<NodeModel, GL21RenderedNodeModel> renderedNodeModelLookup;
	protected Map<AccessorModel, Matrix4f[]> inverseBindMatricesLookup;
	protected List<GL21RenderedNodeModel> renderedNodeModelTree;
	protected GL21RenderedGltfModel renderedGltfModel;

	public GL21RenderedGltfModel create(GltfModel gltfModel) {
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.glBufferLookup = new IdentityHashMap<>(gltfModel.getBufferViewModels().size());

		List<MaterialModel> materialModels = gltfModel.getMaterialModels();
		renderedMaterialModelCreator.combinedRenderedTextureModelCreator.glTextureLookup = new IdentityHashMap<>(gltfModel.getTextureModels().size());
		Map<MaterialModel, DefaultRenderedMaterialModel> renderedMaterialModelLookup = new IdentityHashMap<>(materialModels.size());
		GltfMaterialToTextureConstants.getInstance().setupCanvasQuad();
		for (MaterialModel materialModel : materialModels) {
			renderedMaterialModelLookup.put(materialModel, renderedMaterialModelCreator.create(materialModel));
		}
		renderedMaterialModelCreator.combinedRenderedTextureModelCreator.deleteGlTextureLookup();
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.renderedMaterialModelLookup = renderedMaterialModelLookup;

		renderedNodeModelCreator.renderedMeshModelLookup = new IdentityHashMap<>(gltfModel.getMeshModels().size());

		jointMatricesLength = 0;
		calculatedJointMatricesLength = 0;
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.allZeroWeightsLength = 0;
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.dynamicFloatBufferSize = 0;

		nodeModels = gltfModel.getNodeModels();
		CommonNodeAccessor[] nodeAccessors = new CommonNodeAccessor[nodeModels.size()];

		nodeAccessorLookup = new IdentityHashMap<>(nodeModels.size());
		renderedNodeModelLookup = new IdentityHashMap<>(nodeModels.size());

		for (int i = 0; i < nodeModels.size(); i++) {
			nodeAccessors[i] = createNodeAccessor(nodeModels.get(i));
		}

		inverseBindMatricesLookup = new IdentityHashMap<>(gltfModel.getAccessorModels().size());
		Map<NodeModel, Map.Entry<CommonNodeAccessor, List<GL21RenderedNodeModel>>> rootNodeLookup = createRootNodeLookup();
		List<SceneModel> sceneModels = gltfModel.getSceneModels();
		GL21RenderedSceneModel[] renderedSceneModels = new GL21RenderedSceneModel[sceneModels.size()];
		for (int i = 0; i < sceneModels.size(); i++) {
			SceneModel sceneModel = sceneModels.get(i);
			GL21RenderedSceneModel renderedSceneModel = renderedSceneModels[i] = renderedSceneModelCreator.create(sceneModel);
			List<GL21RenderedNodeModel> renderedNodeModels = new ArrayList<>(nodeModels.size());
			for (NodeModel nodeModel : sceneModel.getNodeModels()) {
				renderedNodeModels.addAll(rootNodeLookup.get(nodeModel).getValue());
			}
			renderedSceneModel.renderedNodeModels = renderedNodeModels.toArray(new GL21RenderedNodeModel[0]);
		}

		renderedGltfModel = new GL21RenderedGltfModel();
		renderedGltfModel.renderedSceneModels = renderedSceneModels;
		renderedGltfModel.nodeAccessors = nodeAccessors;
		renderedGltfModel.rootNodeAccessors = new CommonNodeAccessor[rootNodeLookup.size()];
		int i = 0;
		for (Map.Entry<CommonNodeAccessor, List<GL21RenderedNodeModel>> entry : rootNodeLookup.values()) {
			renderedGltfModel.rootNodeAccessors[i++] = entry.getKey();
		}

		initSharedRenderData();

		return renderedGltfModel;
	}

	protected CommonNodeAccessor createNodeAccessor(NodeModel nodeModel) {
		CommonNodeAccessor nodeAccessor;
		GL21RenderedNodeModel renderedNodeModel = renderedNodeModelCreator.create(nodeModel);
		if (renderedNodeModel != null) {
			renderedNodeModelLookup.put(nodeModel, renderedNodeModel);
			nodeAccessor = renderedNodeModel;
		} else nodeAccessor = new CommonNodeAccessor();

		nodeAccessorLookup.put(nodeModel, nodeAccessor);

		float[] m = nodeModel.getMatrix();
		if (m != null) {
			nodeAccessor.setTransformMatrix(nodeAccessor.originalTransformMatrix = new Matrix4f().set(m));
		} else {
			float[] t = nodeModel.getTranslation();
			if (t != null) nodeAccessor.originalTranslation = new Vector3f(nodeAccessor.getTranslation().set(t));
			float[] r = nodeModel.getRotation();
			if (r != null)
				nodeAccessor.originalRotation = new Quaternionf(nodeAccessor.getRotation().set(r[0], r[1], r[2], r[3]));
			float[] s = nodeModel.getScale();
			if (s != null) nodeAccessor.originalScale = new Vector3f(nodeAccessor.getScale().set(s));
		}
		return nodeAccessor;
	}

	protected Map<NodeModel, Map.Entry<CommonNodeAccessor, List<GL21RenderedNodeModel>>> createRootNodeLookup() {
		Map<NodeModel, Map.Entry<CommonNodeAccessor, List<GL21RenderedNodeModel>>> rootNodeLookup = new IdentityHashMap<>(nodeModels.size());
		for (NodeModel nodeModel : nodeModels) {
			if (nodeModel.getParent() == null) {
				CommonNodeAccessor nodeAccessor = nodeAccessorLookup.get(nodeModel);
				nodeAccessor.initGlobalTransform();
				renderedNodeModelTree = new ArrayList<>(nodeModels.size());
				rootNodeLookup.put(nodeModel, new AbstractMap.SimpleEntry<>(nodeAccessor, renderedNodeModelTree));

				configureNodeSkin(nodeModel);

				List<NodeModel> childNodeModels = nodeModel.getChildren();
				nodeAccessor.children = new CommonNodeAccessor[childNodeModels.size()];
				for (int i = 0; i < childNodeModels.size(); i++) {
					NodeModel childNodeModel = childNodeModels.get(i);
					nodeAccessor.children[i] = resolveChildNode(childNodeModel, nodeAccessor);
				}
			}
		}
		return rootNodeLookup;
	}

	protected CommonNodeAccessor resolveChildNode(NodeModel nodeModel, CommonNodeAccessor parentNodeAccessor) {
		CommonNodeAccessor nodeAccessor = nodeAccessorLookup.get(nodeModel);
		nodeAccessor.initGlobalTransform(parentNodeAccessor.getOriginalGlobalTransformMatrix());

		configureNodeSkin(nodeModel);

		List<NodeModel> childNodeModels = nodeModel.getChildren();
		nodeAccessor.children = new CommonNodeAccessor[childNodeModels.size()];
		for (int i = 0; i < childNodeModels.size(); i++) {
			NodeModel childNodeModel = childNodeModels.get(i);
			nodeAccessor.children[i] = resolveChildNode(childNodeModel, nodeAccessor);
		}
		return nodeAccessor;
	}

	protected void configureNodeSkin(NodeModel nodeModel) {
		GL21RenderedNodeModel renderedNodeModel = renderedNodeModelLookup.get(nodeModel);
		if (renderedNodeModel != null) {
			renderedNodeModelTree.add(renderedNodeModel);
			SkinModel skinModel = nodeModel.getSkinModel();
			if (skinModel != null) {
				AccessorModel inverseBindMatrixAccessorModel = skinModel.getInverseBindMatrices();
				if (inverseBindMatrixAccessorModel != null) {
					Matrix4f[] inverseBindMatrices = inverseBindMatricesLookup.get(inverseBindMatrixAccessorModel);
					if (inverseBindMatrices == null) {
						AccessorFloatData accessorFloatData = (AccessorFloatData) inverseBindMatrixAccessorModel.getAccessorData();
						inverseBindMatrices = new Matrix4f[accessorFloatData.getNumElements()];
						for (int i = 0; i < inverseBindMatrices.length; i++) {
							inverseBindMatrices[i] = new Matrix4f(
									accessorFloatData.get(i, 0), accessorFloatData.get(i, 1), accessorFloatData.get(i, 2), accessorFloatData.get(i, 3),
									accessorFloatData.get(i, 4), accessorFloatData.get(i, 5), accessorFloatData.get(i, 6), accessorFloatData.get(i, 7),
									accessorFloatData.get(i, 8), accessorFloatData.get(i, 9), accessorFloatData.get(i, 10), accessorFloatData.get(i, 11),
									accessorFloatData.get(i, 12), accessorFloatData.get(i, 13), accessorFloatData.get(i, 14), accessorFloatData.get(i, 15)
							);
						}
						if (inverseBindMatrices.length > calculatedJointMatricesLength)
							calculatedJointMatricesLength = inverseBindMatrices.length;
					}
					renderedNodeModel.inverseBindMatrices = inverseBindMatrices;
				}

				List<NodeModel> joints = skinModel.getJoints();
				int jointCount = joints.size();
				if (jointCount > jointMatricesLength) jointMatricesLength = jointCount;
				renderedNodeModel.jointNodeAccessors = new NodeAccessor[jointCount];
				for (int i = 0; i < jointCount; i++) {
					renderedNodeModel.jointNodeAccessors[i] = nodeAccessorLookup.get(joints.get(i));
				}
			}
		}
	}

	protected void initSharedRenderData() {
		if (jointMatricesLength > 0) {
			Matrix4f[] jointMatrices = new Matrix4f[jointMatricesLength];
			if (calculatedJointMatricesLength > 0) {
				Matrix4f[] calculatedJointMatrices = new Matrix4f[calculatedJointMatricesLength];
				for (int i = 0; i < calculatedJointMatrices.length; i++) {
					calculatedJointMatrices[i] = new Matrix4f();
				}
				for (GL21RenderedNodeModel renderedNodeModel : renderedNodeModelLookup.values()) {
					renderedNodeModel.calculatedJointMatrices = calculatedJointMatrices;
					renderedNodeModel.jointMatrices = jointMatrices;
				}
			} else {
				for (GL21RenderedNodeModel renderedNodeModel : renderedNodeModelLookup.values()) {
					renderedNodeModel.jointMatrices = jointMatrices;
				}
			}

			renderedGltfModel.dynamicFloatBuffer = MemoryUtil.memAllocFloat(renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.dynamicFloatBufferSize);
			int glDynamicBuffer = GL15.glGenBuffers();
			renderedGltfModel.glBufferViews.add(glDynamicBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glDynamicBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedGltfModel.dynamicFloatBuffer, GL15.GL_STATIC_DRAW);

			if (renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.allZeroWeightsLength > 0) {
				float[] allZeroWeights = new float[renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.allZeroWeightsLength];
				for (GL21RenderedMeshModel renderedMeshModel : renderedNodeModelCreator.renderedMeshModelLookup.values()) {
					renderedMeshModel.allZeroWeights = allZeroWeights;
					for (GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
						renderedMeshPrimitiveModel.glDynamicBuffer = glDynamicBuffer;
						renderedMeshPrimitiveModel.dynamicFloatBuffer = renderedGltfModel.dynamicFloatBuffer;
						renderedMeshPrimitiveModel.jointMatrices = jointMatrices;
					}
				}
			} else {
				for (GL21RenderedMeshModel renderedMeshModel : renderedNodeModelCreator.renderedMeshModelLookup.values()) {
					for (GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
						renderedMeshPrimitiveModel.glDynamicBuffer = glDynamicBuffer;
						renderedMeshPrimitiveModel.dynamicFloatBuffer = renderedGltfModel.dynamicFloatBuffer;
						renderedMeshPrimitiveModel.jointMatrices = jointMatrices;
					}
				}
			}
		} else {
			if (renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.allZeroWeightsLength > 0) {
				float[] allZeroWeights = new float[renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.allZeroWeightsLength];
				for (GL21RenderedMeshModel renderedMeshModel : renderedNodeModelCreator.renderedMeshModelLookup.values()) {
					renderedMeshModel.allZeroWeights = allZeroWeights;
				}
			} else if (renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.dynamicFloatBufferSize == 0) {
				return;
			}

			renderedGltfModel.dynamicFloatBuffer = MemoryUtil.memAllocFloat(renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.dynamicFloatBufferSize);
			int glDynamicBuffer = GL15.glGenBuffers();
			renderedGltfModel.glBufferViews.add(glDynamicBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glDynamicBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, renderedGltfModel.dynamicFloatBuffer, GL15.GL_STATIC_DRAW);

			for (GL21RenderedMeshModel renderedMeshModel : renderedNodeModelCreator.renderedMeshModelLookup.values()) {
				for (GL21RenderedMeshPrimitiveModel renderedMeshPrimitiveModel : renderedMeshModel.renderedMeshPrimitiveModels) {
					renderedMeshPrimitiveModel.glDynamicBuffer = glDynamicBuffer;
					renderedMeshPrimitiveModel.dynamicFloatBuffer = renderedGltfModel.dynamicFloatBuffer;
				}
			}
		}
	}
}
