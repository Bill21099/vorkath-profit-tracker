# Vorkath Profit Tracker

A RuneLite plugin that tracks your Vorkath sessions in Old School RuneScape, providing real-time statistics on kills, deaths, session duration, combat experience, loot value, supply costs, and net GP profit per hour.

---

## Features

- **Session Time Tracking:** Automatically detects when you are inside the Vorkath encounter instance on Ungael and records active fight duration.
- **Kill & Death Counter:** Automatically increments kills upon Vorkath's defeat and tracks player deaths during encounters.
- **Loot Tracking:** Monitors drop spawns within the arena and calculates loot value using real-time Grand Exchange pricing.
- **Consumable & Supply Cost Calculation:**
  - Tracks food eaten and potions consumed, pro-rating potion costs per dose.
  - Tracks teleport tablets and return methods (such as House teleport tablets and Rellekka teleport tablets).
- **Net Profit & Rates:**
  - Real-time calculation of Gross GP gained vs. GP spent.
  - Computes net profit and hourly projections (`GP / hour` and `XP / hour`).
- **Sidebar Panel:** Clean, icy-themed UI panel on the RuneLite sidebar displaying all session metrics with a one-click session reset option.

---

## How It Works

1. **Instance & Boss Detection:** The plugin listens for Vorkath's NPC spawns and verifies that you are within the Ungael instance regions (`9023`, `9279`).
2. **Combat & Drops:** When Vorkath dies, ground drops within the arena are aggregated using RuneLite's `ItemManager` Grand Exchange prices.
3. **Supplies:** When inventory items decrease immediately following an eat, drink, or break action, their unit/dose cost is added to your session expenses.

---

## Development & Building

### Requirements
- Java 11 or higher
- Gradle (wrapper included)

### Building
To build the plugin JAR:
```bash
./gradlew build
```

### Running Locally
To launch a development RuneLite client with this plugin loaded:
```bash
./gradlew run
```

### Running Tests
To run unit tests:
```bash
./gradlew test
```

---

## License

This project is licensed under the **BSD 2-Clause License**. See the [LICENSE](LICENSE) file for details.

