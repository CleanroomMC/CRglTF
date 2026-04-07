package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.GL30Abstraction;
import com.timlee9024.crgltf.gl.OpenGLObjectRefSet;
import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import com.timlee9024.crgltf.gl.rendered.RenderedGltfModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public class GL21ExtRenderedGltfModel implements RenderedGltfModel {

	public OpenGLObjectRefSet glTextures = new OpenGLObjectRefSet();
	public OpenGLObjectRefSet glBufferViews = new OpenGLObjectRefSet();
	public OpenGLObjectRefSet glVertexArrays = new OpenGLObjectRefSet();

	public GL21ExtRenderedSceneModel[] renderedSceneModels;
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
		glVertexArrays.forEach(GL30Abstraction::glDeleteVertexArrays);
		glBufferViews.forEach(GL15::glDeleteBuffers);
		glTextures.forEach(GL11::glDeleteTextures);
	}
}
