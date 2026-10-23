package com.vorkprofit.vorkath.tracker;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Session-only kill, death, session time, instance time, idle time, experience, loot and consumable/reclaim cost
 * accounting for Vorkath. Values are reset when the plugin restarts or the user clicks Reset.
 */
public final class SessionTracker
{
	private static final long LOOT_WINDOW_MILLIS = 30_000L;
	/** RuneScape death-reclaim cost at Vorkath (gravestones cost ~100k). */
	private static final long DEATH_RECLAIM_COST = 100_000L;

	private long startedAt = -1L;
	private long startingXp;
	private int kills;
	private int deaths;
	private long sessionTimeMillis;
	private long instanceTimeMillis;
	private long timerLastTick = -1L;
	private long gpGained;
	private long gpSpent;
	private long lootWindowEnds;
	private final Map<Integer, Integer> sessionItems = new LinkedHashMap<>();

	public boolean isStarted()
	{
		return startedAt >= 0L;
	}

	public void startIfNeeded(long now, long overallXp)
	{
		if (startedAt < 0L)
		{
			start(now, overallXp);
		}
	}

	public void start(long now, long overallXp)
	{
		startedAt = now;
		startingXp = overallXp;
		timerLastTick = now;
	}

	public void tickTimers(long now, boolean inInstance)
	{
		if (startedAt < 0L)
		{
			return;
		}

		if (timerLastTick >= 0L)
		{
			long delta = now - timerLastTick;
			if (delta > 0L && delta < 10_000L)
			{
				sessionTimeMillis += delta;
				if (inInstance)
				{
					instanceTimeMillis += delta;
				}
			}
		}
		timerLastTick = now;
	}

	public void tickInstanceTimer(long now, boolean inInstance)
	{
		tickTimers(now, inInstance);
	}

	public void resetSession(long now, long overallXp)
	{
		startedAt = -1L;
		startingXp = overallXp;
		kills = 0;
		deaths = 0;
		sessionTimeMillis = 0L;
		instanceTimeMillis = 0L;
		timerLastTick = -1L;
		gpGained = 0L;
		gpSpent = 0L;
		lootWindowEnds = 0L;
		sessionItems.clear();
	}

	public void recordKill(long now)
	{
		kills++;
		lootWindowEnds = now + LOOT_WINDOW_MILLIS;
	}

	public void recordDeath()
	{
		deaths++;
		gpSpent += DEATH_RECLAIM_COST;
	}

	public boolean acceptsLoot(long now)
	{
		return now <= lootWindowEnds;
	}

	public void recordLoot(long value)
	{
		recordLoot(-1, 0, value);
	}

	public void recordLoot(int itemId, int quantity, long value)
	{
		if (value > 0L)
		{
			gpGained += value;
		}
		if (itemId > 0 && quantity > 0)
		{
			sessionItems.put(itemId, sessionItems.getOrDefault(itemId, 0) + quantity);
		}
	}

	public void recordItemSpent(long itemPrice)
	{
		if (itemPrice > 0L)
		{
			gpSpent += itemPrice;
		}
	}

	public Snapshot snapshot(long now, long overallXp)
	{
		if (startedAt < 0L)
		{
			return new Snapshot(
				0, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
				Collections.emptyMap()
			);
		}

		long activeTime = Math.max(1L, instanceTimeMillis);
		long idleTime = Math.max(0L, sessionTimeMillis - instanceTimeMillis);
		long xp = Math.max(0L, overallXp - startingXp);
		long netGp = gpGained - gpSpent;
		return new Snapshot(
			kills,
			deaths,
			sessionTimeMillis,
			instanceTimeMillis,
			idleTime,
			xp,
			gpGained,
			gpSpent,
			netGp,
			kills * 3_600_000L / activeTime,
			xp * 3_600_000L / activeTime,
			gpGained * 3_600_000L / activeTime,
			netGp * 3_600_000L / activeTime,
			Collections.unmodifiableMap(new LinkedHashMap<>(sessionItems))
		);
	}

	public static final class Snapshot
	{
		public final int kills;
		public final int deaths;
		public final long sessionTimeMillis;
		public final long instanceTimeMillis;
		public final long idleTimeMillis;
		public final long xp;
		public final long gpGained;
		public final long gpSpent;
		public final long netGp;
		public final long killsPerHour;
		public final long xpPerHour;
		public final long gpPerHour;
		public final long netGpPerHour;
		public final Map<Integer, Integer> items;

		Snapshot(
			int kills,
			int deaths,
			long sessionTimeMillis,
			long instanceTimeMillis,
			long idleTimeMillis,
			long xp,
			long gpGained,
			long gpSpent,
			long netGp,
			long killsPerHour,
			long xpPerHour,
			long gpPerHour,
			long netGpPerHour,
			Map<Integer, Integer> items
		)
		{
			this.kills = kills;
			this.deaths = deaths;
			this.sessionTimeMillis = sessionTimeMillis;
			this.instanceTimeMillis = instanceTimeMillis;
			this.idleTimeMillis = idleTimeMillis;
			this.xp = xp;
			this.gpGained = gpGained;
			this.gpSpent = gpSpent;
			this.netGp = netGp;
			this.killsPerHour = killsPerHour;
			this.xpPerHour = xpPerHour;
			this.gpPerHour = gpPerHour;
			this.netGpPerHour = netGpPerHour;
			this.items = items;
		}
	}
}
