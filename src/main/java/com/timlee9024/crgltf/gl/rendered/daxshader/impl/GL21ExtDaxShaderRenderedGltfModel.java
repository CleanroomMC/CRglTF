package com.timlee9024.crgltf.gl.rendered.daxshader.impl;

import com.timlee9024.crgltf.gl.rendered.daxshader.DaxShaderRenderedGltfModel;
import com.timlee9024.crgltf.gl.rendered.impl.CommonNodeAccessor;
import com.timlee9024.crgltf.gl.rendered.impl.GL21ExtRenderedGltfModel;

public class GL21ExtDaxShaderRenderedGltfModel extends GL21ExtRenderedGltfModel implements DaxShaderRenderedGltfModel {

	public GL21ExtDaxShaderRenderedSceneModel[] daxShaderRenderedSceneModels;

	@Override
	public void renderSceneForDaxShader(int scene) {
		for (CommonNodeAccessor rootNodeAccessor : rootNodeAccessors) rootNodeAccessor.calculateGlobalTransform();
		daxShaderRenderedSceneModels[scene].renderNodeModelsForDaxShader();
		for (CommonNodeAccessor rootNodeAccessor : rootNodeAccessors) rootNodeAccessor.resetGlobalTransform();
	}
}
