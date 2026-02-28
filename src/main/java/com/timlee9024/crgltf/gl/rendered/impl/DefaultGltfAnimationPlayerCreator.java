package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.animation.AnimationChannel;
import com.timlee9024.crgltf.animation.impl.KeyframeAnimationChannel;
import com.timlee9024.crgltf.animation.impl.MorphWeightsCubicSplineChannel;
import com.timlee9024.crgltf.animation.impl.MorphWeightsLinearChannel;
import com.timlee9024.crgltf.animation.impl.MorphWeightsStepChannel;
import com.timlee9024.crgltf.animation.impl.QuaternionfCubicSplineChannel;
import com.timlee9024.crgltf.animation.impl.QuaternionfLinearChannel;
import com.timlee9024.crgltf.animation.impl.QuaternionfStepChannel;
import com.timlee9024.crgltf.animation.impl.Vector3fCubicSplineChannel;
import com.timlee9024.crgltf.animation.impl.Vector3fLinearChannel;
import com.timlee9024.crgltf.animation.impl.Vector3fStepChannel;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import com.timlee9024.crgltf.gl.rendered.RenderedGltfModel;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.AnimationModel.Channel;
import de.javagl.jgltf.model.AnimationModel.Sampler;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.NodeModel;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class DefaultGltfAnimationPlayerCreator {

	public DefaultGltfAnimationPlayer create(GltfModel gltfModel, RenderedGltfModel renderedGltfModel) {
		List<NodeModel> nodeModels = gltfModel.getNodeModels();
		List<AnimationModel> animationModels = gltfModel.getAnimationModels();
		DefaultGltfAnimationPlayer gltfAnimationPlayer = new DefaultGltfAnimationPlayer();
		gltfAnimationPlayer.animations = new AnimationChannel[animationModels.size()][];
		for (int i = 0; i < animationModels.size(); i++) {
			List<Channel> channels = animationModels.get(i).getChannels();
			AnimationChannel[] animationChannels = gltfAnimationPlayer.animations[i] = new AnimationChannel[channels.size()];
			for (int j = 0; j < channels.size(); j++) {
				Channel channel = channels.get(j);
				int nodeIndex = nodeModels.indexOf(channel.getNodeModel());
				Sampler sampler = channel.getSampler();

				AccessorFloatData inputFloatData = (AccessorFloatData) sampler.getInput().getAccessorData();
				int numKeyElements = inputFloatData.getNumElements();
				float[] keys = new float[numKeyElements];
				for (int e = 0; e < numKeyElements; e++) {
					keys[e] = inputFloatData.get(e, 0);
				}

				NodeAccessor nodeAccessor = renderedGltfModel.getNodeAccessorByNode(nodeIndex);
				switch (channel.getPath()) {
					case "translation":
						KeyframeAnimationChannel<Vector3f> vector3fChannel;
						switch (sampler.getInterpolation()) {
							case STEP:
								vector3fChannel = new Vector3fStepChannel(keys, outputDataToVector3fFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							case LINEAR:
								vector3fChannel = new Vector3fLinearChannel(keys, outputDataToVector3fFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							case CUBICSPLINE:
								vector3fChannel = new Vector3fCubicSplineChannel(keys, outputDataToVector3fPointFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							default:
								return null; //TODO: Throw exception.
						}
						vector3fChannel.setTarget(nodeAccessor.getTranslation());
						animationChannels[j] = vector3fChannel;
						break;
					case "rotation":
						KeyframeAnimationChannel<Quaternionf> quaternionfChannel;
						switch (sampler.getInterpolation()) {
							case STEP:
								quaternionfChannel = new QuaternionfStepChannel(keys, outputDataToQuaternionfFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							case LINEAR:
								quaternionfChannel = new QuaternionfLinearChannel(keys, outputDataToQuaternionfFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							case CUBICSPLINE:
								quaternionfChannel = new QuaternionfCubicSplineChannel(keys, outputDataToVector4fPointFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							default:
								return null; //TODO: Throw exception.
						}
						quaternionfChannel.setTarget(nodeAccessor.getRotation());
						animationChannels[j] = quaternionfChannel;
						break;
					case "scale":
						switch (sampler.getInterpolation()) {
							case STEP:
								vector3fChannel = new Vector3fStepChannel(keys, outputDataToVector3fFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							case LINEAR:
								vector3fChannel = new Vector3fLinearChannel(keys, outputDataToVector3fFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							case CUBICSPLINE:
								vector3fChannel = new Vector3fCubicSplineChannel(keys, outputDataToVector3fPointFrame(sampler)) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setTRSModified(true);
									}

								};
								break;
							default:
								return null; //TODO: Throw exception.
						}
						vector3fChannel.setTarget(nodeAccessor.getScale());
						animationChannels[j] = vector3fChannel;
						break;
					case "weights":
						KeyframeAnimationChannel<float[]> morphWeightsChannel;
						int numComponentsPerElement;
						switch (sampler.getInterpolation()) {
							case STEP:
								AccessorFloatData outputFloatData = (AccessorFloatData) sampler.getOutput().getAccessorData();
								int numElements = outputFloatData.getNumElements();
								numComponentsPerElement = numElements / numKeyElements;
								float[][] outputs = new float[numKeyElements][numComponentsPerElement];
								int globalIndex = 0;
								for (int e = 0; e < numKeyElements; e++) {
									float[] components = outputs[e];
									for (int c = 0; c < numComponentsPerElement; c++) {
										components[c] = outputFloatData.get(globalIndex++, 0);
									}
								}
								morphWeightsChannel = new MorphWeightsStepChannel(keys, outputs) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setWeights(target);
									}

								};
								break;
							case LINEAR:
								outputFloatData = (AccessorFloatData) sampler.getOutput().getAccessorData();
								numElements = outputFloatData.getNumElements();
								numComponentsPerElement = numElements / numKeyElements;
								outputs = new float[numKeyElements][numComponentsPerElement];
								globalIndex = 0;
								for (int e = 0; e < numKeyElements; e++) {
									float[] components = outputs[e];
									for (int c = 0; c < numComponentsPerElement; c++) {
										components[c] = outputFloatData.get(globalIndex++, 0);
									}
								}
								morphWeightsChannel = new MorphWeightsLinearChannel(keys, outputs) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setWeights(target);
									}

								};
								break;
							case CUBICSPLINE:
								outputFloatData = (AccessorFloatData) sampler.getOutput().getAccessorData();
								numElements = outputFloatData.getNumElements();
								numComponentsPerElement = numElements / numKeyElements / 3;
								float[][][] outputsCublicSpline = new float[numKeyElements][3][numComponentsPerElement];
								globalIndex = 0;
								for (int e = 0; e < numKeyElements; e++) {
									float[][] elements = outputsCublicSpline[e];
									for (int k = 0; k < 3; k++) {
										float[] components = elements[k];
										for (int c = 0; c < numComponentsPerElement; c++) {
											components[c] = outputFloatData.get(globalIndex++, 0);
										}
									}
								}
								morphWeightsChannel = new MorphWeightsCubicSplineChannel(keys, outputsCublicSpline) {

									@Override
									public void update(float timeS) {
										super.update(timeS);
										nodeAccessor.setWeights(target);
									}

								};
								break;
							default:
								return null; //TODO: Throw exception.
						}
						morphWeightsChannel.setTarget(new float[numComponentsPerElement]);
						animationChannels[j] = morphWeightsChannel;
						break;
				}
			}
		}
		return gltfAnimationPlayer;
	}

	protected Vector3f[] outputDataToVector3fFrame(Sampler sampler) {
		AccessorFloatData outputFloatData = (AccessorFloatData) sampler.getOutput().getAccessorData();
		int numElements = outputFloatData.getNumElements();
		Vector3f[] outputs = new Vector3f[numElements];
		for (int e = 0; e < numElements; e++) {
			outputs[e] = new Vector3f(outputFloatData.get(e, 0), outputFloatData.get(e, 1), outputFloatData.get(e, 2));
		}
		return outputs;
	}

	protected Vector3f[][] outputDataToVector3fPointFrame(Sampler sampler) {
		AccessorFloatData outputFloatData = (AccessorFloatData) sampler.getOutput().getAccessorData();
		int numElements = outputFloatData.getNumElements() / 3;
		Vector3f[][] outputs = new Vector3f[numElements][3];
		int globalIndex = 0;
		for (int e = 0; e < numElements; e++) {
			Vector3f[] elements = outputs[e];
			for (int i = 0; i < 3; i++) {
				elements[i] = new Vector3f(outputFloatData.get(globalIndex, 0), outputFloatData.get(globalIndex, 1), outputFloatData.get(globalIndex, 2));
				++globalIndex;
			}
		}
		return outputs;
	}

	protected Quaternionf[] outputDataToQuaternionfFrame(Sampler sampler) {
		AccessorFloatData outputFloatData = (AccessorFloatData) sampler.getOutput().getAccessorData();
		int numElements = outputFloatData.getNumElements();
		Quaternionf[] outputs = new Quaternionf[numElements];
		for (int e = 0; e < numElements; e++) {
			outputs[e] = new Quaternionf(outputFloatData.get(e, 0), outputFloatData.get(e, 1), outputFloatData.get(e, 2), outputFloatData.get(e, 3));
		}
		return outputs;
	}

	protected Vector4f[][] outputDataToVector4fPointFrame(Sampler sampler) {
		AccessorFloatData outputFloatData = (AccessorFloatData) sampler.getOutput().getAccessorData();
		int numElements = outputFloatData.getNumElements() / 3;
		Vector4f[][] outputs = new Vector4f[numElements][3];
		int globalIndex = 0;
		for (int e = 0; e < numElements; e++) {
			Vector4f[] elements = outputs[e];
			for (int i = 0; i < 3; i++) {
				elements[i] = new Vector4f(outputFloatData.get(globalIndex, 0), outputFloatData.get(globalIndex, 1), outputFloatData.get(globalIndex, 2), outputFloatData.get(globalIndex, 3));
				++globalIndex;
			}
		}
		return outputs;
	}

}
