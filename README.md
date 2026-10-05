# ColorTube — Water Sort Puzzle Game

A water sort color puzzle game for Android built with Kotlin. Features 850 levels, fluid animations, custom glass tube silhouettes, theme customization, and an integrated mathematical solvability validator.

---

## 🧪 Features

- **850 Verified Solvable Levels:** Progressive difficulty curve ranging from gentle 2-color tutorial stages to expert 10-color / 12-tube challenges.
- **Built-in Solvability Verification:** Integrated Breadth-First Search (BFS) puzzle validator guarantees that every generated layout is 100% mathematically solvable without deadlocks.
- **16 Unique Collectible Tube Silhouettes:**
  - Classic Glass, Chemist Flask, Slender Vial, Crystal Flute
  - Alchemist Bulb, Square Decanter, Narrow Droplet, Apothecary Jar
  - Hourglass Vase, Fantasy Elixir, Octagon Prism, Tall Chalice
  - Ancient Amphora, Celestial Vial, Royal Decanter, Mystic Relic
- **Premium Glassmorphism Aesthetic:** Custom Canvas-rendered multi-layered liquid shaders, surface meniscus, rising carbonation bubbles, and tube completion celebrations.
- **Collection & Long-Term Economy:**
  - Dual unlock system across **Tubes**, **Themes**, and **Color Palettes**.
  - Progressive Coin Economy (3,500 to 15,000+ coins) paired with milestone-based level unlocks.
- **Player Assists:**
  - **Undo:** Full history stack to revert previous moves.
  - **Hint:** Intelligent solver-assisted next move indicator.
  - **Restart:** Instant board reset.
  - **+Tube:** Emergency expansion slot for complex puzzle states.
- **Monetization & AdMob Integration:**
  - Production-ready anchored adaptive bottom banner ad container isolated from the interactive game board.
  - Rewarded ads for bonus coins and hints.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **Platform:** Android (minSdk 24, targetSdk 35, compileSdk 36)
- **UI & Graphics:** Native Android Canvas Custom Views with multi-layer hardware-accelerated drawing.
- **Audio:** Native SoundPool with custom synthesized puzzle tones.
- **Persistence:** Encrypted/SharedPreferences for level progression, coins, and cosmetic unlocks.
- **Testing:** Comprehensive JUnit test suite including full-spectrum BFS state solvability auditing.

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17 or higher
- Android SDK 35+

### Build from Terminal
```bash
# Clone the repository
git clone https://github.com/yogeshmadhukar-apps/ColorTube-Game.git
cd ColorTube-Game

# Build Debug APK
./gradlew assembleDebug

# Run unit & solvability tests
./gradlew testDebugUnitTest
```

---

## 📄 License
All rights reserved © 2026 Yogesh Madhukar.
