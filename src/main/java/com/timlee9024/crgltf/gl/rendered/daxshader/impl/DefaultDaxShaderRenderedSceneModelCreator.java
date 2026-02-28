package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedSceneModel;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedSceneModelCreator;
import de.javagl.jgltf.model.SceneModel;

public class DefaultDaxShaderRenderedSceneModelCreator extends DefaultRenderedSceneModelCreator {

	@Override
	public DefaultRenderedSceneModel create(SceneModel sceneModel) {
		return new DefaultDaxShaderRenderedSceneModel();
	}

}
