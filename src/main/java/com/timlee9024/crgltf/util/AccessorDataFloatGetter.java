package com.timlee9024.crgltf.util;

import de.javagl.jgltf.model.AccessorByteData;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorIntData;
import de.javagl.jgltf.model.AccessorShortData;

@FunctionalInterface
public interface AccessorDataFloatGetter {

	float getFloat(int elementIndex, int componentIndex);

	static AccessorDataFloatGetter createDequantized(AccessorData accessorData) {
		return switch (accessorData) {
			case AccessorByteData accessorByteData -> createDequantized(accessorByteData);
			case AccessorShortData accessorShortData -> createDequantized(accessorShortData);
			case AccessorIntData accessorIntData -> createDequantized(accessorIntData);
			case AccessorFloatData accessorFloatData -> accessorFloatData::get;
			case null, default -> throw new IllegalArgumentException(""); //TODO: Throw proper exception.
		};
	}

	static AccessorDataFloatGetter createDequantized(AccessorByteData accessorData) {
		if (accessorData.isUnsigned()) {
			return (elementIndex, componentIndex) -> Byte.toUnsignedInt(accessorData.get(elementIndex, componentIndex)) / 255.F;
		} else {
			return (elementIndex, componentIndex) -> Math.max(accessorData.get(elementIndex, componentIndex) / (float) Byte.MAX_VALUE, -1.0F);
		}
	}

	static AccessorDataFloatGetter createDequantized(AccessorShortData accessorData) {
		if (accessorData.isUnsigned()) {
			return (elementIndex, componentIndex) -> Short.toUnsignedInt(accessorData.get(elementIndex, componentIndex)) / 65535.F;
		} else {
			return (elementIndex, componentIndex) -> Math.max(accessorData.get(elementIndex, componentIndex) / (float) Short.MAX_VALUE, -1.0F);
		}
	}

	static AccessorDataFloatGetter createDequantized(AccessorIntData accessorData) {
		if (accessorData.isUnsigned()) {
			return (elementIndex, componentIndex) -> Integer.divideUnsigned(accessorData.get(elementIndex, componentIndex), -1);
		} else {
			return (elementIndex, componentIndex) -> Math.max(accessorData.get(elementIndex, componentIndex) / (float) Integer.MAX_VALUE, -1.0F);
		}
	}
}
