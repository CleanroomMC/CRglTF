package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedSceneModel;
import com.timlee9024.crgltf.gl.rendered.impl.GL21RenderedSceneModelCreator;
import de.javagl.jgltf.model.SceneModel;

public class GL21DaxShaderRenderedSceneModelCreator extends GL21RenderedSceneModelCreator {
	@Override
	public GL21RenderedSceneModel create(SceneModel sceneModel) {
		return new GL21DaxShaderRenderedSceneModel();
	}
}
