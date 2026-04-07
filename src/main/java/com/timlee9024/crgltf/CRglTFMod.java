package com.timlee9024.crgltf;

import com.timlee9024.crgltf.api.v0.CRglTFApi;
import com.timlee9024.crgltf.api.v0.UniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.DefaultOptiFineUniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.DefaultUniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.GL21ExtDaxShaderUniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.GL21ExtUniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.GL21OptiFineUniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.GL21UniversalGltfRenderer;
import com.timlee9024.crgltf.api.v0.impl.RenderedUniversalGltfRenderer;
import com.timlee9024.crgltf.config.ModConfig;
import com.timlee9024.crgltf.gl.GL30Abstraction;
import com.timlee9024.crgltf.gl.GL31Abstraction;
import com.timlee9024.crgltf.gl.GltfMaterialConverterPackManager;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfApplySkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfCalcJointMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfCalcSkinMatrixPassConstants;
import com.timlee9024.crgltf.gl.constants.GL21ExtGltfMorphingPassConstants;
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
import org.lwjgl.opengl.GLCapabilities;

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

		GLCapabilities glCapabilities = GL.getCapabilities();

		logger.info("OpenGL features available [OpenGL43: {}, OpenGL31: {}, OpenGL30: {}, GL_ARB_uniform_buffer_object: {}, GL_ARB_vertex_array_object: {}, GL_EXT_bindable_uniform: {}, GL_EXT_transform_feedback: {}, GL_APPLE_vertex_array_object: {}]",
				glCapabilities.OpenGL43,
				glCapabilities.OpenGL31,
				glCapabilities.OpenGL30,
				glCapabilities.GL_ARB_uniform_buffer_object,
				glCapabilities.GL_ARB_vertex_array_object,
				glCapabilities.GL_EXT_bindable_uniform,
				glCapabilities.GL_EXT_transform_feedback,
				false);

		switch (modConfig.getOpenGlAvailability().getString()) {
			case "Full":
				initFullProfile(event);
				break;
			case "GL21_EXT":
				GL31Abstraction.openGL31UniformBufferObject = glCapabilities.GL_ARB_uniform_buffer_object;
				GL30Abstraction.openGL30VertexArrayObject = glCapabilities.GL_ARB_vertex_array_object;
				GL30Abstraction.openGL30TransformFeedback = glCapabilities.OpenGL30;
				initGL21ExtProfile(event);
				break;
			case "GL21_FBO":
				initGL21FboProfile(event);
				break;
			default:
				if (glCapabilities.OpenGL43) {
					initFullProfile(event);
				} else if (glCapabilities.GL_ARB_uniform_buffer_object) {
					// Despite OpenGL30 and OpenGL31 from GLCapabilities are both reported as available on Android Launcher with GL4ES, in reality they are not.
					// By checking the ARB extension should mitigate this issue.
					GL31Abstraction.openGL31UniformBufferObject = true;
					GL30Abstraction.openGL30VertexArrayObject = glCapabilities.GL_ARB_vertex_array_object;
					GL30Abstraction.openGL30TransformFeedback = glCapabilities.OpenGL30;

					initGL21ExtProfile(event);
				} else if (glCapabilities.GL_EXT_bindable_uniform) {
					if (glCapabilities.GL_ARB_vertex_array_object) {
						GL30Abstraction.openGL30VertexArrayObject = true;
						GL30Abstraction.openGL30TransformFeedback = glCapabilities.OpenGL30;

						if (glCapabilities.OpenGL30 || glCapabilities.GL_EXT_transform_feedback) {
							initGL21ExtProfile(event);
						} else {
							initGL21FboProfile(event);
						}
//					https://github.com/LWJGL/lwjgl3/issues/1105
//					} else if (glCapabilities.GL_APPLE_vertex_array_object) {
//						if (glCapabilities.OpenGL30 || glCapabilities.GL_EXT_transform_feedback) {
//							initGL21ExtProfile(event);
//						} else {
//							initGL21FboProfile(event);
//						}
					} else {
						initGL21FboProfile(event);
					}
				} else {
					initGL21FboProfile(event);
				}
				break;
		}
	}

	@EventHandler
	public void onEvent(FMLLoadCompleteEvent event) {
		CRglTFApi.getInstance().onEvent(event);
	}

	protected void initFullProfile(FMLPreInitializationEvent event) {
		new GltfMorphingPassConstants().onEvent(event);
		new GltfCalcJointMatrixPassConstants().onEvent(event);
		new GltfCalcSkinMatrixPassConstants().onEvent(event);
		new GltfApplySkinMatrixPassConstants().onEvent(event);

		RenderedUniversalGltfRenderer<?, ?> renderedUniversalGltfRenderer;
		if (FMLClientHandler.instance().hasOptifine()) {
			new OptiFineShaderRenderConstants().onEvent(event);
			new GltfMaterialConverterPackManager().onEvent(event);

			renderedUniversalGltfRenderer = new DefaultOptiFineUniversalGltfRenderer();
		} else {
			renderedUniversalGltfRenderer = new DefaultUniversalGltfRenderer();
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

		logger.info("Init UniversalGltfRenderer completed with Full OpenGL Features.");
	}

	protected void initGL21ExtProfile(FMLPreInitializationEvent event) {
		new GL21ExtGltfMorphingPassConstants().onEvent(event);
		new GL21ExtGltfCalcJointMatrixPassConstants().onEvent(event);
		new GL21ExtGltfCalcSkinMatrixPassConstants().onEvent(event);
		new GL21ExtGltfApplySkinMatrixPassConstants().onEvent(event);

		RenderedUniversalGltfRenderer<?, ?> renderedUniversalGltfRenderer;
		if (FMLClientHandler.instance().hasOptifine()) {
			new OptiFineShaderRenderConstants().onEvent(event);
			new GltfMaterialConverterPackManager().onEvent(event);

			renderedUniversalGltfRenderer = new GL21ExtDaxShaderUniversalGltfRenderer();
		} else {
			renderedUniversalGltfRenderer = new GL21ExtUniversalGltfRenderer();
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

		logger.info("Init UniversalGltfRenderer completed with OpenGL 2.1 and extensions profile.");
	}

	protected void initGL21FboProfile(FMLPreInitializationEvent event) {
		RenderedUniversalGltfRenderer<?, ?> renderedUniversalGltfRenderer;
		if (FMLClientHandler.instance().hasOptifine()) {
			new OptiFineShaderRenderConstants().onEvent(event);
			new GltfMaterialConverterPackManager().onEvent(event);

			renderedUniversalGltfRenderer = new GL21OptiFineUniversalGltfRenderer();
		} else {
			renderedUniversalGltfRenderer = new GL21UniversalGltfRenderer();
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

		logger.info("Init UniversalGltfRenderer completed with OpenGL 2.1 and Framebuffer Object profile.");
	}

}