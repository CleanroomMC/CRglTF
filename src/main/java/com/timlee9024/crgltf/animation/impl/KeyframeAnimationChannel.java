package com.timlee9024.crgltf.animation.impl;

import com.timlee9024.crgltf.animation.AnimationChannel;

import java.util.Arrays;

public abstract class KeyframeAnimationChannel<T> implements AnimationChannel {

	/**
	 * The key frame times, in seconds
	 */
	protected final float[] timesS;

	protected T target;

	public KeyframeAnimationChannel(float[] timesS) {
		this.timesS = timesS;
	}

	public T getTarget() {
		return target;
	}

	public void setTarget(T target) {
		this.target = target;
	}

	/**
	 * Compute the index of the segment that the given key belongs to.
	 * If the given key is smaller than the smallest or larger than
	 * the largest key, then 0 or <code>keys.length-1<code> will be returned,
	 * respectively.
	 *
	 * @param key The key
	 * @return The index for the key
	 */
	protected int computeIndex(float key) {
		int index = Arrays.binarySearch(timesS, key);
		if (index >= 0) {
			return index;
		}
		return Math.max(0, -index - 2);
	}
}
