package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.daxshader.DaxShaderRenderedGltfModel;
import com.timlee9024.crgltf.gl.rendered.impl.CommonNodeAccessor;
import com.timlee9024.crgltf.gl.rendered.impl.DefaultRenderedGltfModel;

public class DefaultDaxShaderRenderedGltfModel extends DefaultRenderedGltfModel implements DaxShaderRenderedGltfModel {

	public DefaultDaxShaderRenderedSceneModel[] daxShaderRenderedSceneModels;

	@Override
	public void renderSceneForDaxShader(int scene) {
		for (CommonNodeAccessor rootNodeAccessor : rootNodeAccessors) rootNodeAccessor.calculateGlobalTransform();
		daxShaderRenderedSceneModels[scene].renderNodeModelsForDaxShader();
		for (CommonNodeAccessor rootNodeAccessor : rootNodeAccessors) rootNodeAccessor.resetGlobalTransform();
	}
}
