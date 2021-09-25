/*
 * Copyright (C) 2017 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.forge.hacks;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.wurstclient.fmlevents.WCameraTransformViewBobbingEvent;
import net.wurstclient.fmlevents.WUpdateEvent;
import net.wurstclient.forge.Category;
import net.wurstclient.forge.Hack;
import net.wurstclient.forge.compatibility.WMinecraft;
import net.wurstclient.forge.compatibility.WVec3d;
import net.wurstclient.forge.settings.BlockListSetting;
import net.wurstclient.forge.settings.EnumSetting;
import net.wurstclient.forge.settings.SliderSetting;
import net.wurstclient.forge.utils.BlockUtils;
import net.wurstclient.forge.utils.RenderUtils;
import net.wurstclient.forge.utils.RotationUtils;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashSet;

public final class FindBlocksHack extends Hack
{
	private final EnumSetting<Style> style =
		new EnumSetting<>("Style", Style.values(), Style.BOXES);

	private final BlockListSetting blocksSetting = new BlockListSetting("Blocks",null,
      "minecraft:end_portal",
      "minecraft:end_portal_frame",
      "minecraft:portal");

	private final SliderSetting range =
		new SliderSetting("Range (XZ)", 64, 16, 516, 16, SliderSetting.ValueDisplay.INTEGER);

	private final SliderSetting numOfBatches =
		new SliderSetting("Number of batches", 100, 1, 1000, 1, SliderSetting.ValueDisplay.INTEGER);

	private final SliderSetting maxNumberOfBlocks =
			new SliderSetting("Maximum number of blocks", 5000, 1, 10000, 100, SliderSetting.ValueDisplay.INTEGER);

	HashSet<String> blockNames = new HashSet<String>();
	private final ArrayList<AxisAlignedBB> foundBlocks = new ArrayList<>();

	private int boxesGL11;
	private int toRender;

	long xyz;
	long XYZ;
	long XZ;
	long XZ2;
	long batch;
	int maxNumOfBlocks;

	public FindBlocksHack()
	{
		super("Find Blocks",
			"Find and highlights nearby blocks.\n");
		setCategory(Category.RENDER);
		addSetting(style);
		addSetting(blocksSetting);
		addSetting(range);
		addSetting(numOfBatches);
		addSetting(maxNumberOfBlocks);
	}

	@Override
	protected void onEnable()
	{
		MinecraftForge.EVENT_BUS.register(this);
		AxisAlignedBB bb = new AxisAlignedBB(BlockPos.ORIGIN);

		blockNames = new HashSet<String>(blocksSetting.getBlockNames());
		XZ = (long)range.getValueI();
		XZ2 = XZ*2;
		XYZ = (XZ2) + (XZ2)*(XZ2) + ((long)mc.world.getHeight())*(XZ2*XZ2);
		batch = XYZ/(long)numOfBatches.getValueI();
		maxNumOfBlocks = maxNumberOfBlocks.getValueI();

		boxesGL11 = GL11.glGenLists(1);
		GL11.glNewList(boxesGL11, GL11.GL_COMPILE);
		GL11.glColor4f(1, 0, 1, 0.25F);
		GL11.glBegin(GL11.GL_QUADS);
		RenderUtils.drawSolidBox(bb);
		GL11.glEnd();
		GL11.glColor4f(0, 1, 1, 0.5F);
		GL11.glBegin(GL11.GL_LINES);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEnd();
		GL11.glEndList();

		toRender = GL11.glGenLists(1);
	}

	@Override
	protected void onDisable()
	{
		MinecraftForge.EVENT_BUS.unregister(this);
		GL11.glDeleteLists(boxesGL11, 1);
		foundBlocks.clear();
		boxesGL11 = 0;
	}

	@SubscribeEvent
	public void onUpdate(WUpdateEvent event) {

		if (xyz >= XYZ) xyz = 0;

		BlockPos playerPos = event.getPlayer().getPosition();

		final long end = xyz + batch;
		for (; xyz<end; xyz++)
		{
			if(foundBlocks.size() >= maxNumOfBlocks) foundBlocks.remove(0);

			int x = (int)((xyz)%XZ2 - XZ);
			int z = (int)((xyz/XZ2)%XZ2 - XZ);
			int y = (int)((xyz/XZ2/XZ2));

			BlockPos pos = new BlockPos(playerPos.getX() + x, y, playerPos.getZ() + z);

			if(blockNames.contains(BlockUtils.getName(pos))) {
				AxisAlignedBB block = new AxisAlignedBB(pos);
				if (!foundBlocks.contains(block)) foundBlocks.add(block);
			}
		}

		GL11.glNewList(toRender, GL11.GL_COMPILE);
		renderBoxes(foundBlocks,boxesGL11);
		GL11.glEndList();


	}
	
	@SubscribeEvent
	public void onCameraTransformViewBobbing(
		WCameraTransformViewBobbingEvent event)
	{
		if(style.getSelected().lines)
			event.setCanceled(true);
	}
	
	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		GL11.glPushMatrix();
		GL11.glTranslated(-TileEntityRendererDispatcher.staticPlayerX,
			-TileEntityRendererDispatcher.staticPlayerY,
			-TileEntityRendererDispatcher.staticPlayerZ);
		
		if(style.getSelected().boxes)
		{
			GL11.glCallList(toRender);
		}
		
		if(style.getSelected().lines)
		{
			Vec3d start = RotationUtils.getClientLookVec()
				.addVector(0, WMinecraft.getPlayer().getEyeHeight(), 0)
				.addVector(TileEntityRendererDispatcher.staticPlayerX,
					TileEntityRendererDispatcher.staticPlayerY,
					TileEntityRendererDispatcher.staticPlayerZ);
			
			GL11.glBegin(GL11.GL_LINES);
			
			GL11.glColor4f(1, 0, 1, 0.5F);
			renderLines(start, foundBlocks);
			
			GL11.glEnd();
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void renderBoxes(ArrayList<AxisAlignedBB> boxes, int displayList)
	{
		for(AxisAlignedBB bb : boxes)
		{
			GL11.glPushMatrix();
			GL11.glTranslated(bb.minX, bb.minY, bb.minZ);
			GL11.glScaled(bb.maxX - bb.minX, bb.maxY - bb.minY,
				bb.maxZ - bb.minZ);
			GL11.glCallList(displayList);
			GL11.glPopMatrix();
		}
	}
	
	private void renderLines(Vec3d start, ArrayList<AxisAlignedBB> boxes)
	{
		for(AxisAlignedBB bb : boxes)
		{
			Vec3d end = bb.getCenter();
			
			GL11.glVertex3d(WVec3d.getX(start), WVec3d.getY(start),
				WVec3d.getZ(start));
			GL11.glVertex3d(WVec3d.getX(end), WVec3d.getY(end),
				WVec3d.getZ(end));
		}
	}
	
	private enum Style
	{
		BOXES("Boxes only", true, false),
		LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);
		
		private final String name;
		private final boolean boxes;
		private final boolean lines;
		
		private Style(String name, boolean boxes, boolean lines)
		{
			this.name = name;
			this.boxes = boxes;
			this.lines = lines;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
