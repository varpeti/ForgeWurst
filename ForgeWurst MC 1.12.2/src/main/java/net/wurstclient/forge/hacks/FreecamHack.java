/*
 * Copyright (C) 2017 - 2019 | Wurst-Imperium | All rights reserved.
 * Copyright (C) 2021 | varpeti | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */



package net.wurstclient.forge.hacks;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.wurstclient.fmlevents.*;
import net.wurstclient.forge.Category;
import net.wurstclient.forge.Hack;
import net.wurstclient.forge.Hack.DontSaveState;
import net.wurstclient.forge.settings.SliderSetting;
import net.wurstclient.forge.settings.SliderSetting.ValueDisplay;
import net.wurstclient.forge.utils.ChatUtils;
import net.wurstclient.forge.utils.EntityFakePlayer;
import net.wurstclient.forge.utils.KeyBindingUtils;

import java.lang.reflect.Field;

@DontSaveState
public final class FreecamHack extends Hack
{
	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	
	private EntityFakePlayer fakePlayer;
	
	public FreecamHack()
	{
		super("Freecam", "Allows you to move the camera\n"
			+ "without moving your character.");
		setCategory(Category.RENDER);
		addSetting(speed);
	}
	
	@Override
	protected void onEnable()
	{
		MinecraftForge.EVENT_BUS.register(this);
		fakePlayer = new EntityFakePlayer();
		
		GameSettings gs = mc.gameSettings;
		KeyBinding[] bindings = {gs.keyBindForward, gs.keyBindBack,
			gs.keyBindLeft, gs.keyBindRight, gs.keyBindJump, gs.keyBindSneak};
		for(KeyBinding binding : bindings)
			KeyBindingUtils.resetPressed(binding);
	}
	
	@Override
	protected void onDisable()
	{
		MinecraftForge.EVENT_BUS.unregister(this);
		
		fakePlayer.resetPlayerPosition();
		fakePlayer.despawn();
		
		mc.renderGlobal.loadRenderers();
	}

	public double deg2rad(double deg)
	{
		return (deg/180)*Math.PI;
	}
	
	@SubscribeEvent
	public void onUpdate(WUpdateEvent event)
	{
		EntityPlayerSP player = event.getPlayer();

		player.motionX = 0;
		player.motionY = 0;
		player.motionZ = 0;

		player.onGround = false;
		player.jumpMovementFactor = 0;//speed.getValueF();

		if(mc.gameSettings.keyBindJump.isKeyDown()) player.motionY += speed.getValue();
		if(mc.gameSettings.keyBindSneak.isKeyDown()) player.motionY -= speed.getValue();

		double rot = deg2rad(player.rotationYaw);
		int add = 0;

		if(mc.gameSettings.keyBindForward.isKeyDown()) {
			player.motionX = Math.cos(rot-Math.PI/2) * -speed.getValue();
			player.motionZ = Math.sin(rot-Math.PI/2) * -speed.getValue();
			add++;
		}
		if(mc.gameSettings.keyBindBack.isKeyDown()) {
			player.motionX += Math.cos(rot-Math.PI/2) * speed.getValue();
			player.motionZ += Math.sin(rot-Math.PI/2) * speed.getValue();
			add++;
		}

		if(mc.gameSettings.keyBindRight.isKeyDown()) {
			player.motionX += Math.cos(rot) * -speed.getValue();
			player.motionZ += Math.sin(rot) * -speed.getValue();
			add++;
		}
		if(mc.gameSettings.keyBindLeft.isKeyDown()) {
			player.motionX += Math.cos(rot) * speed.getValue();
			player.motionZ += Math.sin(rot) * speed.getValue();
			add++;
		}

		if (add > 1) {
			player.motionX /= add;
			player.motionZ /= add;
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(WPlayerMoveEvent event)
	{
		event.getPlayer().noClip = true;
	}
	
	@SubscribeEvent
	public void onIsNormalCube(WIsNormalCubeEvent event)
	{
		event.setCanceled(true);
	}
	
	@SubscribeEvent
	public void onSetOpaqueCube(WSetOpaqueCubeEvent event)
	{
		event.setCanceled(true);
	}
	
	@SubscribeEvent
	public void onPacketOutput(WPacketOutputEvent event)
	{
		if(event.getPacket() instanceof CPacketPlayer)
			event.setCanceled(true);
	}
}
