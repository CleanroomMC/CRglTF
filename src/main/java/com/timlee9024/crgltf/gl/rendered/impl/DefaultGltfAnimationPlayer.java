package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.animation.AnimationChannel;
import com.timlee9024.crgltf.gl.rendered.GltfAnimationPlayer;

public class DefaultGltfAnimationPlayer implements GltfAnimationPlayer {

	protected AnimationChannel[][] animations;

	@Override
	public void applyAnimation(int animation, float timeInSecond) {
		AnimationChannel[] animationChannels = animations[animation];
		for (AnimationChannel animationChannel : animationChannels) animationChannel.update(timeInSecond);
	}

}
