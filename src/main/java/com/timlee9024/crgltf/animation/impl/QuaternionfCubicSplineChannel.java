package com.timlee9024.crgltf.animation.impl;

import org.joml.Quaternionf;
import org.joml.Vector4fc;

public class QuaternionfCubicSplineChannel extends KeyframeAnimationChannel<Quaternionf> {

	/**
	 * The values. Each element of this array corresponds to one key
	 * frame time
	 */
	protected final Vector4fc[][] values;

	public QuaternionfCubicSplineChannel(float[] timesS, Vector4fc[][] values) {
		super(timesS);
		this.values = values;
	}

	@Override
	public void update(float timeS) {
		if (timeS <= timesS[0]) {
			Vector4fc value = values[0][1];
			target.set(value.x(), value.y(), value.z(), value.w()).normalize();
		} else if (timeS >= timesS[timesS.length - 1]) {
			Vector4fc value = values[values.length - 1][1];
			target.set(value.x(), value.y(), value.z(), value.w()).normalize();
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

			Vector4fc[] previous = values[previousIndex];
			Vector4fc[] next = values[nextIndex];

			Vector4fc previousPoint = previous[1];
			Vector4fc previousOutputTangent = previous[2];
			Vector4fc nextPoint = next[1];
			Vector4fc nextInputTangent = next[0];

			target.set(Math.fma(previousPoint.x(), aa, Math.fma(previousOutputTangent.x(), ab, Math.fma(nextPoint.x(), ac, nextInputTangent.x() * ad))),
					Math.fma(previousPoint.y(), aa, Math.fma(previousOutputTangent.y(), ab, Math.fma(nextPoint.y(), ac, nextInputTangent.y() * ad))),
					Math.fma(previousPoint.z(), aa, Math.fma(previousOutputTangent.z(), ab, Math.fma(nextPoint.z(), ac, nextInputTangent.z() * ad))),
					Math.fma(previousPoint.w(), aa, Math.fma(previousOutputTangent.w(), ab, Math.fma(nextPoint.w(), ac, nextInputTangent.w() * ad)))).normalize();
		}
	}

}
