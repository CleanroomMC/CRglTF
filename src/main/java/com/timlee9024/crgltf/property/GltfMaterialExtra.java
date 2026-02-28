package com.timlee9024.crgltf.property;

public class GltfMaterialExtra {

	public CRglTF crgltf;

	public static class CRglTF {

		public DaxShader daxShader;

		public static class DaxShader {

			public Integer colorMapTexture;

			public Integer normalMapTexture;

			public Integer specularMapTexture;

			public Integer mc_midTexCoord;
		}
	}
}
