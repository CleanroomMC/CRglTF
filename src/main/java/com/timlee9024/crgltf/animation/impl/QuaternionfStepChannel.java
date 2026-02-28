package com.timlee9024.crgltf.animation.impl;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class QuaternionfStepChannel extends KeyframeAnimationChannel<Quaternionf> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final Quaternionfc[] values;

	public QuaternionfStepChannel(float[] timesS, Quaternionfc[] values) {
		super(timesS);
		this.values = values;
	}

	@Override
	public void update(float timeS) {
		target.set(values[computeIndex(timeS)]);
	}

}
