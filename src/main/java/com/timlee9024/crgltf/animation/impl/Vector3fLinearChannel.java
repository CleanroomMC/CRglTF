package com.timlee9024.crgltf.animation.impl;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Vector3fLinearChannel extends KeyframeAnimationChannel<Vector3f> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final Vector3fc[] values;

	public Vector3fLinearChannel(float[] timesS, Vector3fc[] values) {
		super(timesS);
		this.values = values;
	}

	@Override
	public void update(float timeS) {
		if (timeS <= timesS[0]) {
			target.set(values[0]);
		} else if (timeS >= timesS[timesS.length - 1]) {
			target.set(values[values.length - 1]);
		} else {
			int previousIndex = computeIndex(timeS);
			int nextIndex = previousIndex + 1;

			float local = timeS - timesS[previousIndex];
			float delta = timesS[nextIndex] - timesS[previousIndex];
			float alpha = local / delta;

			values[previousIndex].lerp(values[nextIndex], alpha, target);
		}
	}

}
