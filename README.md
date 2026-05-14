<pre align="center">
   ___       __  ___       __    __      _                       ___
  / _ \___ _/ /_/ _ \___ _/ /_  / /_____(_)__ ___   _______  ___/ (_)__  ___ _
 / // / _ `/ __/ , _/ _ `/ __/ / __/ __/ / -_|_-<  / __/ _ \/ _  / / _ \/ _ `/
/____/\_,_/\__/_/|_|\_,_/\__/  \__/_/ /_/\__/___/  \__/\___/\_,_/_/_//_/\_, /
                                                                       /___/
</pre>

<p align="center">
  <img alt="Forge" src="https://img.shields.io/badge/Forge-555?style=for-the-badge">
  <img alt="1.7.10" src="https://img.shields.io/badge/1.7.10-555?style=for-the-badge">
</p>

<p align="center">
  <a href="#features">Features</a> ·
  <a href="#quest-rewarding-system">Quest Rewarding System</a> ·
  <a href="#party-mode-support">Party Mode Support</a> ·
  <a href="#unsupported-quest-types">Unsupported Quest Types</a> ·
  <a href="#build-instructions">Build Instructions</a>
</p>

# Hardcore Questing Mode: Quest Rewarding System

A Minecraft `1.7.10` Forge addon that adds an HQM-compatible block for automatically claiming fixed quest rewards into an automation-friendly inventory.

## Features

- **Quest Rewarding System Block**: Adds a standalone `Quest Rewarding System` block for Hardcore Questing Mode.
- **Quest Book Binding**: Binds to the currently selected HQM quest through the quest book, similar to the vanilla Quest Delivery System flow.
- **Automatic Reward Claiming**: Claims completed fixed item rewards into a 9-slot internal inventory.
- **Atomic Item Insertion**: Waits when rewards do not fit instead of inserting partial rewards or voiding items.
- **Pick-One Rejection**: Refuses quests with pick-one rewards because there is no reward-choice UI.
- **Party Mode Support**: Handles HQM `ALL`, `ANY`, and `RANDOM` reward settings from the bound player's context.
- **Automation-Oriented Inventory**: Allows extraction from all slots and rejects item insertion.

## Quest Rewarding System

The block follows the same player-facing binding pattern as HQM's Quest Delivery System:

- Open the HQM quest book.
- Select the quest to bind.
- Right-click a placed `Quest Rewarding System` with the quest book.
- Complete the bound quest.
- If the bound player is online and the block has space, fixed item rewards appear in the block inventory.

The block has no GUI. Empty-hand or non-book right-clicks print status chat, including unbound state, bound quest, offline player, pending completion, inventory-space waits, and unsupported quest types.

If the internal inventory cannot fit the full fixed reward set, the block inserts nothing and leaves the HQM reward unclaimed for a later retry.

## Party Mode Support

The bound player is always the reward claimant.

- **ALL**: Claims the bound player's own reward entitlement.
- **ANY**: Consumes the shared team reward entitlement like vanilla HQM reward claiming.
- **RANDOM**: Claims only when the bound player is the selected reward recipient.

The bound player must be online for automatic claiming. Offline players are skipped until the fallback tick check sees the player online again.

## Unsupported Quest Types

- Pick-one reward quests are intentionally rejected.
- Quests with fixed item rewards are supported.
- Reputation rewards are applied to the bound player's team alongside item reward claiming.
- Reputation-only quests are supported when HQM exposes a claimable reward state.

## Coremod Approach

Most behavior is implemented as a normal Forge block and tile entity.

- **Late Completion Hook**: Uses UniMixins to inject after `hardcorequesting.quests.QuestTask.completeQuest(Quest, String)`.
- **Loaded Tile Registry**: Notifies loaded Quest Rewarding System tiles bound to the completed quest and player.
- **Fallback Tick Check**: Performs a low-frequency retry on loaded tiles to catch missed completion paths, offline-player changes, and inventory-space changes.
- **Scoped Patch**: The mixin only observes quest completion; vanilla HQM reward screens and Quest Delivery Systems are not replaced.

## Build Instructions

Required local dependency jars go in `deps/`:

- `deps/HQM-The Journey-4.4.4.jar`
- `deps/+unimixins-all-1.7.10-0.3.0.jar`

Example placeholder files are included in `deps/` to document the expected jar names.

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk ./gradlew --no-daemon clean build
```

The built JAR will be located at:

```text
build/libs/hqm-questrewardingsystem-1.7.10-1.0.1.jar
```
