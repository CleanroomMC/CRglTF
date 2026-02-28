package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import com.timlee9024.crgltf.gl.rendered.RenderedGltfModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class GL21RenderedGltfModel implements RenderedGltfModel {

	public OpenGLObjectRefSet glTextures = new OpenGLObjectRefSet();
	public OpenGLObjectRefSet glBufferViews = new OpenGLObjectRefSet();
	public FloatBuffer dynamicFloatBuffer;

	public GL21RenderedSceneModel[] renderedSceneModels;
	public CommonNodeAccessor[] nodeAccessors;
	public CommonNodeAccessor[] rootNodeAccessors;

	@Override
	public void renderScene(int scene) {
		for (CommonNodeAccessor rootNodeAccessor : rootNodeAccessors) rootNodeAccessor.calculateGlobalTransform();
		renderedSceneModels[scene].renderNodeModels();
		for (CommonNodeAccessor rootNodeAccessor : rootNodeAccessors) rootNodeAccessor.resetGlobalTransform();
	}

	@Override
	public NodeAccessor getNodeAccessorByNode(int node) {
		return nodeAccessors[node];
	}

	@Override
	public void deleteOpenGLData() {
		glBufferViews.forEach(GL15::glDeleteBuffers);
		glTextures.forEach(GL11::glDeleteTextures);
		MemoryUtil.memFree(dynamicFloatBuffer);
	}
}
