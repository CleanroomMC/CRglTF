package com.timlee9024.crgltf.animation.impl;

public class MorphWeightsLinearChannel extends KeyframeAnimationChannel<float[]> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final float[][] values;

	public MorphWeightsLinearChannel(float[] timesS, float[][] values) {
		super(timesS);
		this.values = values;
	}

	@Override
	public void update(float timeS) {
		if (timeS <= timesS[0]) {
			System.arraycopy(values[0], 0, target, 0, target.length);
		} else if (timeS >= timesS[timesS.length - 1]) {
			System.arraycopy(values[timesS.length - 1], 0, target, 0, target.length);
		} else {
			int previousIndex = computeIndex(timeS);
			int nextIndex = previousIndex + 1;

			float local = timeS - timesS[previousIndex];
			float delta = timesS[nextIndex] - timesS[previousIndex];
			float alpha = local / delta;

			float[] previousPoint = values[previousIndex];
			float[] nextPoint = values[nextIndex];

			for (int i = 0; i < target.length; i++) {
				float p = previousPoint[i];
				float n = nextPoint[i];
				target[i] = Math.fma(alpha, (n - p), p);
			}
		}
	}

}
