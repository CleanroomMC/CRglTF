package com.timlee9024.crgltf.animation.impl;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class QuaternionfLinearChannel extends KeyframeAnimationChannel<Quaternionf> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final Quaternionfc[] values;

	public QuaternionfLinearChannel(float[] timesS, Quaternionfc[] values) {
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

			values[previousIndex].slerp(values[nextIndex], alpha, target);
		}
	}

}
