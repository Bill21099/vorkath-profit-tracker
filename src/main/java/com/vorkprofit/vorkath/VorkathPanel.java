package com.vorkprofit.vorkath;

import com.vorkprofit.vorkath.tracker.SessionTracker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

public final class VorkathPanel extends PluginPanel
{
	private static final Color ACCENT = new Color(100, 160, 255);   // icy-blue to suit Vorkath
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;

	private final SessionTracker session;
	private final Client client;
	private final ItemManager itemManager;

	private final JLabel status       = value();
	private final JLabel sessionTime  = value();
	private final JLabel instanceTime = value();
	private final JLabel idleTime     = value();
	private final JLabel kills        = value();
	private final JLabel deaths       = value();
	private final JLabel xp           = value();
	private final JLabel xpHour       = value();
	private final JLabel gpGained     = value();
	private final JLabel gpSpent      = value();
	private final JLabel netGp        = value();
	private final JLabel gpHour       = value();

	private final JLabel dropsTitle      = new JLabel("Vorkath x 0");
	private final JLabel dropsTotalValue = new JLabel("0 gp");
	private final JPanel dropsGridPanel  = new JPanel();
	private Map<Integer, Integer> lastRenderedItems = new LinkedHashMap<>();

	public VorkathPanel(SessionTracker session, Client client, ItemManager itemManager)
	{
		super(false);
		this.session = session;
		this.client = client;
		this.itemManager = itemManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(new EmptyBorder(10, 10, 10, 10));

		// ── Header ──────────────────────────────────────────────
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		JLabel title = new JLabel("VORKATH PROFIT TRACKER");
		title.setForeground(ACCENT);
		title.setFont(FontManager.getRunescapeBoldFont());
		header.add(title, BorderLayout.WEST);
		header.add(status, BorderLayout.EAST);
		content.add(header);
		content.add(Box.createVerticalStrut(10));

		// ── Stats card ──────────────────────────────────────────
		JPanel card = new JPanel();
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(8, 10, 8, 10));
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

		section(card, "SESSION");
		row(card, "Session time", sessionTime);
		row(card, "Instance time", instanceTime);
		row(card, "Idle time", idleTime);
		card.add(Box.createVerticalStrut(6));
		row(card, "Kills", kills);
		row(card, "Deaths", deaths);
		card.add(Box.createVerticalStrut(6));
		row(card, "XP gained", xp);
		row(card, "XP / hour", xpHour);
		card.add(Box.createVerticalStrut(6));
		row(card, "GP gained", gpGained);
		row(card, "GP spent", gpSpent);
		row(card, "Net profit", netGp);
		row(card, "Net GP / hour", gpHour);

		content.add(card);
		content.add(Box.createVerticalStrut(10));

		// ── Drops section ───────────────────────────────────────
		JPanel dropsCard = new JPanel(new BorderLayout());
		dropsCard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		dropsCard.setBorder(new EmptyBorder(0, 0, 0, 0));

		JPanel dropsHeader = new JPanel(new BorderLayout());
		dropsHeader.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		dropsHeader.setBorder(new EmptyBorder(6, 8, 6, 8));

		dropsTitle.setForeground(Color.WHITE);
		dropsTitle.setFont(FontManager.getRunescapeBoldFont());
		dropsHeader.add(dropsTitle, BorderLayout.WEST);

		dropsTotalValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		dropsTotalValue.setFont(FontManager.getRunescapeSmallFont());
		dropsHeader.add(dropsTotalValue, BorderLayout.EAST);

		dropsCard.add(dropsHeader, BorderLayout.NORTH);

		dropsGridPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		dropsCard.add(dropsGridPanel, BorderLayout.CENTER);

		content.add(dropsCard);
		content.add(Box.createVerticalStrut(10));

		// ── Reset button ────────────────────────────────────────
		JButton resetButton = new JButton("Reset Session");
		resetButton.setToolTipText("Clear kills, deaths, timers, XP and GP totals for a new session");
		resetButton.addActionListener(e -> {
			int choice = JOptionPane.showConfirmDialog(this,
				"Reset all Vorkath Profit Tracker session statistics?", "Reset Session",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice == JOptionPane.YES_OPTION)
			{
				session.resetSession(System.currentTimeMillis(), combatExperience());
				refresh();
			}
		});
		resetButton.setAlignmentX(LEFT_ALIGNMENT);
		content.add(resetButton);

		// Wrapper & scroll pane for smooth vertical scrolling
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);
		wrapper.add(content, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(wrapper);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		scrollPane.getVerticalScrollBar().setBorder(new EmptyBorder(0, 0, 0, 0));

		add(scrollPane, BorderLayout.CENTER);
		refresh();
	}

	// ── Helpers ─────────────────────────────────────────────────

	private void section(JPanel panel, String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ACCENT);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setBorder(new EmptyBorder(0, 0, 5, 0));
		panel.add(label);
	}

	private void row(JPanel panel, String text, JLabel valueLbl)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		JLabel label = new JLabel(text);
		label.setForeground(MUTED);
		label.setFont(FontManager.getRunescapeSmallFont());
		valueLbl.setForeground(Color.WHITE);
		valueLbl.setFont(FontManager.getRunescapeSmallFont());
		row.add(label, BorderLayout.WEST);
		row.add(valueLbl, BorderLayout.EAST);
		panel.add(row);
	}

	private static JLabel value() { return new JLabel("—"); }

	// ── Refresh ─────────────────────────────────────────────────

	public void refresh()
	{
		SessionTracker.Snapshot s = session.snapshot(System.currentTimeMillis(), combatExperience());
		if (session.isStarted())
		{
			status.setText("TRACKING");
			status.setForeground(ACCENT);
		}
		else
		{
			status.setText("WAITING");
			status.setForeground(MUTED);
		}

		sessionTime.setText(duration(s.sessionTimeMillis));
		instanceTime.setText(duration(s.instanceTimeMillis));
		idleTime.setText(duration(s.idleTimeMillis));
		kills.setText(number(s.kills));
		deaths.setText(number(s.deaths));
		xp.setText(number(s.xp));
		xpHour.setText(number(s.xpPerHour));
		gpGained.setText(number(s.gpGained));
		gpSpent.setText(number(s.gpSpent));
		netGp.setText(number(s.netGp));
		gpHour.setText(number(s.netGpPerHour));

		dropsTitle.setText("Vorkath x " + s.kills);
		dropsTotalValue.setText(QuantityFormatter.quantityToStackSize(s.gpGained) + " gp");
		dropsTotalValue.setToolTipText("Total value: " + QuantityFormatter.formatNumber(s.gpGained) + " gp");

		if (!s.items.equals(lastRenderedItems))
		{
			lastRenderedItems = new LinkedHashMap<>(s.items);
			rebuildDropsGrid(s.items);
		}
	}

	private void rebuildDropsGrid(Map<Integer, Integer> items)
	{
		dropsGridPanel.removeAll();
		if (items.isEmpty())
		{
			JPanel emptyPanel = new JPanel();
			emptyPanel.setOpaque(false);
			emptyPanel.setBorder(new EmptyBorder(8, 0, 8, 0));
			JLabel emptyLabel = new JLabel("No drops recorded yet");
			emptyLabel.setFont(FontManager.getRunescapeSmallFont());
			emptyLabel.setForeground(MUTED);
			emptyPanel.add(emptyLabel);

			dropsGridPanel.setLayout(new BorderLayout());
			dropsGridPanel.add(emptyPanel, BorderLayout.CENTER);
			dropsGridPanel.revalidate();
			dropsGridPanel.repaint();
			return;
		}

		List<LootDisplayItem> displayItems = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : items.entrySet())
		{
			int id = entry.getKey();
			int qty = entry.getValue();
			ItemComposition comp = itemManager.getItemComposition(id);
			String name = comp != null ? comp.getName() : "Item " + id;
			long unitPrice = itemManager.getItemPrice(id);
			displayItems.add(new LootDisplayItem(id, qty, name, unitPrice));
		}

		// Sort by total price descending, then quantity, then name
		displayItems.sort((a, b) -> {
			int cmp = Long.compare(b.totalPrice, a.totalPrice);
			if (cmp != 0) return cmp;
			cmp = Integer.compare(b.qty, a.qty);
			if (cmp != 0) return cmp;
			return a.name.compareToIgnoreCase(b.name);
		});

		int total = displayItems.size();
		int rows = (int) Math.ceil((double) total / 5.0);
		dropsGridPanel.setLayout(new GridLayout(rows, 5, 1, 1));
		dropsGridPanel.setBorder(new EmptyBorder(1, 1, 1, 1));

		for (LootDisplayItem item : displayItems)
		{
			JPanel slot = new JPanel(new BorderLayout());
			slot.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			slot.setPreferredSize(new Dimension(36, 32));

			JLabel iconLabel = new JLabel();
			iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
			iconLabel.setVerticalAlignment(SwingConstants.CENTER);

			String tooltip = "<html>" + item.name
				+ (item.qty > 1 ? " x " + QuantityFormatter.formatNumber(item.qty) : "")
				+ " (" + QuantityFormatter.formatNumber(item.totalPrice) + " gp)</html>";
			iconLabel.setToolTipText(tooltip);

			AsyncBufferedImage img = itemManager.getImage(item.id, item.qty, item.qty > 1);
			img.addTo(iconLabel);

			slot.add(iconLabel, BorderLayout.CENTER);
			dropsGridPanel.add(slot);
		}

		// Fill remainder of row with empty slots to preserve 5-column alignment
		int remainder = total % 5;
		if (remainder > 0)
		{
			int emptySlots = 5 - remainder;
			for (int i = 0; i < emptySlots; i++)
			{
				JPanel emptySlot = new JPanel();
				emptySlot.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				emptySlot.setPreferredSize(new Dimension(36, 32));
				dropsGridPanel.add(emptySlot);
			}
		}

		dropsGridPanel.revalidate();
		dropsGridPanel.repaint();
	}

	// ── Utilities ───────────────────────────────────────────────

	private long combatExperience()
	{
		long total = 0;
		for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC})
			total += client.getSkillExperience(skill);
		return total;
	}

	private static String number(long n) { return String.format("%,d", n); }

	private static String duration(long millis)
	{
		long seconds = millis / 1000, hours = seconds / 3600, minutes = (seconds % 3600) / 60;
		return hours > 0
			? String.format("%d:%02d:%02d", hours, minutes, seconds % 60)
			: String.format("%02d:%02d", minutes, seconds % 60);
	}

	private static final class LootDisplayItem
	{
		final int id;
		final int qty;
		final String name;
		final long unitPrice;
		final long totalPrice;

		LootDisplayItem(int id, int qty, String name, long unitPrice)
		{
			this.id = id;
			this.qty = qty;
			this.name = name;
			this.unitPrice = unitPrice;
			this.totalPrice = unitPrice * qty;
		}
	}
}
