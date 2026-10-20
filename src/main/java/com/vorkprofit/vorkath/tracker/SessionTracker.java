package com.vorkprofit.vorkath.tracker;

/**
 * Session-only kill, death, instance time, experience, loot and consumable/reclaim cost
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
	private long instanceTimeMillis;
	private long instanceTimerLastTick = -1L;
	private long gpGained;
	private long gpSpent;
	private long lootWindowEnds;

	public void startIfNeeded(long now, long overallXp)
	{
		if (startedAt < 0) { startedAt = now; startingXp = overallXp; }
	}

	public void tickInstanceTimer(long now, boolean inInstance)
	{
		if (inInstance)
		{
			if (instanceTimerLastTick > 0L)
			{
				long delta = now - instanceTimerLastTick;
				if (delta > 0L && delta < 10_000L)
				{
					instanceTimeMillis += delta;
				}
			}
			instanceTimerLastTick = now;
		}
		else
		{
			instanceTimerLastTick = -1L;
		}
	}

	public void resetSession(long now, long overallXp)
	{
		startedAt = now;
		startingXp = overallXp;
		kills = 0;
		deaths = 0;
		instanceTimeMillis = 0L;
		instanceTimerLastTick = -1L;
		gpGained = 0L;
		gpSpent = 0L;
		lootWindowEnds = 0L;
	}

	public void recordKill(long now) { kills++; lootWindowEnds = now + LOOT_WINDOW_MILLIS; }
	public void recordDeath() { deaths++; gpSpent += DEATH_RECLAIM_COST; }
	public boolean acceptsLoot(long now) { return now <= lootWindowEnds; }
	public void recordLoot(long value) { if (value > 0) gpGained += value; }
	public void recordItemSpent(long itemPrice) { if (itemPrice > 0) gpSpent += itemPrice; }

	public Snapshot snapshot(long now, long overallXp)
	{
		if (startedAt < 0) return new Snapshot(0, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
		// Use active instance time for per-hour calculations so idle time outside the instance is excluded.
		// If instance time is under 1 ms, fallback to 1ms to prevent division by zero.
		long activeTime = Math.max(1L, instanceTimeMillis);
		long xp = Math.max(0L, overallXp - startingXp);
		long netGp = gpGained - gpSpent;
		return new Snapshot(
			kills,
			deaths,
			instanceTimeMillis,
			xp,
			gpGained,
			gpSpent,
			netGp,
			kills * 3_600_000L / activeTime,
			xp * 3_600_000L / activeTime,
			gpGained * 3_600_000L / activeTime,
			netGp * 3_600_000L / activeTime
		);
	}

	public static final class Snapshot
	{
		public final int kills;
		public final int deaths;
		public final long instanceTimeMillis;
		public final long xp;
		public final long gpGained;
		public final long gpSpent;
		public final long netGp;
		public final long killsPerHour;
		public final long xpPerHour;
		public final long gpPerHour;
		public final long netGpPerHour;

		Snapshot(int kills, int deaths, long instanceTimeMillis, long xp, long gpGained, long gpSpent, long netGp, long killsPerHour, long xpPerHour, long gpPerHour, long netGpPerHour)
		{
			this.kills = kills;
			this.deaths = deaths;
			this.instanceTimeMillis = instanceTimeMillis;
			this.xp = xp;
			this.gpGained = gpGained;
			this.gpSpent = gpSpent;
			this.netGp = netGp;
			this.killsPerHour = killsPerHour;
			this.xpPerHour = xpPerHour;
			this.gpPerHour = gpPerHour;
			this.netGpPerHour = netGpPerHour;
		}
	}
}

