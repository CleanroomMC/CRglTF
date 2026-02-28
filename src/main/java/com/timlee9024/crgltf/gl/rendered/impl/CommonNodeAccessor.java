package com.timlee9024.crgltf.gl.rendered.impl;

import com.timlee9024.crgltf.gl.rendered.NodeAccessor;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class CommonNodeAccessor implements NodeAccessor {

	public CommonNodeAccessor[] children;

	protected final Matrix4f calculatedTransformMatrix = new Matrix4f();

	protected Matrix4fc globalTransformMatrix;

	protected Matrix4fc originalGlobalTransformMatrix;

	protected boolean isTransformMatrixModified;

	protected Matrix4fc transformMatrix;

	public Matrix4fc originalTransformMatrix;

	protected boolean isTRSModified;

	protected final Vector3f translation = new Vector3f();
	protected final Quaternionf rotation = new Quaternionf();
	protected final Vector3f scale = new Vector3f(1);

	public Vector3fc originalTranslation;
	public Quaternionfc originalRotation;
	public Vector3fc originalScale;

	@Override
	public Matrix4fc getGlobalTransformMatrix() {
		return globalTransformMatrix;
	}

	@Override
	public boolean isGlobalTransformZeroMatrix() {
		return globalTransformMatrix == ZERO_MATRIX;
	}

	@Override
	public boolean isTransformMatrixModified() {
		return isTransformMatrixModified;
	}

	@Override
	public void setTransformMatrixModified(boolean isTransformMatrixModified) {
		this.isTransformMatrixModified = isTransformMatrixModified;
	}

	@Override
	public void setTransformMatrix(Matrix4fc transformMatrix) {
		this.transformMatrix = transformMatrix;
	}

	@Override
	public Matrix4fc getTransformMatrix() {
		return transformMatrix;
	}

	@Override
	public boolean isTRSModified() {
		return isTRSModified;
	}

	@Override
	public void setTRSModified(boolean isTRSModified) {
		this.isTRSModified = isTRSModified;
	}

	@Override
	public Vector3f getTranslation() {
		return translation;
	}

	@Override
	public Quaternionf getRotation() {
		return rotation;
	}

	@Override
	public Vector3f getScale() {
		return scale;
	}

	@Override
	public void setWeights(float[] weights) {
	}

	@Override
	public float[] getWeights() {
		return null;
	}

	public Matrix4fc getOriginalGlobalTransformMatrix() {
		return originalGlobalTransformMatrix;
	}

	public void initGlobalTransform() {
		if (originalTransformMatrix != null) {
			if (originalTransformMatrix.m00() == 0 && originalTransformMatrix.m01() == 0 && originalTransformMatrix.m02() == 0
					&& originalTransformMatrix.m10() == 0 && originalTransformMatrix.m11() == 0 && originalTransformMatrix.m12() == 0
					&& originalTransformMatrix.m20() == 0 && originalTransformMatrix.m21() == 0 && originalTransformMatrix.m22() == 0) {
				globalTransformMatrix = originalGlobalTransformMatrix = ZERO_MATRIX;
			} else {
				globalTransformMatrix = originalGlobalTransformMatrix = originalTransformMatrix;
			}
		} else {
			if (scale.x == 0 && scale.y == 0 && scale.z == 0) {
				globalTransformMatrix = originalGlobalTransformMatrix = ZERO_MATRIX;
			} else {
				globalTransformMatrix = originalGlobalTransformMatrix = new Matrix4f().translationRotateScale(translation, rotation, scale);
			}
		}
	}

	public void initGlobalTransform(Matrix4fc parentMatrix) {
		if (parentMatrix == ZERO_MATRIX) {
			globalTransformMatrix = originalGlobalTransformMatrix = ZERO_MATRIX;
		} else {
			if (transformMatrix != null) {
				if (originalTransformMatrix.m00() == 0 && originalTransformMatrix.m01() == 0 && originalTransformMatrix.m02() == 0
						&& originalTransformMatrix.m10() == 0 && originalTransformMatrix.m11() == 0 && originalTransformMatrix.m12() == 0
						&& originalTransformMatrix.m20() == 0 && originalTransformMatrix.m21() == 0 && originalTransformMatrix.m22() == 0) {
					globalTransformMatrix = originalGlobalTransformMatrix = ZERO_MATRIX;
				} else {
					globalTransformMatrix = originalGlobalTransformMatrix = parentMatrix.mul(originalTransformMatrix, new Matrix4f());
				}
			} else {
				if (scale.x == 0 && scale.y == 0 && scale.z == 0) {
					globalTransformMatrix = originalGlobalTransformMatrix = ZERO_MATRIX;
				} else {
					globalTransformMatrix = originalGlobalTransformMatrix = parentMatrix.translate(translation, new Matrix4f()).rotate(rotation).scale(scale);
				}
			}
		}
	}

	public void resetTransformRecursively() {
		for (CommonNodeAccessor child : children) {
			child.resetTransformRecursively();
		}
		if (isTRSModified) {
			resetTRS();
			isTRSModified = false;
		}
		if (isTransformMatrixModified) {
			transformMatrix = originalTransformMatrix;
			isTransformMatrixModified = false;
		}
	}

	public void setGlobalTransformToZeroMatrixRecursively() {
		globalTransformMatrix = ZERO_MATRIX;
		for (CommonNodeAccessor child : children) {
			child.setGlobalTransformToZeroMatrixRecursively();
		}
		if (isTRSModified) {
			resetTRS();
			isTRSModified = false;
		}
		if (isTransformMatrixModified) {
			transformMatrix = originalTransformMatrix;
			isTransformMatrixModified = false;
		}
	}

	public void calculateGlobalTransform() {
		if (transformMatrix != null) {
			if (isTransformMatrixModified) {
				if (transformMatrix == ZERO_MATRIX) {
					globalTransformMatrix = ZERO_MATRIX;
					for (CommonNodeAccessor child : children) {
						child.setGlobalTransformToZeroMatrixRecursively();
					}
				} else {
					globalTransformMatrix = transformMatrix;
					for (CommonNodeAccessor children : children) {
						children.calculateGlobalTransformParentTransformed(this);
					}
				}
				transformMatrix = originalTransformMatrix;
				isTransformMatrixModified = false;
			} else {
				if (originalGlobalTransformMatrix == ZERO_MATRIX) {
					for (CommonNodeAccessor child : children) {
						child.resetTransformRecursively();
					}
				} else {
					for (CommonNodeAccessor children : children) {
						children.calculateGlobalTransform(this);
					}
				}
			}
			if (isTRSModified) {
				resetTRS();
				isTRSModified = false;
			}
		} else {
			if (isTRSModified) {
				if (scale.x == 0 && scale.y == 0 && scale.z == 0) {
					globalTransformMatrix = ZERO_MATRIX;
					for (CommonNodeAccessor children : children) {
						children.setGlobalTransformToZeroMatrixRecursively();
					}
				} else {
					globalTransformMatrix = calculatedTransformMatrix.translationRotateScale(translation, rotation, scale);
					for (CommonNodeAccessor children : children) {
						children.calculateGlobalTransformParentTransformed(this);
					}
				}
				resetTRS();
				isTRSModified = false;
			} else {
				if (originalGlobalTransformMatrix == ZERO_MATRIX) {
					for (CommonNodeAccessor child : children) {
						child.resetTransformRecursively();
					}
				} else {
					for (CommonNodeAccessor children : children) {
						children.calculateGlobalTransform(this);
					}
				}
			}
			if (isTransformMatrixModified) {
				transformMatrix = originalTransformMatrix;
				isTransformMatrixModified = false;
			}
		}
	}

	public void calculateGlobalTransform(CommonNodeAccessor parentNode) {
		if (transformMatrix != null) {
			if (isTransformMatrixModified) {
				if (transformMatrix == ZERO_MATRIX) {
					globalTransformMatrix = ZERO_MATRIX;
					for (CommonNodeAccessor children : children) {
						children.setGlobalTransformToZeroMatrixRecursively();
					}
				} else {
					globalTransformMatrix = parentNode.globalTransformMatrix.mul(transformMatrix, calculatedTransformMatrix);
					for (CommonNodeAccessor children : children) {
						children.calculateGlobalTransformParentTransformed(this);
					}
				}
				transformMatrix = originalTransformMatrix;
				isTransformMatrixModified = false;
			} else {
				if (originalGlobalTransformMatrix == ZERO_MATRIX) {
					for (CommonNodeAccessor child : children) {
						child.resetTransformRecursively();
					}
				} else {
					for (CommonNodeAccessor children : children) {
						children.calculateGlobalTransform(this);
					}
				}
			}
			if (isTRSModified) {
				resetTRS();
				isTRSModified = false;
			}
		} else {
			if (isTRSModified) {
				if (scale.x == 0 && scale.y == 0 && scale.z == 0) {
					globalTransformMatrix = ZERO_MATRIX;
					for (CommonNodeAccessor children : children) {
						children.setGlobalTransformToZeroMatrixRecursively();
					}
				} else {
					globalTransformMatrix = parentNode.globalTransformMatrix.translate(translation, calculatedTransformMatrix).rotate(rotation).scale(scale);
					for (CommonNodeAccessor children : children) {
						children.calculateGlobalTransformParentTransformed(this);
					}
				}
				resetTRS();
				isTRSModified = false;
			} else {
				if (originalGlobalTransformMatrix == ZERO_MATRIX) {
					for (CommonNodeAccessor child : children) {
						child.resetTransformRecursively();
					}
				} else {
					for (CommonNodeAccessor children : children) {
						children.calculateGlobalTransform(this);
					}
				}
			}
			if (isTransformMatrixModified) {
				transformMatrix = originalTransformMatrix;
				isTransformMatrixModified = false;
			}
		}
	}

	public void calculateGlobalTransformParentTransformed(CommonNodeAccessor parentNode) {
		if (transformMatrix != null) {
			if (transformMatrix == ZERO_MATRIX) {
				globalTransformMatrix = ZERO_MATRIX;
				for (CommonNodeAccessor children : children) {
					children.setGlobalTransformToZeroMatrixRecursively();
				}
			} else {
				globalTransformMatrix = parentNode.globalTransformMatrix.mul(transformMatrix, calculatedTransformMatrix);
				for (CommonNodeAccessor children : children) {
					children.calculateGlobalTransformParentTransformed(this);
				}
			}
		} else {
			if (scale.x == 0 && scale.y == 0 && scale.z == 0) {
				globalTransformMatrix = ZERO_MATRIX;
				for (CommonNodeAccessor children : children) {
					children.setGlobalTransformToZeroMatrixRecursively();
				}
			} else {
				globalTransformMatrix = parentNode.globalTransformMatrix.translate(translation, calculatedTransformMatrix).rotate(rotation).scale(scale);
				for (CommonNodeAccessor children : children) {
					children.calculateGlobalTransformParentTransformed(this);
				}
			}
		}
		if (isTRSModified) {
			resetTRS();
			isTRSModified = false;
		}
		if (isTransformMatrixModified) {
			transformMatrix = originalTransformMatrix;
			isTransformMatrixModified = false;
		}
	}

	public void resetGlobalTransform() {
		globalTransformMatrix = originalGlobalTransformMatrix;
	}

	protected void resetTRS() {
		if (originalTranslation != null) translation.set(originalTranslation);
		else translation.zero();
		if (originalRotation != null) rotation.set(originalRotation);
		else rotation.identity();
		if (originalScale != null) scale.set(originalScale);
		else scale.set(1);
	}
}
