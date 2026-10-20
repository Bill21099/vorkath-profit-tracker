package com.vorkprofit.vorkath;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Starts a local developer client with VorkathPlugin registered. */
public final class DevClientLauncher
{
	private DevClientLauncher()
	{
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(VorkathPlugin.class);
		RuneLite.main(args);
	}
}

