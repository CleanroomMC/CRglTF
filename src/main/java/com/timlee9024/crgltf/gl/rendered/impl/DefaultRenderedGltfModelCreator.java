package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.gl.constants.GltfCalcJointMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.ElementType;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class DefaultRenderedGltfModelCreator {

	public DefaultRenderedMaterialModelCreator renderedMaterialModelCreator;
	public DefaultRenderedNodeModelCreator renderedNodeModelCreator;
	public DefaultRenderedSceneModelCreator renderedSceneModelCreator;

	public OpenGLObjectRefSet glBuffers;
	public OpenGLObjectRefSet glVertexArrays;

	protected List<NodeModel> nodeModels;

	protected Map<BufferViewModel, Integer> glBufferLookup;
	protected Map<NodeModel, CommonNodeAccessor> nodeAccessorLookup;
	protected Map<NodeModel, DefaultRenderedNodeModel> renderedNodeModelLookup;
	protected Map<SkinModel, DefaultNodeSkin> nodeSkinLookup;
	protected List<DefaultRenderedNodeModel> renderedNodeModelTree;

	public DefaultRenderedGltfModel create(GltfModel gltfModel) {
		glBufferLookup = new IdentityHashMap<>(gltfModel.getBufferViewModels().size());
		renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.glBufferLookup = glBufferLookup;

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

		nodeModels = gltfModel.getNodeModels();
		CommonNodeAccessor[] nodeAccessors = new CommonNodeAccessor[nodeModels.size()];

		nodeAccessorLookup = new IdentityHashMap<>(nodeModels.size());
		renderedNodeModelLookup = new IdentityHashMap<>(nodeModels.size());

		for (int i = 0; i < nodeModels.size(); i++) {
			nodeAccessors[i] = createNodeAccessor(nodeModels.get(i));
		}

		List<SkinModel> skinModels = gltfModel.getSkinModels();
		nodeSkinLookup = new IdentityHashMap<>(skinModels.size());
		for (SkinModel skinModel : skinModels) {
			nodeSkinLookup.put(skinModel, createNodeSkin(skinModel));
		}

		Map<NodeModel, Map.Entry<CommonNodeAccessor, List<DefaultRenderedNodeModel>>> rootNodeLookup = createRootNodeLookup();
		List<SceneModel> sceneModels = gltfModel.getSceneModels();
		DefaultRenderedSceneModel[] renderedSceneModels = new DefaultRenderedSceneModel[sceneModels.size()];
		for (int i = 0; i < sceneModels.size(); i++) {
			SceneModel sceneModel = sceneModels.get(i);
			DefaultRenderedSceneModel renderedSceneModel = renderedSceneModels[i] = renderedSceneModelCreator.create(sceneModel);
			List<DefaultRenderedNodeModel> renderedNodeModels = new ArrayList<>(nodeModels.size());
			for (NodeModel nodeModel : sceneModel.getNodeModels()) {
				renderedNodeModels.addAll(rootNodeLookup.get(nodeModel).getValue());
			}
			renderedSceneModel.renderedNodeModels = renderedNodeModels.toArray(new DefaultRenderedNodeModel[0]);
			checkMorphingAndSkinning(renderedSceneModel);
		}

		DefaultRenderedGltfModel renderedGltfModel = new DefaultRenderedGltfModel();
		renderedGltfModel.glTextures = renderedMaterialModelCreator.combinedRenderedTextureModelCreator.glTextures;
		renderedGltfModel.glBufferViews = glBuffers;
		renderedGltfModel.glVertexArrays = glVertexArrays;
		renderedGltfModel.renderedSceneModels = renderedSceneModels;
		renderedGltfModel.nodeAccessors = nodeAccessors;
		renderedGltfModel.rootNodeAccessors = new CommonNodeAccessor[rootNodeLookup.size()];
		int i = 0;
		for (Map.Entry<CommonNodeAccessor, List<DefaultRenderedNodeModel>> entry : rootNodeLookup.values()) {
			renderedGltfModel.rootNodeAccessors[i++] = entry.getKey();
		}
		return renderedGltfModel;
	}

	protected CommonNodeAccessor createNodeAccessor(NodeModel nodeModel) {
		CommonNodeAccessor nodeAccessor;
		DefaultRenderedNodeModel renderedNodeModel = renderedNodeModelCreator.create(nodeModel);
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

	protected DefaultNodeSkin createNodeSkin(SkinModel skinModel) {
		DefaultNodeSkin nodeSkin;
		AccessorModel inverseBindMatrixAccessorModel = skinModel.getInverseBindMatrices();
		if (inverseBindMatrixAccessorModel != null) {
			DefaultNodeSkinWithInverseBindMatrices nodeSkinWithInverseBindMatrices = new DefaultNodeSkinWithInverseBindMatrices();
			glVertexArrays.add(nodeSkinWithInverseBindMatrices.glInverseBindMatrixVAO = GL30.glGenVertexArrays());
			GL30.glBindVertexArray(nodeSkinWithInverseBindMatrices.glInverseBindMatrixVAO);

			uploadAndBindArrayBuffer(inverseBindMatrixAccessorModel.getBufferViewModel());
			for (int i = 0; i < 4; i++) {
				GL20.glVertexAttribPointer(
						GltfCalcJointMatrixPassConstants.getInstance().getInverseBindMatrixAttribute() + i,
						4,
						inverseBindMatrixAccessorModel.getComponentType(),
						false,
						inverseBindMatrixAccessorModel.getByteStride(),
						inverseBindMatrixAccessorModel.getByteOffset() + 16 * i);
				GL20.glEnableVertexAttribArray(GltfCalcJointMatrixPassConstants.getInstance().getInverseBindMatrixAttribute() + i);
			}
			nodeSkin = nodeSkinWithInverseBindMatrices;
		} else nodeSkin = new DefaultNodeSkin();

		List<NodeModel> joints = skinModel.getJoints();
		int jointCount = joints.size();
		nodeSkin.jointNodeAccessors = new NodeAccessor[jointCount];
		glBuffers.add(nodeSkin.glJointMatrixBuffer = GL15.glGenBuffers());
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, nodeSkin.glJointMatrixBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) jointCount * ElementType.MAT4.getNumComponents() * Float.BYTES, GL15.GL_STATIC_DRAW);
		for (int i = 0; i < jointCount; i++) {
			nodeSkin.jointNodeAccessors[i] = nodeAccessorLookup.get(joints.get(i));
		}
		return nodeSkin;
	}

	protected Map<NodeModel, Map.Entry<CommonNodeAccessor, List<DefaultRenderedNodeModel>>> createRootNodeLookup() {
		Map<NodeModel, Map.Entry<CommonNodeAccessor, List<DefaultRenderedNodeModel>>> rootNodeLookup = new IdentityHashMap<>(nodeModels.size());
		for (NodeModel nodeModel : nodeModels) {
			if (nodeModel.getParent() == null) {
				CommonNodeAccessor nodeAccessor = nodeAccessorLookup.get(nodeModel);
				nodeAccessor.initGlobalTransform();
				renderedNodeModelTree = new ArrayList<>(nodeModels.size());
				rootNodeLookup.put(nodeModel, new AbstractMap.SimpleEntry<>(nodeAccessor, renderedNodeModelTree));

				DefaultRenderedNodeModel renderedNodeModel = renderedNodeModelLookup.get(nodeModel);
				if (renderedNodeModel != null) {
					renderedNodeModelTree.add(renderedNodeModel);
					SkinModel skinModel = nodeModel.getSkinModel();
					if (skinModel != null) renderedNodeModel.nodeSkin = nodeSkinLookup.get(skinModel);
					else renderedNodeModel.nodeSkin = DefaultNodeSkin.DUMMY;
				}

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

		DefaultRenderedNodeModel renderedNodeModel = renderedNodeModelLookup.get(nodeModel);
		if (renderedNodeModel != null) {
			renderedNodeModelTree.add(renderedNodeModel);
			SkinModel skinModel = nodeModel.getSkinModel();
			if (skinModel != null) renderedNodeModel.nodeSkin = nodeSkinLookup.get(skinModel);
			else renderedNodeModel.nodeSkin = DefaultNodeSkin.DUMMY;
		}

		List<NodeModel> childNodeModels = nodeModel.getChildren();
		nodeAccessor.children = new CommonNodeAccessor[childNodeModels.size()];
		for (int i = 0; i < childNodeModels.size(); i++) {
			NodeModel childNodeModel = childNodeModels.get(i);
			nodeAccessor.children[i] = resolveChildNode(childNodeModel, nodeAccessor);
		}
		return nodeAccessor;
	}

	protected void uploadAndBindArrayBuffer(BufferViewModel bufferViewModel) {
		Integer glBuffer = glBufferLookup.get(bufferViewModel);
		if (glBuffer == null) {
			glBuffer = GL15.glGenBuffers();
			glBuffers.add(glBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBuffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferViewModel.getBufferViewData(), GL15.GL_STATIC_DRAW);
			glBufferLookup.put(bufferViewModel, glBuffer);
		} else GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBuffer);
	}

	protected void checkMorphingAndSkinning(DefaultRenderedSceneModel renderedSceneModel) {
		for (DefaultRenderedNodeModel renderedNodeModel : renderedSceneModel.renderedNodeModels) {
			if (renderedNodeModel.morphing != DefaultRenderedNodeModel.Morphing.DUMMY) {
				renderedSceneModel.hasMorphing = true;
				break;
			}
		}
		List<DefaultNodeSkin> nodeSkins = new ArrayList<>(nodeSkinLookup.size());
		for (DefaultRenderedNodeModel renderedNodeModel : renderedSceneModel.renderedNodeModels) {
			if (renderedNodeModel.nodeSkin instanceof DefaultNodeSkinWithInverseBindMatrices) {
				renderedSceneModel.hasInverseBindMatrices = true;
				if(nodeSkins.stream().noneMatch(n -> n == renderedNodeModel.nodeSkin)) nodeSkins.add(renderedNodeModel.nodeSkin);
			} else if (renderedNodeModel.nodeSkin != DefaultNodeSkin.DUMMY) {
				if(nodeSkins.stream().noneMatch(n -> n == renderedNodeModel.nodeSkin)) nodeSkins.add(renderedNodeModel.nodeSkin);
			}
		}
		if(!nodeSkins.isEmpty()) renderedSceneModel.nodeSkins = nodeSkins.toArray(new DefaultNodeSkin[0]);
	}

}
