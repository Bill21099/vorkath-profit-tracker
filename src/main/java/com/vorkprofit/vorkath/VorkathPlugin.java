package com.vorkprofit.vorkath;

import com.vorkprofit.vorkath.tracker.SessionTracker;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

/**
 * Vorkath Profit Tracker — session-based loot and cost tracker for Vorkath.
 *
 * <p>Key differences from the Zulrah version:
 * <ul>
 *   <li>Vorkath NPC IDs: 8058 (pre-fight), 8059 (main fight), 8060, 8061 (phase variants)</li>
 *   <li>Vorkath instance region IDs: 9023, 9279 (Ungael island)</li>
 *   <li>Tracks "Home Teleport" (tablet / spell) as a cost instead of Zul-andra teleports</li>
 *   <li>Panel accent colour is icy-blue rather than orange</li>
 * </ul>
 */
@PluginDescriptor(
	name        = "Vorkath Profit Tracker",
	description = "Tracks Vorkath session time, XP, loot and costs",
	tags        = {"vorkath", "gp", "profit", "tracker"},
	enabledByDefault = true
)
@Singleton
public final class VorkathPlugin extends Plugin
{
	// ── Vorkath NPC IDs ─────────────────────────────────────────────────────────
	// 8058 = sleeping/pre-fight, 8059 = main phase, 8060 = zombie-spawn phase, 8061 = enraged
	private static final int[] VORKATH_IDS = {8058, 8059, 8060, 8061};

	// ── Vorkath instance region IDs (Ungael) ────────────────────────────────────
	private static final int[] VORKATH_REGIONS = {9023, 9279};

	@Inject private Client client;
	@Inject private ClientToolbar toolbar;
	@Inject private SessionTracker session;
	@Inject private ItemManager itemManager;

	private NPC vorkath;
	private VorkathPanel panel;
	private NavigationButton button;
	private boolean deathRecorded;
	private WorldPoint lootOrigin;
	private int pendingItemId = -1;
	private long pendingItemClickTime;
	private Map<Integer, Integer> lastInventory = new HashMap<>();

	// ── Lifecycle ────────────────────────────────────────────────────────────────

	@Override
	protected void startUp()
	{
		panel = new VorkathPanel(session, client);
		button = NavigationButton.builder()
			.tooltip("Vorkath Profit Tracker")
			.priority(8)
			.icon(VorkathIcon.create())
			.panel(panel)
			.build();
		toolbar.addNavigation(button);
	}

	@Override
	protected void shutDown()
	{
		if (button != null) toolbar.removeNavigation(button);
		vorkath = null;
		panel = null;
	}

	// ── Game tick: instance detection & panel refresh ─────────────────────────

	@Subscribe
	public void onGameTick(GameTick event)
	{
		long now = System.currentTimeMillis();
		session.startIfNeeded(now, combatExperience());

		boolean inInstance = isInVorkathInstance();
		session.tickInstanceTimer(now, inInstance);

		if (vorkath == null)
			for (NPC npc : client.getNpcs())
				if (isVorkath(npc)) { vorkath = npc; deathRecorded = false; break; }

		if (panel != null) panel.refresh();
	}

	// ── NPC tracking ─────────────────────────────────────────────────────────

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (isVorkath(event.getNpc())) { vorkath = event.getNpc(); deathRecorded = false; }
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == vorkath) { vorkath = null; deathRecorded = false; }
	}

	// ── Kill / death recording ─────────────────────────────────────────────────

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == vorkath && !deathRecorded)
		{
			deathRecorded = true;
			lootOrigin = vorkath.getWorldLocation();
			session.recordKill(System.currentTimeMillis());
		}
		else if (event.getActor() == client.getLocalPlayer())
		{
			session.recordDeath();
		}
	}

	// ── Loot tracking ─────────────────────────────────────────────────────────

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		WorldPoint location = event.getTile().getWorldLocation();
		if (!session.acceptsLoot(System.currentTimeMillis())
			|| lootOrigin == null
			|| location.distanceTo2D(lootOrigin) > 8) return;

		TileItem item = event.getItem();
		session.recordLoot((long) item.getQuantity() * itemManager.getItemPrice(item.getId()));
	}

	// ── Consumable / cost tracking ─────────────────────────────────────────────

	/**
	 * Intercepts clicks on consumables and home teleport tablets/spells so we can
	 * correlate them with the subsequent inventory-change event.
	 *
	 * <p>For Vorkath we track:
	 * <ul>
	 *   <li>Food (eat)</li>
	 *   <li>Potions (drink)</li>
	 *   <li>Teleport tablets / runes (teleport / break) — covers "Home Teleport" tablets
	 *       and POH teleport tablets used to return after a kill</li>
	 *   <li>Prayer flick items (read) e.g. Dragonfire shields are not consumables but
	 *       scrolls/prayer books with a "read" action would be counted</li>
	 * </ul>
	 */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		int itemId = event.getItemId();
		if (itemId <= 0) return;

		ItemComposition def = itemManager.getItemComposition(itemId);
		if (def == null || def.getName() == null) return;

		String name   = def.getName().toLowerCase(Locale.ROOT);
		String option = event.getMenuOption() == null ? "" : event.getMenuOption().toLowerCase(Locale.ROOT);

		// Never count depositing/storing items as a cost
		if (option.startsWith("deposit") || option.startsWith("store")) return;

		boolean trackedAction = "eat".equals(option)
			|| "drink".equals(option)
			|| "teleport".equals(option)
			|| "break".equals(option)
			|| "read".equals(option);

		// Explicitly catch house teleports, home teleports, and Rellekka teleports by name
		// (e.g. Teleport to house tablet, House teleport tablet, Construction cape, Rellekka teleport tablet)
		if (!trackedAction && (name.contains("teleport to house")
			|| name.contains("house teleport")
			|| name.contains("home teleport")
			|| name.contains("rellekka teleport")
			|| name.contains("construction cape")))
		{
			trackedAction = true;
		}

		// Fall back: check the item's own inventory actions for eat/drink/teleport/break
		if (!trackedAction && def.getInventoryActions() != null)
			for (String action : def.getInventoryActions())
				if (action != null && action.equalsIgnoreCase(option)
					&& ("eat".equalsIgnoreCase(action) || "drink".equalsIgnoreCase(action)
					|| "teleport".equalsIgnoreCase(action) || "break".equalsIgnoreCase(action)
					|| "read".equalsIgnoreCase(action)))
				{
					trackedAction = true;
					break;
				}

		if (trackedAction) { pendingItemId = itemId; pendingItemClickTime = System.currentTimeMillis(); }
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId()
			|| event.getItemContainer() == null) return;

		Map<Integer, Integer> current = new HashMap<>();
		for (Item item : event.getItemContainer().getItems())
			if (item != null && item.getId() > 0)
				current.put(item.getId(), current.getOrDefault(item.getId(), 0) + item.getQuantity());

		if (!lastInventory.isEmpty() && System.currentTimeMillis() - pendingItemClickTime < 3000)
		{
			for (Map.Entry<Integer, Integer> entry : lastInventory.entrySet())
			{
				int id = entry.getKey();
				int consumed = entry.getValue() - current.getOrDefault(id, 0);
				if (id == pendingItemId && consumed > 0)
				{
					ItemComposition def = itemManager.getItemComposition(id);
					if (def != null && def.getName() != null
						&& isTrackedConsumable(def, def.getName().toLowerCase(Locale.ROOT)))
					{
						long unitCost = calculateItemCost(def.getName().toLowerCase(Locale.ROOT),
							itemManager.getItemPrice(id));
						session.recordItemSpent(unitCost * consumed);
					}
					pendingItemId = -1;
					break;
				}
			}
		}
		lastInventory = current;
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	/**
	 * Returns true when the player is inside the Vorkath (Ungael) instanced region.
	 * We check both {@code isInInstancedRegion()} and the specific region IDs to avoid
	 * false positives in other instanced content.
	 */
	private boolean isInVorkathInstance()
	{
		if (!client.isInInstancedRegion()) return false;
		int[] regions = client.getMapRegions();
		if (regions == null) return false;
		for (int region : regions)
			for (int vorkRegion : VORKATH_REGIONS)
				if (region == vorkRegion) return true;
		return false;
	}

	private boolean isTrackedConsumable(ItemComposition def, String name)
	{
		// Potion doses, teleport tablets and home/house teleport items
		if (name.contains("teleport to house")
			|| name.contains("house teleport")
			|| name.contains("home teleport")
			|| name.contains("rellekka teleport")
			|| name.matches(".*\\([1-4]\\)$")) return true;
		String[] actions = def.getInventoryActions();
		if (actions != null)
			for (String action : actions)
				if ("eat".equalsIgnoreCase(action) || "drink".equalsIgnoreCase(action)
					|| "break".equalsIgnoreCase(action) || "teleport".equalsIgnoreCase(action)) return true;
		return false;
	}

	/**
	 * Converts a full-item price to a per-use (per-dose) cost.
	 * Teleport tablets are single-use; potions are divided by their current dose count.
	 */
	private long calculateItemCost(String name, long price)
	{
		// Single-use teleport items — charge the full GE price
		if (name.contains("teleport to house")
			|| name.contains("house teleport")
			|| name.contains("home teleport")
			|| name.contains("rellekka teleport")) return price;
		// Potions: "(4)" "(3)" "(2)" "(1)" — pro-rate by dose
		Matcher matcher = Pattern.compile(".*\\((\\d)\\)$").matcher(name);
		if (matcher.matches())
		{
			int doses = Integer.parseInt(matcher.group(1));
			if (doses > 0) return Math.max(1, price / doses);
		}
		return price;
	}

	private boolean isVorkath(NPC npc)
	{
		if (npc == null) return false;
		int id = npc.getId();
		for (int vid : VORKATH_IDS)
			if (id == vid) return true;
		return false;
	}

	private long combatExperience()
	{
		long total = 0;
		for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC})
			total += client.getSkillExperience(skill);
		return total;
	}
}

