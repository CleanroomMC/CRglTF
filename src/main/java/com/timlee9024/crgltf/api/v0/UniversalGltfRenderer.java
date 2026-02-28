package com.timlee9024.crgltf.api.v0;

import de.javagl.jgltf.model.GltfModel;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.util.Collection;
import java.util.Map;

/**
 * A general-purpose glTF model renderer and model loader that support load file from {@link ResourceLocation} or custom model source.
 */
public interface UniversalGltfRenderer {

	/**
	 * Batch adding {@link ResourceLocation} of gltf file into this renderer.</br>
	 * This renderer will handle the model loading/reloading and create renderable model that can be access by {@code modelId} in the {@link UniversalGltfRendererListener}.</br>
	 * The gltf file and its dependency files will share across different {@link UniversalGltfRendererListener} that pair with it.</br>
	 * Once this method return, you are ready to use the {@code modelId} delivered by {@link UniversalGltfRendererListener} to render the model.</br>
	 * </br>
	 * Notes:
	 * <ul>
	 *     <li>Only call this method at the main client thread, since the creation of renderable model will require OpenGL context.</li>
	 *     <li>{@link UniversalGltfRendererListener#onModelIdAssigned(int, CompiledGltfModel)} will be called in another thread,
	 *     you can read {@link GltfModel} there to find the index of animation and node by its {@link de.javagl.jgltf.model.NamedModelElement#getName() getName()} or {@link de.javagl.jgltf.model.ModelElement#getExtras() getExtras()}.</li>
	 *     <li>If you call this method with same {@link ResourceLocation} that already in this renderer after game startup complete,
	 *     the model associate to it will force to reload and refresh the renderable model.</li>
	 * </ul>
	 *
	 * @param entries Each entry contains {@link ResourceLocation} of gltf file and {@link UniversalGltfRendererListener} that pair with it.
	 */
	void addShareableModels(@NotNull Collection<Map.Entry<ResourceLocation, UniversalGltfRendererListener>> entries);

	/**
	 * Batch adding {@link CompiledGltfModel} from {@link UniversalGltfRendererListener} into this renderer.</br>
	 * This renderer will create renderable model that can be access by {@code modelId} in the {@link UniversalGltfRendererListener}.</br>
	 * This will allow you to create your own custom gltf files management(e.g. online model delivery),
	 * and reduce game stalling by moving model IO and processing into another thread.</br>
	 * Once this method return, you are ready to use the {@code modelId} delivered by {@link UniversalGltfRendererListener} to render the model.
	 * </br>
	 * </br>
	 * Notes:
	 * <ul>
	 *     <li>Only call this method at the main client thread, since the creation of renderable model will require OpenGL context.</li>
	 *     <li>The {@link CompiledGltfModel} will not share across different {@link UniversalGltfRendererListener} even you pass the same instance.</li>
	 *     <li>This renderer does not store {@link CompiledGltfModel} internally after this method return.
	 *     So during game resources reload, the renderable model will not refresh, only {@code modelId} will be reassigned if necessary.
	 *     </li>
	 *     <li>And for the above reason, the {@link CompiledGltfModel} deliver from {@link UniversalGltfRendererListener#onModelIdAssigned(int, CompiledGltfModel)} will always being null.</li>
	 * </ul>
	 *
	 * @param listeners Each {@link UniversalGltfRendererListener} that return {@link CompiledGltfModel} for renderer by {@link UniversalGltfRendererListener#onModelIdAssigned(int, CompiledGltfModel)}.
	 */
	void addStandaloneModels(@NotNull Collection<UniversalGltfRendererListener> listeners);

	/**
	 * Remove {@link UniversalGltfRendererListener}s associate to the shareable model in this renderer.
	 * The shareable model will be removed if all of its {@link UniversalGltfRendererListener} get removed.
	 *
	 * @param resourceLocation {@link ResourceLocation} of the shareable model.
	 * @param listeners        {@link Collection} that contains {@link UniversalGltfRendererListener} associate to the shareable model.
	 */
	void removeListenersFromShareableModel(@NotNull ResourceLocation resourceLocation, @NotNull Collection<UniversalGltfRendererListener> listeners);

	/**
	 * Remove the standalone model by its {@code modelId}.
	 *
	 * @param modelId Model ID belongs to the standalone model.
	 */
	void removeStandaloneModel(int modelId);

	/**
	 * Create a {@link CompiledGltfModel} that compatible with this renderer for {@link #addStandaloneModels(Collection)}.
	 * This method can be in the same thread of your model IO.
	 *
	 * @param gltfModel The {@link GltfModel} used to create {@link CompiledGltfModel}. This instance will be modified if necessary.
	 * @return {@link CompiledGltfModel} that compatible with this renderer.
	 */
	@NotNull CompiledGltfModel createCompiledGltfModel(@NotNull GltfModel gltfModel);

	/**
	 * Render the model by its {@code modelId} and its {@code scene} index.</br>
	 *
	 * @param modelId Model in this renderer, it is given by {@link UniversalGltfRendererListener}.
	 * @param scene   Scene index of the model.
	 */
	void render(int modelId, int scene);

	/**
	 * Set the model's transformation and morph weight at the specified time tick in the animation.</br>
	 * The transformation and morph weight will reset to its initial state after {@link #render(int, int)}.
	 *
	 * @param modelId      Model in this renderer, it is given by {@link UniversalGltfRendererListener}.
	 * @param animation    Animation index of the model.
	 * @param timeInSecond Time tick in second of the animation.
	 */
	void playAnimation(int modelId, int animation, float timeInSecond);

	/**
	 * Set translation of the node's TRS properties by its index.</br>
	 * The result will be ignored if transform matrix present in the node.</br>
	 * This property will reset to its initial value after {@link #render(int, int)}.</br>
	 *
	 * @param modelId Model in this renderer, it is given by {@link UniversalGltfRendererListener}.
	 * @param node    Node index of model.
	 * @param x       X value of translation.
	 * @param y       Y value of translation.
	 * @param z       Z value of translation.
	 */
	void setNodeTranslation(int modelId, int node, float x, float y, float z);

	/**
	 * Set rotation of the node's TRS properties by its index.</br>
	 * The result will be ignored if transform matrix present in the node.</br>
	 * This property will reset to its initial value after {@link #render(int, int)}.</br>
	 *
	 * @param modelId Model in this renderer, it is given by {@link UniversalGltfRendererListener}.
	 * @param node    Node index of model.
	 * @param x       X value of quaternion.
	 * @param y       Y value of quaternion.
	 * @param z       Z value of quaternion.
	 * @param w       W value of quaternion.
	 */
	void setNodeRotation(int modelId, int node, float x, float y, float z, float w);

	/**
	 * Set scale of the node's TRS properties by its index.</br>
	 * If all the xyz are set to 0, it will be able to culling mesh to save performance.</br>
	 * The result will be ignored if transform matrix present in the node.</br>
	 * This property will reset to its initial value after {@link #render(int, int)}.
	 *
	 * @param modelId Model in this renderer, it is given by {@link UniversalGltfRendererListener}.
	 * @param node    Node index of model.
	 * @param x       X value of scale.
	 * @param y       Y value of scale.
	 * @param z       Z value of scale.
	 */
	void setNodeScale(int modelId, int node, float x, float y, float z);

	/**
	 * Set transform matrix of the node by its index.</br>
	 * If pass zero matrix from {@link #getZeroMatrixReference()}, it will be able to culling mesh to save performance.</br>
	 * This property will reset to its initial value after {@link #render(int, int)}.
	 *
	 * @param modelId         Model in this renderer, it is given by {@link UniversalGltfRendererListener}.
	 * @param node            Node index of model.
	 * @param transformMatrix The transform matrix, it can pass null to use node's TRS properties.
	 */
	void setNodeTransformMatrix(int modelId, int node, @Nullable Matrix4fc transformMatrix);

	/**
	 * The zero matrix reference used by internal culling function.</br>
	 * Only this reference will trigger mesh culling, other zero matrix instance will not.</br>
	 *
	 * @return The zero matrix reference.
	 */
	Matrix4fc getZeroMatrixReference();

	/**
	 * Set morph weights of the node by its index.</br>
	 * This property will reset to its initial value after {@link #render(int, int)}.
	 *
	 * @param modelId Model in this renderer, it is given by {@link UniversalGltfRendererListener}.
	 * @param node    Node index of model.
	 * @param weights The morph weights, it can pass null to use either mesh's morph weights or all zero weights.
	 */
	void setNodeWeights(int modelId, int node, float @Nullable [] weights);
}
