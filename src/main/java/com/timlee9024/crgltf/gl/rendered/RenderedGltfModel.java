package com.timlee9024.crgltf.gl.rendered;

public interface RenderedGltfModel {

	void renderScene(int scene);

	NodeAccessor getNodeAccessorByNode(int node);

	void deleteOpenGLData();
}
