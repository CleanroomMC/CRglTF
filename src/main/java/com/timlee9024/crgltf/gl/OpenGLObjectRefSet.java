package com.timlee9024.crgltf.gl;

import java.util.function.IntConsumer;

public class OpenGLObjectRefSet {

	protected int[][] startAndEnds;
	protected int[] current;

	public void add(int objectRef) {
		if (startAndEnds == null) {
			startAndEnds = new int[1][2];
			current = startAndEnds[0];
			current[0] = objectRef;
			current[1] = objectRef + 1;
		} else {
			if (Integer.compareUnsigned(current[1], objectRef) == 0) {
				current[1] = objectRef + 1;
			} else {
				int[][] startAndEndsNew = new int[startAndEnds.length + 1][];
				for (int i = 0; i < startAndEnds.length; i++) {
					startAndEndsNew[i] = startAndEnds[i];
				}
				startAndEndsNew[startAndEnds.length] = current = new int[]{objectRef, objectRef + 1};
				startAndEnds = startAndEndsNew;
			}
		}
	}

	public void forEach(IntConsumer consumer) {
		if (startAndEnds == null) return;
		for (int i = 0; i < startAndEnds.length; i++) {
			int[] startAndEnd = startAndEnds[i];
			for (int j = startAndEnd[0]; j < startAndEnd[1]; j++) {
				consumer.accept(j);
			}
		}
	}
}
