# Changelog

All notable changes to Socket Sorcery.

## [Unreleased]

### Changed
- **Ported to Minecraft 26.1** (from 1.21.1). Requires Java 25 and Fabric Loader ≥ 0.18.4.
- **Trinkets → Trinkets (Updated)**: the original has no 26.x build, so accessories now depend on
  the maintained `trinkets_updated` fork (≥ 4.0.0-beta.3).
- **EMI → JEI**: EMI has no 26.1 build. The Engraving and Socketing categories carry over
  unchanged, still generated from the synced pattern registry so datapack content appears
  automatically.
- **Engraved-gem icons are now data-driven.** Opting a modded item into the composited engraved icon
  no longer needs a Java call — point its item model definition at `socket-sorcery:engraved_gem`
  (see `DATA_DRIVEN.md`). `SocketSorceryClientApi.registerEngravableGem` is deprecated and does
  nothing.
- Recipe ingredients moved to the current format, and `minecraft:chain` became
  `minecraft:iron_chain` — the necklace recipe is unchanged in substance.

### Added
- **Data-driven patterns & modifiers**: every pattern, modifier, and gem/scroll pairing is now JSON,
  loaded through synced dynamic registries (like vanilla enchantments). Datapacks can add patterns,
  make any item engravable, and register scrolls with no code — see `DATA_DRIVEN.md`.
- **Advancement progression**: a full engraving branch (First Cut → Deeper Still → Master Engraver)
  and accessory branch (Well Equipped → Sorcery Unleashed), driven by three custom criteria.
- **In-game guidebook** (Patchouli): the *Engraver's Handbook*, covering the full loop, every
  pattern's symbol and effects, modifiers, gems, accessories and treasure.
- **Recipe viewer integration**: Engraving and Socketing recipe categories, plus all crafting recipes.
- **Configuration** (Cloth Config + Mod Menu): loot chances, artifact gem counts, cooldown
  multiplier, bangle reach.
- **6 new patterns**: Wind, Earth, Lifesteal, Blink, Haste, Spikes.
- **4 new engravable gems**: Prismarine, Glowstone, Copper, Ender Pearl.
- **Ring** accessory: a single-socket reactive accessory whose gem fires automatically when the
  wearer takes damage.
- **Tiered chisels**: Diamond (more durability) and Netherite (more durability + cheaper carves).
- Amethyst gem is now craftable from amethyst shards.
- Sounds, particles, and UI feedback across the whole loop: carve ticks, engrave success/failure
  results, live engraved-gem preview, socket chimes, cooldown HUD countdown and ready chime.
- Advancement tree and item tags.

### Changed
- Engraved gems and socketed accessories now render as properly extruded 3D icons.
- Balance constants consolidated (and exposed through the config).

## [1.0.0]

- Initial release: 5 patterns, 7 modifiers, 10 gems, 5 scrolls, Engraving Table, Socketing
  Bench, Necklace and Bangle accessories via Trinkets, treasure loot with pre-socketed artifacts.
