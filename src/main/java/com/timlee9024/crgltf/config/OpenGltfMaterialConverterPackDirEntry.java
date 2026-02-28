package com.timlee9024.crgltf.config;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.HoverChecker;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.io.File;

public class OpenGltfMaterialConverterPackDirEntry extends GuiConfigEntries.ListEntryBase {

	protected final GuiButtonExt btnOpenDir;

	public OpenGltfMaterialConverterPackDirEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
		super(owningScreen, owningEntryList, configElement);

		this.btnOpenDir = new GuiButtonExt(0, 0, 0, 300, 18, I18n.format(name));
		this.tooltipHoverChecker = new HoverChecker(this.btnOpenDir, 800);

		drawLabel = false;
	}

	@Override
	public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partial) {
		this.btnOpenDir.x = listWidth / 2 - 150;
		this.btnOpenDir.y = y;
		this.btnOpenDir.enabled = enabled();
		this.btnOpenDir.drawButton(this.mc, mouseX, mouseY, partial);

		super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial);
	}

	@Override
	public void drawToolTip(int mouseX, int mouseY) {
		boolean canHover = mouseY < this.owningScreen.entryList.bottom && mouseY > this.owningScreen.entryList.top;

		if (this.tooltipHoverChecker.checkHover(mouseX, mouseY, canHover))
			this.owningScreen.drawToolTip(toolTip, mouseX, mouseY);

		super.drawToolTip(mouseX, mouseY);
	}

	/**
	 * Called when the mouse is clicked within this entry. Returning true means that something within this entry was clicked and the list should not be dragged.
	 */
	@Override
	public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
		if (this.btnOpenDir.mousePressed(this.mc, x, y)) {
			btnOpenDir.playPressSound(mc.getSoundHandler());
			File gltfMaterialConverterPackDir = ModConfig.getInstance().getGltfMaterialConverterPackDir();
			if (!gltfMaterialConverterPackDir.exists()) gltfMaterialConverterPackDir.mkdir();
			OpenGlHelper.openFile(gltfMaterialConverterPackDir);
			return true;
		} else
			return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY);
	}

	/**
	 * Fired when the mouse button is released. Arguments: index, x, y, mouseEvent, relativeX, relativeY"
	 */
	@Override
	public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
		this.btnOpenDir.mouseReleased(x, y);
	}

	@Override
	public boolean isDefault() {
		return true;
	}

	@Override
	public void setToDefault() {

	}

	@Override
	public void keyTyped(char c, int i) {

	}

	@Override
	public void updateCursorCounter() {

	}

	@Override
	public void mouseClicked(int i, int j, int k) {

	}

	@Override
	public boolean isChanged() {
		return false;
	}

	@Override
	public void undoChanges() {

	}

	@Override
	public boolean saveConfigElement() {
		return false;
	}

	@Override
	public int getLabelWidth() {
		return 0;
	}

	@Override
	public int getEntryRightBound() {
		return this.owningEntryList.width / 2 + 155 + 22 + 18;
	}

	@Override
	public String getCurrentValue() {
		return "";
	}

	@Override
	public String[] getCurrentValues() {
		return new String[]{getCurrentValue()};
	}
}
