/*
 * Copyright (C) 2021 | varpeti | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.forge.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.wurstclient.fmlevents.WUpdateEvent;
import net.wurstclient.forge.Category;
import net.wurstclient.forge.Hack;
import net.wurstclient.forge.compatibility.WMinecraft;
import net.wurstclient.forge.settings.CheckboxSetting;
import net.wurstclient.forge.utils.BlockUtils;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.ArrayList;

public final class BlockDataHack extends Hack
{
	private final CheckboxSetting technicalName = new CheckboxSetting("Technical name", true);
	private final CheckboxSetting localizedName = new CheckboxSetting("Localized name", false);
	private final CheckboxSetting unlocalizedName = new CheckboxSetting("Unlocalized name", false);
	private final CheckboxSetting metadata = new CheckboxSetting("Technical name with Metadata", false);

	String text;

	public BlockDataHack()
	{
		super("Block Data",
			"Shows the data of the block the player looking at.");
		setCategory(Category.RENDER);
		addSetting(technicalName);
		addSetting(localizedName);
		addSetting(unlocalizedName);
		addSetting(metadata);
	}
	
	@Override
	protected void onEnable()
	{
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Override
	protected void onDisable()
	{
		MinecraftForge.EVENT_BUS.unregister(this);
	}
	
	@SubscribeEvent
	public void onUpdate(WUpdateEvent event)
	{
		RayTraceResult lookingAt = mc.objectMouseOver;
		if (lookingAt != null && lookingAt.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos pos = lookingAt.getBlockPos();
			Block block = BlockUtils.getBlock(pos);
			IBlockState state = BlockUtils.getState(pos);
			text = "";
			if (technicalName.isChecked())
				text += BlockUtils.getName(pos) + " ";
			if (localizedName.isChecked())
				text += block.getLocalizedName() + " ";
			if (unlocalizedName.isChecked())
				text += block.getUnlocalizedName() + " ";
			if (metadata.isChecked())
				text += state.toString() + " ";
			return;
		}
		text = "";
	}

	@Override
	public String getRenderName()
	{
		return getName() + "( " + text + ")";
	}
}
