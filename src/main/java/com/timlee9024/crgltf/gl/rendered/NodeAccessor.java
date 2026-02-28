package com.timlee9024.crgltf.gl.rendered;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface NodeAccessor {

	Matrix4f ZERO_MATRIX = new Matrix4f().zero();

	//boolean isTransformMatrixOrTRS();

	//void setTransformMatrixOrTRS(boolean isTransformMatrixOrTRS);

	Matrix4fc getGlobalTransformMatrix();

	boolean isGlobalTransformZeroMatrix();

	boolean isTransformMatrixModified();

	void setTransformMatrixModified(boolean isTransformMatrixModified);

	void setTransformMatrix(Matrix4fc transformMatrix);

	Matrix4fc getTransformMatrix();

	boolean isTRSModified();

	void setTRSModified(boolean isTRSModified);

	Vector3f getTranslation();

	Quaternionf getRotation();

	Vector3f getScale();

	void setWeights(float[] weights);

	float[] getWeights();
}
