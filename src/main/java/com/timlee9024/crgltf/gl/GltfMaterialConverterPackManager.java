package com.timlee9024.crgltf.gl;

import com.timlee9024.crgltf.CRglTFMod;
import com.timlee9024.crgltf.config.ModConfig;
import com.timlee9024.crgltf.gl.constants.GltfMaterialToTextureConstants;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class GltfMaterialConverterPackManager {

	private static GltfMaterialConverterPackManager instance;

	protected GltfMaterialToTextureConstants.Program colorTextureProgram;
	protected GltfMaterialToTextureConstants.Program normalTextureProgram;
	protected GltfMaterialToTextureConstants.Program specularTextureProgram;

	public static GltfMaterialConverterPackManager getInstance() {
		return instance;
	}

	public void onEvent(FMLPreInitializationEvent event) {
		instance = this;
		reloadTextureCombinerPrograms();
	}

	public GltfMaterialToTextureConstants.Program getColorTextureProgram() {
		return colorTextureProgram;
	}

	public GltfMaterialToTextureConstants.Program getNormalTextureProgram() {
		return normalTextureProgram;
	}

	public GltfMaterialToTextureConstants.Program getSpecularTextureProgram() {
		return specularTextureProgram;
	}

	public void reloadTextureCombinerPrograms() {
		if (colorTextureProgram != null) GL20.glDeleteProgram(colorTextureProgram.getGlProgram());
		if (normalTextureProgram != null) GL20.glDeleteProgram(normalTextureProgram.getGlProgram());
		if (specularTextureProgram != null) GL20.glDeleteProgram(specularTextureProgram.getGlProgram());

		File file = new File(ModConfig.getInstance().getGltfMaterialConverterPackDir(), ModConfig.getInstance().getGltfMaterialConverterPack().getString());
		if (file.exists()) {
			if (file.isDirectory()) {

				File colorShaderFile = new File(file, "color.glsl");
				if (colorShaderFile.canRead()) {
					try {
						colorTextureProgram = GltfMaterialToTextureConstants.getInstance().new Program(Files.readString(colorShaderFile.toPath()));
						CRglTFMod.logger.debug("Color texture program loaded.");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else colorTextureProgram = null;

				File normalShaderFile = new File(file, "normal.glsl");
				if (normalShaderFile.canRead()) {
					try {
						normalTextureProgram = GltfMaterialToTextureConstants.getInstance().new Program(Files.readString(normalShaderFile.toPath()));
						CRglTFMod.logger.debug("Normal texture program loaded.");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else normalTextureProgram = null;

				File specularShaderFile = new File(file, "specular.glsl");
				if (specularShaderFile.canRead()) {
					try {
						specularTextureProgram = GltfMaterialToTextureConstants.getInstance().new Program(Files.readString(specularShaderFile.toPath()));
						CRglTFMod.logger.debug("Specular texture program loaded.");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else specularTextureProgram = null;
			} else {
				try (FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jar:" + file.toURI()), Collections.emptyMap())) {

					Path colorShaderPath = fileSystem.getPath("/color.glsl");
					if (Files.isReadable(colorShaderPath)) {
						colorTextureProgram = GltfMaterialToTextureConstants.getInstance().new Program(Files.readString(colorShaderPath));
						CRglTFMod.logger.debug("Color texture program loaded.");
					} else colorTextureProgram = null;

					Path normalShaderPath = fileSystem.getPath("/normal.glsl");
					if (Files.isReadable(normalShaderPath)) {
						normalTextureProgram = GltfMaterialToTextureConstants.getInstance().new Program(Files.readString(normalShaderPath));
						CRglTFMod.logger.debug("Normal texture program loaded.");
					} else normalTextureProgram = null;

					Path specularShaderPath = fileSystem.getPath("/specular.glsl");
					if (Files.isReadable(specularShaderPath)) {
						specularTextureProgram = GltfMaterialToTextureConstants.getInstance().new Program(Files.readString(specularShaderPath));
						CRglTFMod.logger.debug("Specular texture program loaded.");
					} else specularTextureProgram = null;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			colorTextureProgram = null;
			normalTextureProgram = null;
			specularTextureProgram = null;
		}
	}
}
