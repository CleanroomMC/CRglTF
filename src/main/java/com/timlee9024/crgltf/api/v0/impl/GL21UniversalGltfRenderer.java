package com.timlee9024.crgltf.api.v0.impl;

import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import com.timlee9024.crgltf.gl.constants.VanillaRenderConstants;
import com.timlee9024.crgltf.gl.rendered.impl.CombinedRenderedTextureModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultGltfAnimationPlayer;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultGltfAnimationPlayerCreator;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedMaterialModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedGltfModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedGltfModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedMeshPrimitiveModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedNodeModelCreator;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedSceneModelCreator;
import de.javagl.jgltf.model.GltfModel;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.Collection;

public class GL21UniversalGltfRenderer extends RenderedUniversalGltfRenderer<GL21RenderedGltfModel, DefaultGltfAnimationPlayer> {

	@Override
	protected GL21RenderedGltfModel[] createRenderedGltfModelsArray(int length) {
		return new GL21RenderedGltfModel[length];
	}

	@Override
	protected DefaultGltfAnimationPlayer[] createGltfAnimationPlayerArray(int length) {
		return new DefaultGltfAnimationPlayer[length];
	}

	@Override
	protected void setupGltfModelForRender(Collection<ListenerGroup> listenerGroups) {
		GL21RenderedGltfModelCreator renderedGltfModelCreator = new GL21RenderedGltfModelCreator();
		renderedGltfModelCreator.renderedSceneModelCreator = new GL21RenderedSceneModelCreator();
		renderedGltfModelCreator.renderedNodeModelCreator = new GL21RenderedNodeModelCreator();
		renderedGltfModelCreator.renderedNodeModelCreator.renderedMeshModelCreator = new GL21RenderedMeshModelCreator();
		renderedGltfModelCreator.renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator = new GL21RenderedMeshPrimitiveModelCreator();

		DefaultRenderedMaterialModelCreator renderedMaterialModelCreator = new DefaultRenderedMaterialModelCreator();
		renderedGltfModelCreator.renderedMaterialModelCreator = renderedMaterialModelCreator;
		renderedMaterialModelCreator.combinedRenderedTextureModelCreator = new CombinedRenderedTextureModelCreator();

		DefaultGltfAnimationPlayerCreator gltfAnimationPlayerCreator = new DefaultGltfAnimationPlayerCreator();

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);

		int currentFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
		int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

		OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, GltfMaterialToTextureConstants.getInstance().getGlCanvasFramebuffer());
		for (ListenerGroup listenerGroup : listenerGroups) {
			if (listenerGroup.compiledGltfModel == null) continue;
			renderedMaterialModelCreator.combinedRenderedTextureModelCreator.pixelDataLookup = listenerGroup.compiledGltfModel.getPixelDataLookup();
			renderedMaterialModelCreator.combinedRenderedTextureModelCreator.glTextures = new OpenGLObjectRefSet();
			renderedGltfModelCreator.renderedNodeModelCreator.renderedMeshModelCreator.renderedMeshPrimitiveModelCreator.glBuffers = new OpenGLObjectRefSet();
			GltfModel gltfModel = listenerGroup.compiledGltfModel.getGltfModel();
			gltfAnimationPlayers[listenerGroup.modelId] = gltfAnimationPlayerCreator.create(gltfModel, renderedGltfModels[listenerGroup.modelId] = renderedGltfModelCreator.create(gltfModel));
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

		//For render in vanilla fixed function pipeline.
		GL11.glEnable(GL12.GL_RESCALE_NORMAL); //In case of render as Entity.
		GL11.glEnable(GL11.GL_LIGHTING); //In case of render in GUI.
		GL11.glShadeModel(GL11.GL_SMOOTH);
		VanillaRenderConstants.getInstance().setupEmissiveTexEnv();

		renderedGltfModels[modelId].renderScene(scene);

		GL11.glPopAttrib();
	}
}
