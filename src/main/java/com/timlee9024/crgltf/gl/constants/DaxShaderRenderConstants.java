package com.timlee9024.crgltf.gl.constants;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * https://daxnitro.fandom.com/wiki/Editing_Shaders_(Shaders2)
 */
public abstract class DaxShaderRenderConstants {

	private static DaxShaderRenderConstants instance;

	public static DaxShaderRenderConstants getInstance() {
		return instance;
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
	}

	/**
	 * mc_midTexCoord
	 *
	 * @return
	 */
	public abstract int getMcMidTexCoordAttributeIndex();

	/**
	 * at_tangent
	 *
	 * @return
	 */
	public abstract int getTangentAttributeIndex();

	public abstract int getNormalTextureIndex();

	public abstract int getSpecularTextureIndex();

}
