package com.timlee9024.crgltf.animation.impl;

public class MorphWeightsStepChannel extends KeyframeAnimationChannel<float[]> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final float[][] values;

	public MorphWeightsStepChannel(float[] timesS, float[][] values) {
		super(timesS);
		this.values = values;
	}

	@Override
	public void update(float timeS) {
		System.arraycopy(values[computeIndex(timeS)], 0, target, 0, target.length);
	}

}
