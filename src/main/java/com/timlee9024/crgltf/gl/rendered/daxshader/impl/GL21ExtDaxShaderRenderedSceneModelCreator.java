package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedSceneModelCreator;
import de.javagl.jgltf.model.SceneModel;

public class GL21ExtDaxShaderRenderedSceneModelCreator extends GL21ExtRenderedSceneModelCreator {

	@Override
	public GL21ExtDaxShaderRenderedSceneModel create(SceneModel sceneModel) {
		return new GL21ExtDaxShaderRenderedSceneModel();
	}

}
