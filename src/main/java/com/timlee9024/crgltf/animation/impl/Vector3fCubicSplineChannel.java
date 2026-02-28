package com.timlee9024.crgltf.animation.impl;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Vector3fCubicSplineChannel extends KeyframeAnimationChannel<Vector3f> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final Vector3fc[][] values;

	public Vector3fCubicSplineChannel(float[] timesS, Vector3fc[][] values) {
		super(timesS);
		this.values = values;
	}

	@Override
	public void update(float timeS) {
		if (timeS <= timesS[0]) {
			target.set(values[0][1]);
		} else if (timeS >= timesS[timesS.length - 1]) {
			target.set(values[values.length - 1][1]);
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

			Vector3fc[] previous = values[previousIndex];
			Vector3fc[] next = values[nextIndex];

			previous[1].mul(aa, target).fma(ab, previous[2]).fma(ac, next[1]).fma(ad, next[0]);
		}
	}

}
