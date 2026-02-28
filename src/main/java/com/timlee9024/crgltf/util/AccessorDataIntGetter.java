package com.timlee9024.crgltf.util;

import de.javagl.jgltf.model.AccessorByteData;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorIntData;
import de.javagl.jgltf.model.AccessorShortData;

@FunctionalInterface
public interface AccessorDataIntGetter {

	int getInt(int elementIndex, int componentIndex);

	static AccessorDataIntGetter createDequantized(AccessorData accessorData) {
		return switch (accessorData) {
			case AccessorByteData accessorByteData -> createDequantized(accessorByteData);
			case AccessorShortData accessorShortData -> createDequantized(accessorShortData);
			case AccessorIntData accessorIntData -> accessorIntData::get;
			case null, default -> throw new IllegalArgumentException(""); //TODO: Throw proper exception.
		};
	}

	static AccessorDataIntGetter createDequantized(AccessorByteData accessorData) {
		if (accessorData.isUnsigned()) {
			return (elementIndex, componentIndex) -> Byte.toUnsignedInt(accessorData.get(elementIndex, componentIndex));
		} else {
			return accessorData::get;
		}
	}

	static AccessorDataIntGetter createDequantized(AccessorShortData accessorData) {
		if (accessorData.isUnsigned()) {
			return (elementIndex, componentIndex) -> Short.toUnsignedInt(accessorData.get(elementIndex, componentIndex));
		} else {
			return accessorData::get;
		}
	}

}
