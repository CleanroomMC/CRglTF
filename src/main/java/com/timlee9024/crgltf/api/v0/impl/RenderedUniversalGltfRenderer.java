package com.timlee9024.crgltf.api.v0.impl;

import com.timlee9024.crgltf.CRglTFMod;
import com.timlee9024.crgltf.api.v0.CompiledGltfModel;
import com.timlee9024.crgltf.api.v0.UniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.UniversalGltfRendererListener;
import com.timlee9024.crgltf.gl.rendered.GltfAnimationPlayer;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import com.timlee9024.crgltf.gl.rendered.RenderedGltfModel;
import com.timlee9024.crgltf.modified.jgltf.model.io.GltfReader;
import com.timlee9024.crgltf.util.GltfModelAttributeProcessor;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.image.PixelData;
import de.javagl.jgltf.model.image.PixelDatas;
import de.javagl.jgltf.model.io.Buffers;
import de.javagl.jgltf.model.io.GltfReference;
import de.javagl.jgltf.model.io.IO;
import de.javagl.jgltf.model.io.RawGltfData;
import de.javagl.jgltf.model.io.RawGltfDataReader;
import de.javagl.jgltf.model.io.v1.GltfAssetV1;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.v1.GltfModelCreatorV1;
import de.javagl.jgltf.model.v2.GltfModelCreatorV2;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class RenderedUniversalGltfRenderer<T extends RenderedGltfModel, A extends GltfAnimationPlayer> implements UniversalGltfRenderer, ISelectiveResourceReloadListener {

	protected final Map<ResourceLocation, ListenerGroup> sharedModelListenerGroups = new HashMap<>();
	protected final Map<String, Callable<ByteBuffer>> sharedURLByteDatas = new HashMap<>();
	protected final Map<ResourceLocation, Callable<ByteBuffer>> sharedByteDatas = new HashMap<>();
	protected final Map<ResourceLocation, Supplier<PixelData>> sharedPixelDatas = new HashMap<>();

	protected final Int2ObjectOpenHashMap<UniversalGltfRendererListener> standaloneModelListeners = new Int2ObjectOpenHashMap<>();

	protected boolean isFinishModLoading;
	protected IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();

	/**
	 * The renderable models.
	 */
	public T[] renderedGltfModels;
	public A[] gltfAnimationPlayers;

	@Override
	public void addShareableModels(@NotNull Collection<Map.Entry<ResourceLocation, UniversalGltfRendererListener>> entries) {
		if (gltfAnimationPlayers == null) {
			int currentModelId = 0;
			for (Map.Entry<ResourceLocation, UniversalGltfRendererListener> entry : entries) {
				ListenerGroup listenerGroup = sharedModelListenerGroups.get(entry.getKey());
				if (listenerGroup != null) {
					listenerGroup.listeners.add(entry.getValue());
				} else {
					listenerGroup = new ListenerGroup();
					listenerGroup.listeners = new LinkedList<>();
					listenerGroup.modelId = currentModelId++;
					listenerGroup.listeners.add(entry.getValue());
					sharedModelListenerGroups.put(entry.getKey(), listenerGroup);
				}
			}

			List<Map.Entry<Map.Entry<ResourceLocation, ListenerGroup>, Future<?>>> results = new ArrayList<>(sharedModelListenerGroups.size());
			try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
				for (Map.Entry<ResourceLocation, ListenerGroup> entry : sharedModelListenerGroups.entrySet()) {
					results.add(new AbstractMap.SimpleEntry<>(entry, executorService.submit(entry.getValue().createLoadModelCallable(entry.getKey()))));
				}
			}
			for (Map.Entry<Map.Entry<ResourceLocation, ListenerGroup>, Future<?>> result : results) {
				try {
					result.getValue().get();
				} catch (InterruptedException | ExecutionException e) {
					CRglTFMod.logger.error("Failed to load glTF model from {}", result.getKey().getKey().toString(), e);
					ListenerGroup listenerGroup = result.getKey().getValue();
					for (UniversalGltfRendererListener listener : listenerGroup.listeners) {
						listener.onModelIdAssigned(listenerGroup.modelId, null);
					}
				}
			}

			renderedGltfModels = createRenderedGltfModelsArray(currentModelId);
			gltfAnimationPlayers = createGltfAnimationPlayerArray(currentModelId);

			setupGltfModelForRender(sharedModelListenerGroups.values());

			if (isFinishModLoading) {
				for (Map.Entry<ResourceLocation, ListenerGroup> modelToLoad : sharedModelListenerGroups.entrySet()) {
					ListenerGroup listenerGroup = modelToLoad.getValue();
					listenerGroup.compiledGltfModel = null;
				}
				sharedURLByteDatas.clear();
				sharedByteDatas.clear();
				sharedPixelDatas.clear();
			}
		} else {
			Map<ResourceLocation, ListenerGroup> pendingToLoadListenerGroups = new HashMap<>(entries.size());
			if (isFinishModLoading) {
				Map<ListenerGroup, UniversalGltfRendererListener> lastListenerOfGroups = new IdentityHashMap<>(entries.size());
				for (Map.Entry<ResourceLocation, UniversalGltfRendererListener> entry : entries) {
					ResourceLocation resourceLocation = entry.getKey();
					ListenerGroup listenerGroup = sharedModelListenerGroups.get(resourceLocation);
					if (listenerGroup != null) {
						CRglTFMod.logger.debug("Duplicated ResourceLocation found in another addShareableModels() after game startup completed, delete existing renderable model.");
						T renderedGltfModel = renderedGltfModels[listenerGroup.modelId];
						if (renderedGltfModel != null) renderedGltfModel.deleteOpenGLData();
						lastListenerOfGroups.put(listenerGroup, listenerGroup.listeners.getLast());
						pendingToLoadListenerGroups.put(resourceLocation, listenerGroup);
					}
				}

				int currentModelId = gltfAnimationPlayers.length;
				for (Map.Entry<ResourceLocation, UniversalGltfRendererListener> entry : entries) {
					ListenerGroup listenerGroup = sharedModelListenerGroups.get(entry.getKey());
					if (listenerGroup != null) {
						listenerGroup.listeners.add(entry.getValue());
					} else {
						listenerGroup = new ListenerGroup();
						listenerGroup.listeners = new LinkedList<>();
						listenerGroup.modelId = currentModelId++;
						listenerGroup.listeners.add(entry.getValue());
						sharedModelListenerGroups.put(entry.getKey(), listenerGroup);
						pendingToLoadListenerGroups.put(entry.getKey(), listenerGroup);
					}
				}

				List<Map.Entry<Map.Entry<ResourceLocation, ListenerGroup>, Future<?>>> results = new ArrayList<>(pendingToLoadListenerGroups.size());
				try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
					for (Map.Entry<ResourceLocation, ListenerGroup> entry : pendingToLoadListenerGroups.entrySet()) {
						results.add(new AbstractMap.SimpleEntry<>(entry, executorService.submit(entry.getValue().createLoadModelCallable(entry.getKey()))));
					}
				}
				for (Map.Entry<Map.Entry<ResourceLocation, ListenerGroup>, Future<?>> result : results) {
					try {
						result.getValue().get();
					} catch (InterruptedException | ExecutionException e) {
						CRglTFMod.logger.error("Failed to load glTF model from {}", result.getKey().getKey().toString(), e);
						ListenerGroup listenerGroup = result.getKey().getValue();
						for (UniversalGltfRendererListener listener : listenerGroup.listeners) {
							listener.onModelIdAssigned(listenerGroup.modelId, null);
						}
					}
				}

				T[] resizedRenderedGltfModels = createRenderedGltfModelsArray(renderedGltfModels.length + pendingToLoadListenerGroups.size());
				System.arraycopy(renderedGltfModels, 0, resizedRenderedGltfModels, 0, renderedGltfModels.length);
				renderedGltfModels = resizedRenderedGltfModels;
				A[] resizedGltfAnimationPlayers = createGltfAnimationPlayerArray(gltfAnimationPlayers.length + pendingToLoadListenerGroups.size());
				System.arraycopy(gltfAnimationPlayers, 0, resizedGltfAnimationPlayers, 0, gltfAnimationPlayers.length);
				gltfAnimationPlayers = resizedGltfAnimationPlayers;

				setupGltfModelForRender(pendingToLoadListenerGroups.values());

				for (Map.Entry<ResourceLocation, ListenerGroup> entry : pendingToLoadListenerGroups.entrySet()) {
					ListenerGroup listenerGroup = entry.getValue();
					UniversalGltfRendererListener lastListenerOfGroup = lastListenerOfGroups.get(listenerGroup);
					if (lastListenerOfGroup != null) {
						for (UniversalGltfRendererListener listener : listenerGroup.listeners) {
							listener.onModelRefreshComplete();
							if (listener == lastListenerOfGroup) break;
						}
					}
					listenerGroup.compiledGltfModel = null;
				}

				sharedURLByteDatas.clear();
				sharedByteDatas.clear();
				sharedPixelDatas.clear();
			} else {
				List<Runnable> alreadyLoadedModelToListeners = new ArrayList<>(entries.size());
				int currentModelId = gltfAnimationPlayers.length;
				for (Map.Entry<ResourceLocation, UniversalGltfRendererListener> entry : entries) {
					ResourceLocation resourceLocation = entry.getKey();
					ListenerGroup listenerGroup = sharedModelListenerGroups.get(resourceLocation);
					if (listenerGroup != null) {
						if (listenerGroup.compiledGltfModel != null) {
							UniversalGltfRendererListener listener = entry.getValue();
							int modelId = listenerGroup.modelId;
							CompiledGltfModel compiledGltfModel = listenerGroup.compiledGltfModel;
							alreadyLoadedModelToListeners.add(() -> listener.onModelIdAssigned(modelId, compiledGltfModel));
						}
						listenerGroup.listeners.add(entry.getValue());
					} else {
						listenerGroup = new ListenerGroup();
						listenerGroup.listeners = new LinkedList<>();
						listenerGroup.modelId = currentModelId++;
						listenerGroup.listeners.add(entry.getValue());
						sharedModelListenerGroups.put(resourceLocation, listenerGroup);
						pendingToLoadListenerGroups.put(resourceLocation, listenerGroup);
					}
				}

				List<Map.Entry<Map.Entry<ResourceLocation, ListenerGroup>, Future<?>>> results = new ArrayList<>(pendingToLoadListenerGroups.size());
				try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
					for (Map.Entry<ResourceLocation, ListenerGroup> entry : pendingToLoadListenerGroups.entrySet()) {
						results.add(new AbstractMap.SimpleEntry<>(entry, executorService.submit(entry.getValue().createLoadModelCallable(entry.getKey()))));
					}
					for (Runnable runnable : alreadyLoadedModelToListeners) {
						results.add(new AbstractMap.SimpleEntry<>(null, executorService.submit(runnable)));
					}
				}
				for (Map.Entry<Map.Entry<ResourceLocation, ListenerGroup>, Future<?>> result : results) {
					try {
						result.getValue().get();
					} catch (InterruptedException | ExecutionException e) {
						if (result.getKey() == null) {
							CRglTFMod.logger.error(e);
						} else {
							CRglTFMod.logger.error("Failed to load glTF model from {}", result.getKey().getKey().toString(), e);
							ListenerGroup listenerGroup = result.getKey().getValue();
							for (UniversalGltfRendererListener listener : listenerGroup.listeners) {
								listener.onModelIdAssigned(listenerGroup.modelId, null);
							}
						}
					}
				}

				T[] resizedRenderedGltfModels = createRenderedGltfModelsArray(currentModelId);
				System.arraycopy(renderedGltfModels, 0, resizedRenderedGltfModels, 0, renderedGltfModels.length);
				renderedGltfModels = resizedRenderedGltfModels;
				A[] resizedGltfAnimationPlayers = createGltfAnimationPlayerArray(currentModelId);
				System.arraycopy(gltfAnimationPlayers, 0, resizedGltfAnimationPlayers, 0, gltfAnimationPlayers.length);
				gltfAnimationPlayers = resizedGltfAnimationPlayers;

				setupGltfModelForRender(pendingToLoadListenerGroups.values());
			}
		}
	}

	@Override
	public void addStandaloneModels(@NotNull Collection<UniversalGltfRendererListener> listeners) {
		if (gltfAnimationPlayers == null) {
			int currentModelId = 0;

			List<ListenerGroup> listenerGroups = new ArrayList<>(listeners.size());
			for (UniversalGltfRendererListener listener : listeners) {
				ListenerGroup listenerGroup = new ListenerGroup();
				listenerGroup.modelId = currentModelId++;
				listenerGroup.compiledGltfModel = listener.onModelIdAssigned(listenerGroup.modelId, null);
				listenerGroups.add(listenerGroup);
			}

			renderedGltfModels = createRenderedGltfModelsArray(currentModelId);
			gltfAnimationPlayers = createGltfAnimationPlayerArray(currentModelId);

			setupGltfModelForRender(listenerGroups);
		} else {
			int currentModelId = gltfAnimationPlayers.length;

			List<ListenerGroup> listenerGroups = new ArrayList<>(listeners.size());
			for (UniversalGltfRendererListener listener : listeners) {
				ListenerGroup listenerGroup = new ListenerGroup();
				listenerGroup.modelId = currentModelId++;
				listenerGroup.compiledGltfModel = listener.onModelIdAssigned(listenerGroup.modelId, null);
				listenerGroups.add(listenerGroup);
			}

			T[] resizedRenderedGltfModels = createRenderedGltfModelsArray(currentModelId);
			System.arraycopy(renderedGltfModels, 0, resizedRenderedGltfModels, 0, renderedGltfModels.length);
			renderedGltfModels = resizedRenderedGltfModels;
			A[] resizedGltfAnimationPlayers = createGltfAnimationPlayerArray(currentModelId);
			System.arraycopy(gltfAnimationPlayers, 0, resizedGltfAnimationPlayers, 0, gltfAnimationPlayers.length);
			gltfAnimationPlayers = resizedGltfAnimationPlayers;

			setupGltfModelForRender(listenerGroups);
		}
	}

	@Override
	public void removeListenersFromShareableModel(@NotNull ResourceLocation resourceLocation, @NotNull Collection<UniversalGltfRendererListener> listeners) {
		ListenerGroup listenerGroup = sharedModelListenerGroups.get(resourceLocation);
		if (listenerGroup == null) return;
		listenerGroup.listeners.removeAll(listeners);
		if (listenerGroup.listeners.isEmpty()) {
			CRglTFMod.logger.debug("Remove shareable model {}, because no more listeners reference to it.", resourceLocation.toString());
			T renderedGltfModel = renderedGltfModels[listenerGroup.modelId];
			if (renderedGltfModel != null) {
				renderedGltfModel.deleteOpenGLData();
				renderedGltfModels[listenerGroup.modelId] = null;
			}
			gltfAnimationPlayers[listenerGroup.modelId] = null;
			sharedModelListenerGroups.remove(resourceLocation);
		}
	}

	@Override
	public void removeStandaloneModel(int modelId) {
		if (renderedGltfModels[modelId] == null) return;
		renderedGltfModels[modelId].deleteOpenGLData();
		standaloneModelListeners.remove(modelId);
		renderedGltfModels[modelId] = null;
		gltfAnimationPlayers[modelId] = null;
	}

	@Override
	public @NotNull CompiledGltfModel createCompiledGltfModel(@NotNull GltfModel gltfModel) {
		GltfModelAttributeProcessor.processAttributesForRender(gltfModel);
		Map<ImageModel, PixelData> pixelDataLookup = new IdentityHashMap<>();
		for (ImageModel imageModel : gltfModel.getImageModels()) {
			BufferViewModel bufferViewModel = imageModel.getBufferViewModel();
			if (bufferViewModel != null) {
				pixelDataLookup.put(imageModel, PixelDatas.create(bufferViewModel.getBufferViewData()));
			} else {
				pixelDataLookup.put(imageModel, PixelDatas.create(imageModel.getImageData()));
			}
		}
		return new CompiledGltfModel() {
			@Override
			public @NotNull GltfModel getGltfModel() {
				return gltfModel;
			}

			@Override
			public @NotNull Map<ImageModel, PixelData> getPixelDataLookup() {
				return pixelDataLookup;
			}
		};
	}

	@Override
	public void onResourceManagerReload(@NotNull IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
		if (!resourcePredicate.test(VanillaResourceType.MODELS) || !isFinishModLoading) return;
		CRglTFMod.logger.debug("Refresh all models started.");
		this.resourceManager = resourceManager;

		T[] oldRenderedGltfModels = renderedGltfModels;
		A[] oldGltfAnimationPlayers = gltfAnimationPlayers;

		int size = sharedModelListenerGroups.size() + standaloneModelListeners.size();
		renderedGltfModels = createRenderedGltfModelsArray(size);
		gltfAnimationPlayers = createGltfAnimationPlayerArray(size);

		int currentModelId = 0;
		List<ListenerGroup> pendingReloadListenerGroups = new ArrayList<>(size);

		UniversalGltfRendererListener[] listeners = new UniversalGltfRendererListener[standaloneModelListeners.size()];
		for (Int2ObjectMap.Entry<UniversalGltfRendererListener> entry : standaloneModelListeners.int2ObjectEntrySet()) {
			renderedGltfModels[currentModelId] = oldRenderedGltfModels[entry.getIntKey()];
			gltfAnimationPlayers[currentModelId] = oldGltfAnimationPlayers[entry.getIntKey()];

			UniversalGltfRendererListener listener = entry.getValue();
			CompiledGltfModel compiledGltfModel = listener.onModelIdAssigned(currentModelId, null);
			if (compiledGltfModel != null) {
				renderedGltfModels[currentModelId].deleteOpenGLData();
				ListenerGroup listenerGroup = new ListenerGroup();
				listenerGroup.modelId = currentModelId;
				listenerGroup.compiledGltfModel = compiledGltfModel;
				pendingReloadListenerGroups.add(listenerGroup);
			} else listener.onModelRefreshComplete();
			listeners[currentModelId++] = listener;
		}
		standaloneModelListeners.clear();
		for (int i = 0; i < listeners.length; i++) {
			standaloneModelListeners.put(i, listeners[i]);
		}

		List<Map.Entry<Map.Entry<ResourceLocation, ListenerGroup>, Future<?>>> results = new ArrayList<>(sharedModelListenerGroups.size());
		try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			for (Map.Entry<ResourceLocation, ListenerGroup> entry : sharedModelListenerGroups.entrySet()) {
				ListenerGroup listenerGroup = entry.getValue();
				T renderedGltfModel = oldRenderedGltfModels[listenerGroup.modelId];
				if (renderedGltfModel != null) renderedGltfModel.deleteOpenGLData();
				listenerGroup.modelId = currentModelId++;
				pendingReloadListenerGroups.add(listenerGroup);
				results.add(new AbstractMap.SimpleEntry<>(entry, executorService.submit(entry.getValue().createLoadModelCallable(entry.getKey()))));
			}
		}
		for (Map.Entry<Map.Entry<ResourceLocation, ListenerGroup>, Future<?>> result : results) {
			try {
				result.getValue().get();
			} catch (InterruptedException | ExecutionException e) {
				CRglTFMod.logger.error("Failed to load glTF model from {}", result.getKey().getKey().toString(), e);
				ListenerGroup listenerGroup = result.getKey().getValue();
				for (UniversalGltfRendererListener listener : listenerGroup.listeners) {
					listener.onModelIdAssigned(listenerGroup.modelId, null);
				}
			}
		}

		setupGltfModelForRender(pendingReloadListenerGroups);
		for (ListenerGroup listenerGroup : pendingReloadListenerGroups) {
			for (UniversalGltfRendererListener listener : listenerGroup.listeners) {
				listener.onModelRefreshComplete();
			}
			listenerGroup.compiledGltfModel = null;
		}
		sharedURLByteDatas.clear();
		sharedByteDatas.clear();
		sharedPixelDatas.clear();
		CRglTFMod.logger.debug("Refresh all models completed.");
	}

	@Override
	public void playAnimation(int modelId, int animation, float timeInSecond) {
		gltfAnimationPlayers[modelId].applyAnimation(animation, timeInSecond);
	}

	@Override
	public void setNodeTranslation(int modelId, int node, float x, float y, float z) {
		NodeAccessor nodeAccessor = renderedGltfModels[modelId].getNodeAccessorByNode(node);
		nodeAccessor.getTranslation().set(x, y, z);
		nodeAccessor.setTRSModified(true);
	}

	@Override
	public void setNodeRotation(int modelId, int node, float x, float y, float z, float w) {
		NodeAccessor nodeAccessor = renderedGltfModels[modelId].getNodeAccessorByNode(node);
		nodeAccessor.getRotation().set(x, y, z, w);
		nodeAccessor.setTRSModified(true);
	}

	@Override
	public void setNodeScale(int modelId, int node, float x, float y, float z) {
		NodeAccessor nodeAccessor = renderedGltfModels[modelId].getNodeAccessorByNode(node);
		nodeAccessor.getScale().set(x, y, z);
		nodeAccessor.setTRSModified(true);
	}

	@Override
	public void setNodeTransformMatrix(int modelId, int node, @Nullable Matrix4fc transformMatrix) {
		NodeAccessor nodeAccessor = renderedGltfModels[modelId].getNodeAccessorByNode(node);
		nodeAccessor.setTransformMatrix(transformMatrix);
		nodeAccessor.setTransformMatrixModified(true);
	}

	@Override
	public Matrix4fc getZeroMatrixReference() {
		return NodeAccessor.ZERO_MATRIX;
	}

	@Override
	public void setNodeWeights(int modelId, int node, float @Nullable [] weights) {
		renderedGltfModels[modelId].getNodeAccessorByNode(node).setWeights(weights);
	}

	public void onEvent(FMLLoadCompleteEvent event) {
		((SimpleReloadableResourceManager) resourceManager).registerReloadListener(this);
		sharedModelListenerGroups.forEach((key, value) -> value.compiledGltfModel = null);
		sharedURLByteDatas.clear();
		sharedByteDatas.clear();
		sharedPixelDatas.clear();
		isFinishModLoading = true;
		CRglTFMod.logger.debug("Game startup model loading completed.");
	}

	protected abstract T[] createRenderedGltfModelsArray(int length);

	protected abstract A[] createGltfAnimationPlayerArray(int length);

	protected abstract void setupGltfModelForRender(Collection<ListenerGroup> listenerGroups);

	public class ListenerGroup {
		public int modelId;
		public CompiledGltfModel compiledGltfModel;
		public List<UniversalGltfRendererListener> listeners;

		public Callable<?> createLoadModelCallable(ResourceLocation resourceLocation) {
			return () -> {
				URI baseUri = new URI(resourceLocation.getNamespace() + "/" + resourceLocation.getPath());
				GltfModel gltfModel;
				try (IResource resource = resourceManager.getResource(resourceLocation)) {
					try (InputStream inputStream = resource.getInputStream()) {
						RawGltfData rawGltfData = RawGltfDataReader.read(inputStream);
						GltfReader gltfReader = new GltfReader();
						ByteBuffer jsonData = rawGltfData.getJsonData();
						try (InputStream jsonInputStream = Buffers.createByteBufferInputStream(jsonData)) {
							gltfReader.read(jsonInputStream);
							int majorVersion = gltfReader.getMajorVersion();
							if (majorVersion == 1) {
								de.javagl.jgltf.impl.v1.GlTF gltfV1 = gltfReader.getAsGltfV1();
								GltfAssetV1 gltfAsset = new GltfAssetV1(gltfV1, rawGltfData.getBinaryData());
								for (GltfReference reference : gltfAsset.getBufferReferences())
									resolveGltfReference(reference, baseUri);
								for (GltfReference reference : gltfAsset.getImageReferences())
									resolveGltfReference(reference, baseUri);
								gltfModel = GltfModelCreatorV1.create(gltfAsset);
							} else if (majorVersion == 2) {
								de.javagl.jgltf.impl.v2.GlTF gltfV2 = gltfReader.getAsGltfV2();
								GltfAssetV2 gltfAsset = new GltfAssetV2(gltfV2, rawGltfData.getBinaryData());
								for (GltfReference reference : gltfAsset.getBufferReferences())
									resolveGltfReference(reference, baseUri);
								for (GltfReference reference : gltfAsset.getImageReferences())
									resolveGltfReference(reference, baseUri);
								gltfModel = GltfModelCreatorV2.create(gltfAsset);
							} else {
								throw new IOException("Unsupported major version: " + majorVersion);
							}
						}
					}
				}

				GltfModelAttributeProcessor.processAttributesForRender(gltfModel);

				List<ImageModel> imageModels = gltfModel.getImageModels();
				Map<ImageModel, PixelData> pixelDataLookup = new IdentityHashMap<>(imageModels.size());
				for (ImageModel imageModel : imageModels) {
					BufferViewModel bufferViewModel = imageModel.getBufferViewModel();
					if (bufferViewModel != null) {
						pixelDataLookup.put(imageModel, PixelDatas.create(bufferViewModel.getBufferViewData()));
					} else {
						String uri = imageModel.getUri();
						if (IO.isDataUriString(uri)) {
							pixelDataLookup.put(imageModel, PixelDatas.create(imageModel.getImageData()));
						} else {
							ResourceLocation referenceLocation = uriPathToResourceLocation(baseUri.resolve(new URI(uri.replaceAll(" ", "%20"))).toString());
							Supplier<PixelData> supplier;
							synchronized (sharedPixelDatas) {
								supplier = sharedPixelDatas.computeIfAbsent(referenceLocation, k -> new Supplier<>() {
									PixelData value;

									@Override
									public synchronized PixelData get() {
										if (value == null) value = PixelDatas.create(imageModel.getImageData());
										return value;
									}

								});
							}
							pixelDataLookup.put(imageModel, supplier.get());
						}
					}
				}

				compiledGltfModel = new CompiledGltfModel() {
					@Override
					public @NotNull GltfModel getGltfModel() {
						return gltfModel;
					}

					@Override
					public @NotNull Map<ImageModel, PixelData> getPixelDataLookup() {
						return pixelDataLookup;
					}
				};

				for (UniversalGltfRendererListener listener : listeners) {
					listener.onModelIdAssigned(modelId, compiledGltfModel);
				}
				return null;
			};
		}

		protected void resolveGltfReference(GltfReference reference, URI baseUri) throws Exception {
			String escapedUriString = reference.getUri().replaceAll(" ", "%20");
			URI uri = new URI(escapedUriString);
			if (uri.isAbsolute()) {
				URL url = uri.toURL();
				Callable<ByteBuffer> callable;
				synchronized (sharedURLByteDatas) {
					callable = sharedURLByteDatas.computeIfAbsent(url.toString(), l -> new Callable<>() {
						ByteBuffer value;

						@Override
						public ByteBuffer call() throws Exception {
							if (value == null) {
								try (InputStream inputStream = url.openConnection().getInputStream()) {
									byte[] data = IO.readStream(inputStream);
									return Buffers.create(data);
								}
							}
							return value;
						}
					});
				}
				reference.getTarget().accept(callable.call());
			} else {
				ResourceLocation referenceLocation = uriPathToResourceLocation(baseUri.resolve(uri).toString());
				Callable<ByteBuffer> callable;
				synchronized (sharedByteDatas) {
					callable = sharedByteDatas.computeIfAbsent(referenceLocation, l -> new Callable<>() {
						ByteBuffer value;

						@Override
						public ByteBuffer call() throws Exception {
							if (value == null) {
								try (IResource resource = resourceManager.getResource(l)) {
									try (InputStream inputStream = resource.getInputStream()) {
										byte[] data = IO.readStream(inputStream);
										return Buffers.create(data);
									}
								}
							}
							return value;
						}
					});
				}
				reference.getTarget().accept(callable.call());
			}
		}

		protected ResourceLocation uriPathToResourceLocation(String path) {
			int firstSlash = path.indexOf(47);
			if (firstSlash > 0)
				return new ResourceLocation(path.substring(0, firstSlash), path.substring(firstSlash + 1));
			else if (firstSlash == 0) return new ResourceLocation("minecraft", path.substring(1));
			else return new ResourceLocation("minecraft", path);
		}
	}
}
