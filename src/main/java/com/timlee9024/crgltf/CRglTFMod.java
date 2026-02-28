package com.timlee9024.crgltf;

import com.timlee9024.crgltf.api.v0.CRglTFApi;
import com.timlee9024.crgltf.api.v0.UniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.DefaultOptiFineUniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.DefaultUniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.GL21OptiFineUniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.GL21UniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.RenderedUniversalGltfRenderer;
import com.timlee9024.crgltf.config.ModConfig;
import com.timlee9024.crgltf.gl.GltfMaterialConverterPackManager;
import com.timlee9024.crgltf.gl.constants.GltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcJointMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import com.timlee9024.crgltf.gl.constants.GltfMorphingPassConstants;
import com.timlee9024.crgltf.gl.constants.OptiFineShaderRenderConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedTextureModel;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, clientSideOnly = true, useMetadata = true, guiFactory = Reference.PACKAGE + ".config.ModConfigGuiFactory")
public class CRglTFMod {

	public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

	private static CRglTFMod INSTANCE;

	public CRglTFMod() {
		INSTANCE = this;
	}

	public static CRglTFMod getInstance() {
		return INSTANCE;
	}

	@EventHandler
	public void onEvent(FMLPreInitializationEvent event) {
		ModConfig modConfig = new ModConfig();
		modConfig.onEvent(event);

		DefaultRenderedTextureModel.initTextureConstants();

		new GltfMaterialToTextureConstants().onEvent(event);
		new VanillaRenderConstants().onEvent(event);

		RenderedUniversalGltfRenderer<?, ?> renderedUniversalGltfRenderer;
		String openGlAvailability = modConfig.getOpenGlAvailability().getString();
		if ("Full".equals(openGlAvailability) || (!"GL21_FBO".equals(openGlAvailability) && GL.getCapabilities().OpenGL43)) {
			new GltfMorphingPassConstants().onEvent(event);
			new GltfCalcJointMatrixPassConstants().onEvent(event);
			new GltfCalcSkinMatrixPassConstants().onEvent(event);
			new GltfApplySkinMatrixPassConstants().onEvent(event);

			if (FMLClientHandler.instance().hasOptifine()) {
				new OptiFineShaderRenderConstants().onEvent(event);
				new GltfMaterialConverterPackManager().onEvent(event);

				renderedUniversalGltfRenderer = new DefaultOptiFineUniversalGltfRenderer();
			} else {
				renderedUniversalGltfRenderer = new DefaultUniversalGltfRenderer();
			}
			logger.info("Init UniversalGltfRenderer completed with Full OpenGL Features.");
		} else {
			if (FMLClientHandler.instance().hasOptifine()) {
				new OptiFineShaderRenderConstants().onEvent(event);
				new GltfMaterialConverterPackManager().onEvent(event);

				renderedUniversalGltfRenderer = new GL21OptiFineUniversalGltfRenderer();
			} else {
				renderedUniversalGltfRenderer = new GL21UniversalGltfRenderer();
			}
			logger.info("Init UniversalGltfRenderer completed with Minimal OpenGL Features.");
		}

		new CRglTFApi() {
			@Override
			public void onEvent(FMLLoadCompleteEvent event) {
				renderedUniversalGltfRenderer.onEvent(event);
			}

			@Override
			public UniversalGltfRenderer getUniversalGltfRenderer() {
				return renderedUniversalGltfRenderer;
			}
		}.onEvent(event);
	}

	@EventHandler
	public void onEvent(FMLLoadCompleteEvent event) {
		CRglTFApi.getInstance().onEvent(event);
	}

}