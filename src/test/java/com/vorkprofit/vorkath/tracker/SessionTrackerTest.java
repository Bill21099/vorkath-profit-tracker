package com.vorkprofit.vorkath.tracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionTrackerTest
{
	@Test
	public void calculatesSessionRatesAndLootBasedOnInstanceTime()
	{
		SessionTracker tracker = new SessionTracker();
		tracker.startIfNeeded(1_000L, 10_000L);
		// Simulate ticks inside Vorkath instance: tick every second for 3600 seconds
		for (long t = 1_000L; t <= 3_601_000L; t += 1_000L) {
			tracker.tickInstanceTimer(t, true);
		}

		tracker.recordKill(1_000L);
		tracker.recordKill(1_800_000L);
		tracker.recordDeath();
		assertTrue(tracker.acceptsLoot(1_800_001L));
		tracker.recordLoot(250_000L);
		tracker.recordItemSpent(25_000L); // e.g. food + potion + house tab

		// Elapsed wall clock time could be 10 hours (36,000,000ms), but active instance time is 1 hour (3,600,000ms)
		SessionTracker.Snapshot stats = tracker.snapshot(36_000_000L, 12_000L);
		assertEquals(2, stats.kills);
		assertEquals(1, stats.deaths);
		assertEquals(3_600_000L, stats.instanceTimeMillis);
		assertEquals(2_000L, stats.xp);
		assertEquals(250_000L, stats.gpGained);
		assertEquals(125_000L, stats.gpSpent); // 100k death reclaim + 25k supplies
		assertEquals(125_000L, stats.netGp);

		// Rates should be extrapolated based on 1 hour active instance time
		assertEquals(2L, stats.killsPerHour);
		assertEquals(2_000L, stats.xpPerHour);
		assertEquals(250_000L, stats.gpPerHour);
		assertEquals(125_000L, stats.netGpPerHour);
	}

	@Test
	public void testResetSession()
	{
		SessionTracker tracker = new SessionTracker();
		tracker.startIfNeeded(1_000L, 10_000L);
		tracker.recordKill(1_000L);
		tracker.recordLoot(100_000L);
		tracker.recordItemSpent(5_000L);

		SessionTracker.Snapshot before = tracker.snapshot(2_000L, 10_000L);
		assertEquals(1, before.kills);
		assertEquals(100_000L, before.gpGained);
		assertEquals(5_000L, before.gpSpent);

		tracker.resetSession(3_000L, 15_000L);
		SessionTracker.Snapshot after = tracker.snapshot(3_000L, 15_000L);
		assertEquals(0, after.kills);
		assertEquals(0, after.deaths);
		assertEquals(0L, after.gpGained);
		assertEquals(0L, after.gpSpent);
		assertEquals(0L, after.netGp);
	}
}

