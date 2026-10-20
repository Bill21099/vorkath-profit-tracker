package com.vorkprofit.vorkath;

import com.vorkprofit.vorkath.tracker.SessionTracker;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public final class VorkathPanel extends PluginPanel
{
	private static final Color ACCENT = new Color(100, 160, 255);   // icy-blue to suit Vorkath
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;

	private final SessionTracker session;
	private final Client client;

	private final JLabel status     = value();
	private final JLabel sessionTime = value();
	private final JLabel kills      = value();
	private final JLabel deaths     = value();
	private final JLabel xp         = value();
	private final JLabel xpHour     = value();
	private final JLabel gpGained   = value();
	private final JLabel gpSpent    = value();
	private final JLabel netGp      = value();
	private final JLabel gpHour     = value();

	public VorkathPanel(SessionTracker session, Client client)
	{
		super(false);
		this.session = session;
		this.client = client;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

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
		row(card, "Instance time", sessionTime);
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
		content.add(Box.createVerticalStrut(8));

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

		add(content, BorderLayout.NORTH);
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
		status.setText("TRACKING");
		status.setForeground(ACCENT);
		sessionTime.setText(duration(s.instanceTimeMillis));
		kills.setText(number(s.kills));
		deaths.setText(number(s.deaths));
		xp.setText(number(s.xp));
		xpHour.setText(number(s.xpPerHour));
		gpGained.setText(number(s.gpGained));
		gpSpent.setText(number(s.gpSpent));
		netGp.setText(number(s.netGp));
		gpHour.setText(number(s.netGpPerHour));
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
}

