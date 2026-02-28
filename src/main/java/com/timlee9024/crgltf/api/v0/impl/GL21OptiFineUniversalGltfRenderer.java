package com.timlee9024.crgltf.api.v0.impl;

import com.timlee9024.crgltf.gl.GltfMaterialConverterPackManager;
import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import com.timlee9024.crgltf.gl.rendered.daxshader.impl.DefaultDaxShaderRenderedMaterialModelCreator;
import com.timlee9024.crgltf.gl.rendered.daxshader.impl.GL21DaxShaderRenderedGltfModel;
import com.timlee9024.crgltf.gl.rendered.daxshader.impl.GL21DaxShaderRenderedGltfModelCreator;
import com.timlee9024.crgltf.gl.rendered.daxshader.impl.GL21DaxShaderRenderedMeshModelCreator;
import com.timlee9024.crgltf.gl.rendered.daxshader.impl.GL21DaxShaderRenderedMeshPrimitiveModelCreator;
import com.timlee9024.crgltf.gl.rendered.daxshader.impl.GL21DaxShaderRenderedNodeModelCreator;
import com.timlee9024.crgltf.gl.rendered.daxshader.impl.GL21DaxShaderRenderedSceneModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.CombinedRenderedTextureModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultGltfAnimationPlayer;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultGltfAnimationPlayerCreator;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedTextureModelCreator;
import de.javagl.jgltf.model.GltfModel;
import net.minecraft.client.renderer.OpenGlHelper;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.Collection;

public class GL21OptiFineUniversalGltfRenderer extends RenderedUniversalGltfRenderer<GL21DaxShaderRenderedGltfModel, DefaultGltfAnimationPlayer> {

	@Override
	protected GL21DaxShaderRenderedGltfModel[] createRenderedGltfModelsArray(int length) {
		return new GL21DaxShaderRenderedGltfModel[length];
	}

	@Override
	protected DefaultGltfAnimationPlayer[] createGltfAnimationPlayerArray(int length) {
		return new DefaultGltfAnimationPlayer[length];
	}

	@Override
	protected void setupGltfModelForRender(Collection<ListenerGroup> listenerGroups) {
		GL21DaxShaderRenderedGltfModelCreator renderedGltfModelCreator = new GL21DaxShaderRenderedGltfModelCreator();
		renderedGltfModelCreator.renderedSceneModelCreator = new GL21DaxShaderRenderedSceneModelCreator();
		renderedGltfModelCreator.renderedNodeModelCreator = new GL21DaxShaderRenderedNodeModelCreator();
		renderedGltfModelCreator.renderedNodeModelCreator.renderedMeshModelCreator = new GL21DaxShaderRenderedMeshModelCreator();
		renderedGltfModelCreator.renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator = new GL21DaxShaderRenderedMeshPrimitiveModelCreator();

		DefaultDaxShaderRenderedMaterialModelCreator renderedMaterialModelCreator = new DefaultDaxShaderRenderedMaterialModelCreator();
		renderedGltfModelCreator.renderedMaterialModelCreator = renderedMaterialModelCreator;
		renderedMaterialModelCreator.combinedRenderedTextureModelCreator = new CombinedRenderedTextureModelCreator();
		renderedMaterialModelCreator.renderedTextureModelCreator = new DefaultRenderedTextureModelCreator();

		renderedMaterialModelCreator.colorTextureProgram = GltfMaterialConverterPackManager.getInstance().getColorTextureProgram();
		renderedMaterialModelCreator.normalTextureProgram = GltfMaterialConverterPackManager.getInstance().getNormalTextureProgram();
		renderedMaterialModelCreator.specularTextureProgram = GltfMaterialConverterPackManager.getInstance().getSpecularTextureProgram();

		DefaultGltfAnimationPlayerCreator gltfAnimationPlayerCreator = new DefaultGltfAnimationPlayerCreator();

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);

		int currentFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
		int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

		OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, GltfMaterialToTextureConstants.getInstance().getGlCanvasFramebuffer());
		for (ListenerGroup listenerGroup : listenerGroups) {
			if (listenerGroup.compiledGltfModel == null) continue;
			renderedMaterialModelCreator.renderedTextureModelCreator.pixelDataLookup = renderedMaterialModelCreator.combinedRenderedTextureModelCreator.pixelDataLookup = listenerGroup.compiledGltfModel.getPixelDataLookup();
			renderedMaterialModelCreator.combinedRenderedTextureModelCreator.glTextures = renderedMaterialModelCreator.renderedTextureModelCreator.glTextures = new OpenGLObjectRefSet();
			renderedGltfModelCreator.renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.glBuffers = new OpenGLObjectRefSet();
			GltfModel gltfModel = listenerGroup.compiledGltfModel.getGltfModel();
			gltfAnimationPlayers[listenerGroup.modelId] = gltfAnimationPlayerCreator.create(gltfModel, renderedGltfModels[listenerGroup.modelId] = (GL21DaxShaderRenderedGltfModel) renderedGltfModelCreator.create(gltfModel));
		}

		OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, currentFramebuffer);
		GL20.glUseProgram(currentProgram);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL11.glPopAttrib();
	}

	@Override
	public void render(int modelId, int scene) {
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

		//These render setup is required to match the default value of glTF spec.
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); //The default value if COLOR_0 does not specify.

		int activeProgramID = Shaders.activeProgramID;
		if (activeProgramID == 0) {
			//For render in vanilla fixed function pipeline.
			GL11.glEnable(GL12.GL_RESCALE_NORMAL); //In case of render as Entity.
			GL11.glEnable(GL11.GL_LIGHTING); //In case of render in GUI.
			GL11.glShadeModel(GL11.GL_SMOOTH);
			VanillaRenderConstants.getInstance().setupEmissiveTexEnv();

			renderedGltfModels[modelId].renderScene(scene);
		} else {
			renderedGltfModels[modelId].renderSceneForDaxShader(scene);
		}

		GL11.glPopAttrib();
	}
}
