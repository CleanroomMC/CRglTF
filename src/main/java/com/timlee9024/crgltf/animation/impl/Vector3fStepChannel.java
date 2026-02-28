package com.timlee9024.crgltf.animation.impl;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Vector3fStepChannel extends KeyframeAnimationChannel<Vector3f> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final Vector3fc[] values;

	public Vector3fStepChannel(float[] timesS, Vector3fc[] values) {
		super(timesS);
		this.values = values;
	}

	@Override
	public void update(float timeS) {
		target.set(values[computeIndex(timeS)]);
	}

}
