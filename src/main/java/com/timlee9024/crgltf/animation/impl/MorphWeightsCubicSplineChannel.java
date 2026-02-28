package com.timlee9024.crgltf.animation.impl;

public class MorphWeightsCubicSplineChannel extends KeyframeAnimationChannel<float[]> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final float[][][] values;

	public MorphWeightsCubicSplineChannel(float[] timesS, float[][][] values) {
		super(timesS);
		this.values = values;
	}

	@Override
	public void update(float timeS) {
		if (timeS <= timesS[0]) {
			System.arraycopy(values[0][1], 0, target, 0, target.length);
		} else if (timeS >= timesS[timesS.length - 1]) {
			System.arraycopy(values[timesS.length - 1][1], 0, target, 0, target.length);
		} else {
			// Adapted from https://github.khronos.org/glTF-Tutorials/gltfTutorial/gltfTutorial_007_Animations.html#cubic-spline-interpolation
			int previousIndex = computeIndex(timeS);
			int nextIndex = previousIndex + 1;

			float local = timeS - timesS[previousIndex];
			float delta = timesS[nextIndex] - timesS[previousIndex];
			float alpha = local / delta;
			float alpha2 = alpha * alpha;
			float alpha3 = alpha2 * alpha;

			float aa = Math.fma(2, alpha3, Math.fma(-3, alpha2, 1));
			float ab = (alpha3 + Math.fma(-2, alpha2, alpha)) * delta;
			float ac = Math.fma(-2, alpha3, 3 * alpha2);
			float ad = (alpha3 - alpha2) * delta;

			float[][] previous = values[previousIndex];
			float[][] next = values[nextIndex];

			float[] previousPoint = previous[1];
			float[] nextPoint = next[1];
			float[] previousOutputTangent = previous[2];
			float[] nextInputTangent = next[0];

			for (int i = 0; i < target.length; i++) {
				target[i] = Math.fma(previousPoint[i], aa, Math.fma(previousOutputTangent[i], ab, Math.fma(nextPoint[i], ac, nextInputTangent[i] * ad)));
			}
		}
	}

}
