package com.timlee9024.crgltf.api.v0;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A listener use to listen to {@link UniversalGltfRenderer} for:
 * <ul>
 *     <li>{@code modelId} assignment.</li>
 *     <li>Additional source model processing.</li>
 *     <li>Finish signal of renderable model creation.</li>
 * </ul>
 */
public interface UniversalGltfRendererListener {

	/**
	 * <ol>
	 *     <li>
	 *         For listener added to {@link UniversalGltfRenderer#addShareableModels(Collection)},</br>
	 *         this method will be called in the <b>another thread</b> in between {@code modelId} assignment and renderable model creation when:
	 *         <ul>
	 *             <li>Call from {@link UniversalGltfRenderer#addShareableModels(Collection)}</li>
	 *             <li>Call from {@link net.minecraftforge.client.resource.ISelectiveResourceReloadListener#onResourceManagerReload(net.minecraft.client.resources.IResourceManager, java.util.function.Predicate) onResourceManagerReload} for model files reloading.</li>
	 *             <li>Duplicated {@link net.minecraft.util.ResourceLocation ResourceLocation} found in another {@link UniversalGltfRenderer#addShareableModels(Collection)} call after game startup completed.</li>
	 *         </ul>
	 *         If the model failed to finish the loading, this method will be called in the <b>same thread</b> with param {@link CompiledGltfModel} being <b>null</b>.</br>
	 *     </li>
	 *     <li>
	 *         For listener added to {@link UniversalGltfRenderer#addStandaloneModels(Collection)},</br>
	 *         this method will be called in the <b>same thread</b> in between {@code modelId} assignment and renderable model creation when:
	 *         <ul>
	 *             <li>Call from {@link UniversalGltfRenderer#addStandaloneModels(Collection)}</li>
	 *             <li>Call from {@link net.minecraftforge.client.resource.ISelectiveResourceReloadListener#onResourceManagerReload(net.minecraft.client.resources.IResourceManager, java.util.function.Predicate) onResourceManagerReload} for {@code modelId} reassignment.</li>
	 *         </ul>
	 *     </li>
	 * </ol>
	 *
	 * @param modelId           Used to access renderable model in {@link UniversalGltfRenderer}.
	 * @param compiledGltfModel The {@link CompiledGltfModel} created from {@link net.minecraft.util.ResourceLocation ResourceLocation}. Only provided if listener was added to {@link UniversalGltfRenderer#addShareableModels(Collection)} and the glTF model was loaded successfully.
	 * @return {@link CompiledGltfModel} Only designed for {@link UniversalGltfRenderer#addShareableModels(Collection)} to return {@link CompiledGltfModel} for renderer, doesn't take any effect for {@link UniversalGltfRenderer#addShareableModels(Collection)}. If it is second time this get called, you can return null to retain previous renderable model.
	 */
	@Nullable CompiledGltfModel onModelIdAssigned(int modelId, @Nullable CompiledGltfModel compiledGltfModel);

	/**
	 * Call when the renderable model is ready for the second time.</br>
	 * This is equivalent to the state of renderable model creation finished for the first time after {@link UniversalGltfRenderer#addShareableModels(Collection)} and {@link UniversalGltfRenderer#addStandaloneModels(Collection)}.
	 */
	void onModelRefreshComplete();
}
