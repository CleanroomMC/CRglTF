package com.timlee9024.crgltf.util;

import com.timlee9024.crgltf.modified.jgltf.viewer.AccessorDataUtils;
import com.timlee9024.crgltf.modified.jgltf.viewer.AccessorModelCreation;
import com.timlee9024.crgltf.modified.jme3.util.mikktspace.MikkTSpaceContext;
import com.timlee9024.crgltf.modified.jme3.util.mikktspace.MikktspaceTangentGenerator;
import de.javagl.jgltf.model.AccessorByteData;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorIntData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AccessorShortData;
import de.javagl.jgltf.model.ElementType;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.impl.DefaultMeshPrimitiveModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class GltfModelAttributeProcessor {

	public static void processAttributesForRender(GltfModel gltfModel) {
		for (MeshModel meshModel : gltfModel.getMeshModels()) {
			for (MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
				if (meshPrimitiveModel instanceof DefaultMeshPrimitiveModel defaultMeshPrimitiveModel) {
					Map<String, AccessorModel> attributes = meshPrimitiveModel.getAttributes();
					attributes.forEach((name, accessorModel) -> {
						if (name.startsWith("COLOR_") && accessorModel.getElementType() == ElementType.VEC3) {
							defaultMeshPrimitiveModel.putAttribute(name, obtainVec4ColorsAccessorModel(accessorModel));
						}
					});
					List<Map<String, AccessorModel>> targets = meshPrimitiveModel.getTargets();
					for (int i = 0; i < targets.size(); i++) {
						int index = i;
						targets.get(i).forEach((name, accessorModel) -> {
							if (name.startsWith("COLOR_") && accessorModel.getElementType() == ElementType.VEC3) {
								defaultMeshPrimitiveModel.putTarget(index, name, obtainVec4ColorsAccessorModel(accessorModel));
							}
						});
					}

					if (attributes.get("POSITION") != null) {
						AccessorModel normalsAccessorModel = attributes.get("NORMAL");
						if (normalsAccessorModel == null) {
							convertToNonIndexedAccessorModel(defaultMeshPrimitiveModel, attributes);
							attributes = meshPrimitiveModel.getAttributes();
							targets = meshPrimitiveModel.getTargets();

							AccessorModel positionsAccessorModel = attributes.get("POSITION");
							int count = positionsAccessorModel.getCount();
							normalsAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC3, "");
							AccessorModel tangentsAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");

							defaultMeshPrimitiveModel.putAttribute("NORMAL", normalsAccessorModel);
							defaultMeshPrimitiveModel.putAttribute("TANGENT", tangentsAccessorModel);

							AccessorFloatData positions = (AccessorFloatData) positionsAccessorModel.getAccessorData();
							AccessorFloatData normals = (AccessorFloatData) normalsAccessorModel.getAccessorData();
							AccessorFloatData tangents = (AccessorFloatData) tangentsAccessorModel.getAccessorData();

							processFlatNormalsAccessorData(positions, normals);

							String normalTexcoordsAttributeName = getNormalTexcoordsAttributeName(meshPrimitiveModel);
							AccessorModel normalTexcoordsAccessorModel = attributes.get(normalTexcoordsAttributeName);
							if (normalTexcoordsAccessorModel != null) {
								AccessorDataFloatGetter texcoords = AccessorDataFloatGetter.createDequantized(normalTexcoordsAccessorModel.getAccessorData());
								processMikkTangentsAccessorData(positions, normals, texcoords, tangents);

								for (int i = 0; i < targets.size(); i++) {
									Map<String, AccessorModel> target = targets.get(i);
									AccessorModel positionsTargetAccessorModel = target.get("POSITION");
									if (positionsTargetAccessorModel != null) {
										AccessorModel normalsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC3, "");
										AccessorModel tangentsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
										defaultMeshPrimitiveModel.putTarget(i, "NORMAL", normalsTargetAccessorModel);
										defaultMeshPrimitiveModel.putTarget(i, "TANGENT", tangentsTargetAccessorModel);

										AccessorFloatData positionsTarget = (AccessorFloatData) positionsTargetAccessorModel.getAccessorData();
										AccessorFloatData normalsTarget = (AccessorFloatData) normalsTargetAccessorModel.getAccessorData();
										AccessorFloatData tangentsTarget = (AccessorFloatData) tangentsTargetAccessorModel.getAccessorData();

										processFlatNormalsTargetAccessorData(positions, normals, positionsTarget, normalsTarget);

										AccessorDataFloatGetter positionsMorphed = (e, c) -> positions.get(e, c) + positionsTarget.get(e, c);
										AccessorDataFloatGetter normalsMorphed = (e, c) -> normals.get(e, c) + normalsTarget.get(e, c);
										AccessorDataFloatGetter texcoordsMorphed;
										AccessorModel normalTexcoordsTargetAccessorModel = target.get(normalTexcoordsAttributeName);
										if (normalTexcoordsTargetAccessorModel != null) {
											AccessorDataFloatGetter texcoordsTarget = AccessorDataFloatGetter.createDequantized(normalTexcoordsTargetAccessorModel.getAccessorData());
											texcoordsMorphed = (e, c) -> texcoords.getFloat(e, c) + texcoordsTarget.getFloat(e, c);
										} else texcoordsMorphed = texcoords;

										processMikkTangentsTargetAccessorData(positionsMorphed, normalsMorphed, tangents, texcoordsMorphed, tangentsTarget);
									} else {
										AccessorModel normalTexcoordsTargetAccessorModel = target.get(normalTexcoordsAttributeName);
										if (normalTexcoordsTargetAccessorModel != null) {
											AccessorModel tangentsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
											defaultMeshPrimitiveModel.putTarget(i, "TANGENT", tangentsTargetAccessorModel);

											AccessorFloatData tangentsTarget = (AccessorFloatData) tangentsTargetAccessorModel.getAccessorData();
											AccessorDataFloatGetter texcoordsTarget = AccessorDataFloatGetter.createDequantized(normalTexcoordsTargetAccessorModel.getAccessorData());
											AccessorDataFloatGetter positionsMorphed = positions::get;
											AccessorDataFloatGetter normalsMorphed = normals::get;
											AccessorDataFloatGetter texcoordsMorphed = (e, c) -> texcoords.getFloat(e, c) + texcoordsTarget.getFloat(e, c);

											processMikkTangentsTargetAccessorData(positionsMorphed, normalsMorphed, tangents, texcoordsMorphed, tangentsTarget);
										}
									}
								}
							} else {
								processSimpleTangentsAccessorData(normals, tangents);

								for (int i = 0; i < targets.size(); i++) {
									Map<String, AccessorModel> target = targets.get(i);
									AccessorModel positionsTargetAccessorModel = target.get("POSITION");
									if (positionsTargetAccessorModel != null) {
										AccessorModel normalsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC3, "");
										AccessorModel tangentsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
										defaultMeshPrimitiveModel.putTarget(i, "NORMAL", normalsTargetAccessorModel);
										defaultMeshPrimitiveModel.putTarget(i, "TANGENT", tangentsTargetAccessorModel);

										AccessorFloatData positionsTarget = (AccessorFloatData) positionsTargetAccessorModel.getAccessorData();
										AccessorFloatData normalsTarget = (AccessorFloatData) normalsTargetAccessorModel.getAccessorData();
										AccessorFloatData tangentsTarget = (AccessorFloatData) tangentsTargetAccessorModel.getAccessorData();

										processFlatNormalsTargetAccessorData(positions, normals, positionsTarget, normalsTarget);
										processSimpleTangentsTargetAccessorData(normals, tangents, normalsTarget, tangentsTarget);
									}
								}
							}
						} else {
							AccessorModel tangentsAccessorModel = attributes.get("TANGENT");
							if (tangentsAccessorModel == null) {
								String normalTexcoordsAttributeName = getNormalTexcoordsAttributeName(meshPrimitiveModel);
								if (attributes.get(normalTexcoordsAttributeName) != null) {
									convertToNonIndexedAccessorModel(defaultMeshPrimitiveModel, attributes);
									attributes = meshPrimitiveModel.getAttributes();
									targets = meshPrimitiveModel.getTargets();

									AccessorModel positionsAccessorModel = attributes.get("POSITION");
									int count = positionsAccessorModel.getCount();
									normalsAccessorModel = attributes.get("NORMAL");
									tangentsAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
									AccessorModel normalTexcoordsAccessorModel = attributes.get(normalTexcoordsAttributeName);

									defaultMeshPrimitiveModel.putAttribute("TANGENT", tangentsAccessorModel);

									AccessorFloatData positions = (AccessorFloatData) positionsAccessorModel.getAccessorData();
									AccessorFloatData normals = (AccessorFloatData) normalsAccessorModel.getAccessorData();
									AccessorFloatData tangents = (AccessorFloatData) tangentsAccessorModel.getAccessorData();
									AccessorDataFloatGetter texcoords = AccessorDataFloatGetter.createDequantized(normalTexcoordsAccessorModel.getAccessorData());

									processMikkTangentsAccessorData(positions, normals, texcoords, tangents);

									for (int i = 0; i < targets.size(); i++) {
										Map<String, AccessorModel> target = targets.get(i);

										AccessorModel positionsTargetAccessorModel = target.get("POSITION");
										if (positionsTargetAccessorModel != null) {
											AccessorModel tangentsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
											defaultMeshPrimitiveModel.putTarget(i, "TANGENT", tangentsTargetAccessorModel);

											AccessorFloatData tangentsTarget = (AccessorFloatData) tangentsTargetAccessorModel.getAccessorData();
											AccessorFloatData positionsTarget = (AccessorFloatData) positionsTargetAccessorModel.getAccessorData();
											AccessorDataFloatGetter positionsMorphed = (e, c) -> positions.get(e, c) + positionsTarget.get(e, c);
											AccessorDataFloatGetter normalsMorphed;
											AccessorDataFloatGetter texcoordsMorphed;

											AccessorModel normalsTargetAccessorModel = target.get("NORMAL");
											if (normalsTargetAccessorModel != null) {
												AccessorFloatData normalsTarget = (AccessorFloatData) normalsTargetAccessorModel.getAccessorData();
												normalsMorphed = (e, c) -> normals.get(e, c) + normalsTarget.get(e, c);
											} else normalsMorphed = normals::get;

											AccessorModel normalTexcoordsTargetAccessorModel = target.get(normalTexcoordsAttributeName);
											if (normalTexcoordsTargetAccessorModel != null) {
												AccessorDataFloatGetter texcoordsTarget = AccessorDataFloatGetter.createDequantized(normalTexcoordsTargetAccessorModel.getAccessorData());
												texcoordsMorphed = (e, c) -> texcoords.getFloat(e, c) + texcoordsTarget.getFloat(e, c);
											} else texcoordsMorphed = texcoords;

											processMikkTangentsTargetAccessorData(positionsMorphed, normalsMorphed, tangents, texcoordsMorphed, tangentsTarget);
										} else {
											AccessorModel normalsTargetAccessorModel = target.get("NORMAL");
											if (normalsTargetAccessorModel != null) {
												AccessorModel tangentsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
												defaultMeshPrimitiveModel.putTarget(i, "TANGENT", tangentsTargetAccessorModel);

												AccessorFloatData tangentsTarget = (AccessorFloatData) tangentsTargetAccessorModel.getAccessorData();
												AccessorFloatData normalsTarget = (AccessorFloatData) normalsTargetAccessorModel.getAccessorData();
												AccessorDataFloatGetter positionsMorphed = positions::get;
												AccessorDataFloatGetter normalsMorphed = (e, c) -> normals.get(e, c) + normalsTarget.get(e, c);
												AccessorDataFloatGetter texcoordsMorphed;

												AccessorModel normalTexcoordsTargetAccessorModel = target.get(normalTexcoordsAttributeName);
												if (normalTexcoordsTargetAccessorModel != null) {
													AccessorDataFloatGetter texcoordsTarget = AccessorDataFloatGetter.createDequantized(normalTexcoordsTargetAccessorModel.getAccessorData());
													texcoordsMorphed = (e, c) -> texcoords.getFloat(e, c) + texcoordsTarget.getFloat(e, c);
												} else texcoordsMorphed = texcoords;

												processMikkTangentsTargetAccessorData(positionsMorphed, normalsMorphed, tangents, texcoordsMorphed, tangentsTarget);
											} else {
												AccessorModel normalTexcoordsTargetAccessorModel = target.get(normalTexcoordsAttributeName);
												if (normalTexcoordsTargetAccessorModel != null) {
													AccessorModel tangentsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
													defaultMeshPrimitiveModel.putTarget(i, "TANGENT", tangentsTargetAccessorModel);

													AccessorFloatData tangentsTarget = (AccessorFloatData) tangentsTargetAccessorModel.getAccessorData();
													AccessorDataFloatGetter texcoordsTarget = AccessorDataFloatGetter.createDequantized(normalTexcoordsTargetAccessorModel.getAccessorData());
													AccessorDataFloatGetter positionsMorphed = positions::get;
													AccessorDataFloatGetter normalsMorphed = normals::get;
													AccessorDataFloatGetter texcoordsMorphed = (e, c) -> texcoords.getFloat(e, c) + texcoordsTarget.getFloat(e, c);

													processMikkTangentsTargetAccessorData(positionsMorphed, normalsMorphed, tangents, texcoordsMorphed, tangentsTarget);
												}
											}
										}
									}
								} else {
									int count = normalsAccessorModel.getCount();
									normalsAccessorModel = attributes.get("NORMAL");
									tangentsAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
									defaultMeshPrimitiveModel.putAttribute("TANGENT", tangentsAccessorModel);

									AccessorFloatData normals = (AccessorFloatData) normalsAccessorModel.getAccessorData();
									AccessorFloatData tangents = (AccessorFloatData) tangentsAccessorModel.getAccessorData();

									processSimpleTangentsAccessorData(normals, tangents);

									for (int i = 0; i < targets.size(); i++) {
										Map<String, AccessorModel> target = targets.get(i);
										AccessorModel normalsTargetAccessorModel = target.get("NORMAL");
										if (normalsTargetAccessorModel != null) {
											AccessorModel tangentsTargetAccessorModel = AccessorModelCreation.createAccessorModel(GltfConstants.GL_FLOAT, count, ElementType.VEC4, "");
											defaultMeshPrimitiveModel.putTarget(i, "TANGENT", tangentsTargetAccessorModel);
											processSimpleTangentsTargetAccessorData(normals, tangents, (AccessorFloatData) normalsTargetAccessorModel.getAccessorData(), (AccessorFloatData) tangentsTargetAccessorModel.getAccessorData());
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private static String getNormalTexcoordsAttributeName(MeshPrimitiveModel meshPrimitiveModel) {
		MaterialModel materialModel = meshPrimitiveModel.getMaterialModel();
		if (materialModel instanceof MaterialModelV2 materialModelV2) {
			if (materialModelV2.getNormalTexture() != null) {
				Integer normalTexcoord = materialModelV2.getNormalTexcoord();
				if (normalTexcoord != null) return "TEXCOORD_" + normalTexcoord;
			}
		}
		return "TEXCOORD_0";
	}

	private static void convertToNonIndexedAccessorModel(DefaultMeshPrimitiveModel meshPrimitiveModel, Map<String, AccessorModel> attributes) {
		AccessorModel indicesAccessorModel = meshPrimitiveModel.getIndices();
		if (indicesAccessorModel != null) {
			int[] indices = AccessorDataUtils.readInts(indicesAccessorModel.getAccessorData());
			attributes.forEach((name, accessorModel) -> meshPrimitiveModel.putAttribute(name, obtainNonIndexedAccessorModel(accessorModel, indices)));
			List<Map<String, AccessorModel>> targets = meshPrimitiveModel.getTargets();
			for (int i = 0; i < targets.size(); i++) {
				int index = i;
				targets.get(i).forEach((name, accessorModel) -> meshPrimitiveModel.putTarget(index, name, obtainNonIndexedAccessorModel(accessorModel, indices)));
			}
			meshPrimitiveModel.setIndices(null);
		}
	}

	public static AccessorModel obtainNonIndexedAccessorModel(AccessorModel accessorModel, int[] indices) {
		ElementType elementType = accessorModel.getElementType();
		AccessorModel nonIndexedAccessorModel = AccessorModelCreation.createAccessorModel(accessorModel.getComponentType(), indices.length, elementType, "");
		AccessorData accessorData = accessorModel.getAccessorData();
		if (accessorData instanceof AccessorByteData src) {
			AccessorByteData dest = (AccessorByteData) nonIndexedAccessorModel.getAccessorData();
			int numComponents = elementType.getNumComponents();
			for (int destElement = 0; destElement < indices.length; destElement++) {
				int srcElement = indices[destElement];
				for (int c = 0; c < numComponents; c++) {
					dest.set(destElement, c, src.get(srcElement, c));
				}
			}
		} else if (accessorData instanceof AccessorShortData src) {
			AccessorShortData dest = (AccessorShortData) nonIndexedAccessorModel.getAccessorData();
			int numComponents = elementType.getNumComponents();
			for (int destElement = 0; destElement < indices.length; destElement++) {
				int index = indices[destElement];
				for (int c = 0; c < numComponents; c++) {
					dest.set(destElement, c, src.get(index, c));
				}
			}
		} else if (accessorData instanceof AccessorIntData src) {
			AccessorIntData dest = (AccessorIntData) nonIndexedAccessorModel.getAccessorData();
			int numComponents = elementType.getNumComponents();
			for (int destElement = 0; destElement < indices.length; destElement++) {
				int srcElement = indices[destElement];
				for (int c = 0; c < numComponents; c++) {
					dest.set(destElement, c, src.get(srcElement, c));
				}
			}
		} else if (accessorData instanceof AccessorFloatData src) {
			AccessorFloatData dest = (AccessorFloatData) nonIndexedAccessorModel.getAccessorData();
			int numComponents = elementType.getNumComponents();
			for (int destElement = 0; destElement < indices.length; destElement++) {
				int srcElement = indices[destElement];
				for (int c = 0; c < numComponents; c++) {
					dest.set(destElement, c, src.get(srcElement, c));
				}
			}
		}
		return nonIndexedAccessorModel;
	}

	public static void processFlatNormalsAccessorData(AccessorFloatData positions, AccessorFloatData normals) {
		int numElements = positions.getNumElements();
		Vector3f vertex0 = new Vector3f();
		Vector3f vertex1 = new Vector3f();
		Vector3f vertex2 = new Vector3f();
		for (int index0 = 0; index0 < numElements; index0 += 3) {
			int index1 = index0 + 1;
			int index2 = index0 + 2;

			vertex0.set(positions.get(index0, 0), positions.get(index0, 1), positions.get(index0, 2));
			vertex1.set(positions.get(index1, 0), positions.get(index1, 1), positions.get(index1, 2));
			vertex2.set(positions.get(index2, 0), positions.get(index2, 1), positions.get(index2, 2));

			vertex1.sub(vertex0).cross(vertex2.sub(vertex0)).normalize();

			normals.set(index0, 0, vertex1.x);
			normals.set(index0, 1, vertex1.y);
			normals.set(index0, 2, vertex1.z);

			normals.set(index1, 0, vertex1.x);
			normals.set(index1, 1, vertex1.y);
			normals.set(index1, 2, vertex1.z);

			normals.set(index2, 0, vertex1.x);
			normals.set(index2, 1, vertex1.y);
			normals.set(index2, 2, vertex1.z);
		}
	}

	public static void processFlatNormalsTargetAccessorData(AccessorFloatData positions, AccessorFloatData normals, AccessorFloatData positionsTarget, AccessorFloatData normalsTarget) {
		int numElements = positions.getNumElements();
		Vector3f vertex0 = new Vector3f();
		Vector3f vertex1 = new Vector3f();
		Vector3f vertex2 = new Vector3f();
		for (int index0 = 0; index0 < numElements; index0 += 3) {
			int index1 = index0 + 1;
			int index2 = index0 + 2;

			vertex0.set(positions.get(index0, 0) + positionsTarget.get(index0, 0), positions.get(index0, 1) + positionsTarget.get(index0, 1), positions.get(index0, 2) + positionsTarget.get(index0, 2));
			vertex1.set(positions.get(index1, 0) + positionsTarget.get(index1, 0), positions.get(index1, 1) + positionsTarget.get(index1, 1), positions.get(index1, 2) + positionsTarget.get(index1, 2));
			vertex2.set(positions.get(index2, 0) + positionsTarget.get(index2, 0), positions.get(index2, 1) + positionsTarget.get(index2, 1), positions.get(index2, 2) + positionsTarget.get(index2, 2));

			vertex1.sub(vertex0).cross(vertex2.sub(vertex0)).normalize();

			normalsTarget.set(index0, 0, vertex1.x - normals.get(index0, 0));
			normalsTarget.set(index0, 1, vertex1.y - normals.get(index0, 1));
			normalsTarget.set(index0, 2, vertex1.z - normals.get(index0, 2));

			normalsTarget.set(index1, 0, vertex1.x - normals.get(index1, 0));
			normalsTarget.set(index1, 1, vertex1.y - normals.get(index1, 1));
			normalsTarget.set(index1, 2, vertex1.z - normals.get(index1, 2));

			normalsTarget.set(index2, 0, vertex1.x - normals.get(index2, 0));
			normalsTarget.set(index2, 1, vertex1.y - normals.get(index2, 1));
			normalsTarget.set(index2, 2, vertex1.z - normals.get(index2, 2));
		}
	}

	/**
	 * Found this simple normals to tangent algorithm here:</br>
	 * <a href="https://stackoverflow.com/questions/55464852/how-to-find-a-randomic-vector-orthogonal-to-a-given-vector">How to find a randomic Vector orthogonal to a given Vector</a>
	 */
	public static void processSimpleTangentsAccessorData(AccessorFloatData normals, AccessorFloatData tangents) {
		int numElements = normals.getNumElements();
		Vector3f normal0 = new Vector3f();
		Vector3f normal1 = new Vector3f();
		for (int i = 0; i < numElements; i++) {
			normal0.set(normals.get(i, 0), normals.get(i, 1), normals.get(i, 2));
			normal1.set(-normal0.z, normal0.x, normal0.y);
			normal0.cross(normal1).normalize();

			tangents.set(i, 0, normal0.x);
			tangents.set(i, 1, normal0.y);
			tangents.set(i, 2, normal0.z);
			tangents.set(i, 3, 1.0F);
		}
	}

	public static void processSimpleTangentsTargetAccessorData(AccessorFloatData normals, AccessorFloatData tangents, AccessorFloatData normalsTarget, AccessorFloatData tangentsTarget) {
		int numElements = normals.getNumElements();
		Vector3f normal0 = new Vector3f();
		Vector3f normal1 = new Vector3f();
		for (int i = 0; i < numElements; i++) {
			normal0.set(normals.get(i, 0) + normalsTarget.get(i, 0), normals.get(i, 1) + normalsTarget.get(i, 1), normals.get(i, 2) + normalsTarget.get(i, 2));
			normal1.set(-normal0.z, normal0.x, normal0.y);
			normal0.cross(normal1).normalize();

			tangentsTarget.set(i, 0, normal0.x - tangents.get(i, 0));
			tangentsTarget.set(i, 1, normal0.y - tangents.get(i, 1));
			tangentsTarget.set(i, 2, normal0.z - tangents.get(i, 2));
			tangentsTarget.set(i, 3, 1.0F);
		}
	}

	public static void processMikkTangentsAccessorData(AccessorFloatData positions, AccessorFloatData normals, AccessorDataFloatGetter texcoords, AccessorFloatData tangents) {
		int numFaces = positions.getNumElements() / 3;
		MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

			@Override
			public int getNumFaces() {
				return numFaces;
			}

			@Override
			public int getNumVerticesOfFace(int face) {
				return 3;
			}

			@Override
			public void getPosition(float[] posOut, int face, int vert) {
				int index = (face * 3) + vert;
				posOut[0] = positions.get(index, 0);
				posOut[1] = positions.get(index, 1);
				posOut[2] = positions.get(index, 2);
			}

			@Override
			public void getNormal(float[] normOut, int face, int vert) {
				int index = (face * 3) + vert;
				normOut[0] = normals.get(index, 0);
				normOut[1] = normals.get(index, 1);
				normOut[2] = normals.get(index, 2);
			}

			@Override
			public void getTexCoord(float[] texOut, int face, int vert) {
				int index = (face * 3) + vert;
				texOut[0] = texcoords.getFloat(index, 0);
				texOut[1] = texcoords.getFloat(index, 1);
			}

			@Override
			public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
				int index = (face * 3) + vert;
				tangents.set(index, 0, tangent[0]);
				tangents.set(index, 1, tangent[1]);
				tangents.set(index, 2, tangent[2]);
				tangents.set(index, 3, sign);
			}

			@Override
			public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
				//Do nothing
			}

		});
	}

	public static void processMikkTangentsTargetAccessorData(AccessorDataFloatGetter positionsMorphed, AccessorDataFloatGetter normalsMorphed, AccessorFloatData tangents, AccessorDataFloatGetter texcoordsMorphed, AccessorFloatData tangentsTarget) {
		int numFaces = tangents.getNumElements() / 3;
		MikktspaceTangentGenerator.genTangSpaceDefault(new MikkTSpaceContext() {

			@Override
			public int getNumFaces() {
				return numFaces;
			}

			@Override
			public int getNumVerticesOfFace(int face) {
				return 3;
			}

			@Override
			public void getPosition(float[] posOut, int face, int vert) {
				int index = (face * 3) + vert;
				posOut[0] = positionsMorphed.getFloat(index, 0);
				posOut[1] = positionsMorphed.getFloat(index, 1);
				posOut[2] = positionsMorphed.getFloat(index, 2);
			}

			@Override
			public void getNormal(float[] normOut, int face, int vert) {
				int index = (face * 3) + vert;
				normOut[0] = normalsMorphed.getFloat(index, 0);
				normOut[1] = normalsMorphed.getFloat(index, 1);
				normOut[2] = normalsMorphed.getFloat(index, 2);
			}

			@Override
			public void getTexCoord(float[] texOut, int face, int vert) {
				int index = (face * 3) + vert;
				texOut[0] = texcoordsMorphed.getFloat(index, 0);
				texOut[1] = texcoordsMorphed.getFloat(index, 1);
			}

			@Override
			public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
				int index = (face * 3) + vert;
				tangentsTarget.set(index, 0, tangent[0] - tangents.get(index, 0));
				tangentsTarget.set(index, 1, tangent[1] - tangents.get(index, 1));
				tangentsTarget.set(index, 2, tangent[2] - tangents.get(index, 2));
				tangentsTarget.set(index, 3, sign);
			}

			@Override
			public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
				//Do nothing
			}

		});
	}

	public static AccessorModel obtainVec4ColorsAccessorModel(AccessorModel accessorModel) {
		int count = accessorModel.getCount();
		AccessorModel colorsVec4AccessorModel = AccessorModelCreation.createAccessorModel(accessorModel.getComponentType(), count, ElementType.VEC4, "");

		AccessorData accessorData = accessorModel.getAccessorData();
		if (accessorData instanceof AccessorByteData src) {
			AccessorByteData dest = (AccessorByteData) colorsVec4AccessorModel.getAccessorData();
			if (src.isUnsigned()) {
				for (int i = 0; i < count; i++) {
					dest.set(i, 0, src.get(i, 0));
					dest.set(i, 1, src.get(i, 1));
					dest.set(i, 2, src.get(i, 2));
					dest.set(i, 3, (byte) -1);
				}
			} else {
				for (int i = 0; i < count; i++) {
					dest.set(i, 0, src.get(i, 0));
					dest.set(i, 1, src.get(i, 1));
					dest.set(i, 2, src.get(i, 2));
					dest.set(i, 3, Byte.MAX_VALUE);
				}
			}
		} else if (accessorData instanceof AccessorShortData src) {
			AccessorShortData dest = (AccessorShortData) colorsVec4AccessorModel.getAccessorData();
			if (src.isUnsigned()) {
				for (int i = 0; i < count; i++) {
					dest.set(i, 0, src.get(i, 0));
					dest.set(i, 1, src.get(i, 1));
					dest.set(i, 2, src.get(i, 2));
					dest.set(i, 3, (short) -1);
				}
			} else {
				for (int i = 0; i < count; i++) {
					dest.set(i, 0, src.get(i, 0));
					dest.set(i, 1, src.get(i, 1));
					dest.set(i, 2, src.get(i, 2));
					dest.set(i, 3, Short.MAX_VALUE);
				}
			}
		} else if (accessorData instanceof AccessorFloatData src) {
			AccessorFloatData dest = (AccessorFloatData) colorsVec4AccessorModel.getAccessorData();
			for (int i = 0; i < count; i++) {
				dest.set(i, 0, src.get(i, 0));
				dest.set(i, 1, src.get(i, 1));
				dest.set(i, 2, src.get(i, 2));
				dest.set(i, 3, 1.0F);
			}
		}
		//Integer Component Type is not available for COLOR_n attribute defined glTF specification,
		//but this part is left for really rare case.
		else if (accessorData instanceof AccessorIntData src) {
			AccessorIntData dest = (AccessorIntData) colorsVec4AccessorModel.getAccessorData();
			if (src.isUnsigned()) {
				for (int i = 0; i < count; i++) {
					dest.set(i, 0, src.get(i, 0));
					dest.set(i, 1, src.get(i, 1));
					dest.set(i, 2, src.get(i, 2));
					dest.set(i, 3, -1);
				}
			} else {
				for (int i = 0; i < count; i++) {
					dest.set(i, 0, src.get(i, 0));
					dest.set(i, 1, src.get(i, 1));
					dest.set(i, 2, src.get(i, 2));
					dest.set(i, 3, Integer.MAX_VALUE);
				}
			}
		}
		return colorsVec4AccessorModel;
	}

}
