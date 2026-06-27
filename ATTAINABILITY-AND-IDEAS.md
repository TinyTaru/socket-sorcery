# Socket Sorcery — Attainability Audit & Accessory Acquisition Design

## Context

**Socket Sorcery** (Fabric, MC 1.21.1, by *tinytaru*) adds an RPG-style engraving/socketing system:
find pattern **scrolls** → chisel a pattern onto a **gem** at the Engraving Table → socket engraved
gems into a **necklace** (passive) or **bangle** (active) at the Socketing Bench → wear them via Trinkets.

An attainability pass found 7 items with no survival path. After review, the decision is:

- **The 5 custom gems (Ruby, Sapphire, Peridot, Amethyst, Topaz) stay creative-only** — a deliberate
  prestige tier. This is safe (see finding #2 below): every pattern they could hold is already reachable
  through a craftable engravable gem, so no *function* is locked away.
- **The Necklace and Bangle are the real gap and must be fixed** — they are the mod's entire payoff, and
  with no recipe or loot the mod is currently incompletable in survival.

The user asked for an analysis plus **creative, entertaining** acquisition. Per the locked-in decisions, the
accessories should be both **craftable** (so the mod is always completable) **and occasionally found as
treasure** (so there's a thrill). Originally a design doc, this now also records the **implemented** feature —
see *Implementation status* at the end.

---

## Executive summary

| Content | Current survival source | Status |
|---|---|---|
| Chisel, Engraving Table, Socketing Bench | Crafting | ✅ |
| 5 Scrolls | Injected into 8 structure loot tables @ 50% | ✅ |
| 5 Engravable gems (diamond/redstone/lapis/emerald/quartz) | Crafting | ✅ |
| 5 Custom gems (ruby/sapphire/peridot/amethyst/topaz) | none | ◻️ creative-only **by design** |
| **Necklace** | Crafting (chain + gold) + rare treasure loot | ✅ added |
| **Bangle** | Crafting (gold ring) + rare treasure loot | ✅ added |

**Two findings:**

1. **The Necklace & Bangle are a hard blocker.** They're the climax of every gameplay loop, yet have no
   recipe and appear in no loot table. A survival player can build both benches, craft gems, find scrolls,
   and engrave flawlessly — then have nowhere to put any of it. This is the one gap that breaks the mod.
2. **Leaving the custom gems creative-only costs nothing functional.** All 5 patterns are already reachable
   via *craftable* engravable gems (Fire→redstone/quartz, Frost→diamond/lapis/quartz, Healing→diamond/emerald,
   Lightning→redstone, Leaping→lapis/emerald). So the custom gems are pure flavor/prestige — fine to keep as a
   creative-tab showcase.

The fix is low-risk: the mod already injects items into loot tables via `LootTableEvents.MODIFY`
([ModLoot.java](src/main/java/tinytaru/socketsorcery/registry/ModLoot.java)) and ships
hand-written shaped recipes (e.g.
[engravable_diamond.json](src/main/resources/data/socket-sorcery/recipe/engravable_diamond.json)).
Both accessory ideas reuse one of those two existing patterns.

---

## The Accessories (craftable + treasure)

These are *tools*, not trophies, so each gets a reliable craft **and** a rare "I found a real one" thrill.

### Necklace — "the empty setting"
*Flavor: a bare pendant of gold and chain, sockets waiting to be filled.*

- **Craft (reliable):** a pendant shape — a chain clasp over a gold setting.
  ```
  " C "      C = chain       →  socket-sorcery:necklace
  "G G"      G = gold ingot
  " G "
  ```
- **Treasure (the thrill):** rarely found as an **heirloom necklace** in high-tier loot — *end city*,
  *ancient city*, *woodland mansion*, *trial-chamber vaults*. Two tiers possible:
  - *Easy:* a plain (empty) necklace as a loot entry — trivial.
  - *Fancy:* a **pre-socketed artifact** already carrying 1–3 random engraved gems — an instant power
    spike. *Lore: "An adventurer before you never made it home."*

### Bangle — "the warrior's band"
*Flavor: a gold wrist-ring that channels active sorcery on command.*

- **Craft (reliable):** a ring of gold.
  ```
  "GGG"      G = gold ingot   →  socket-sorcery:bangle
  "G G"
  "GGG"
  ```
  (Eight gold reads cleanly as a solid bangle and prices it as a genuine endgame item; the count is easy to
  tune down if that's too steep.)
- **Treasure (the thrill):** a **champion's bangle** in combat-flavored loot — *nether bastion*,
  *trial-chamber vaults*, *end city*. Same easy/fancy split as the necklace.

> **Feasibility.** Recipes: two JSON files in `data/socket-sorcery/recipe/`, copying the shape of the existing
> engravable-gem recipes. Empty loot drops: add the items to a `ModLoot`-style `LootTableEvents.MODIFY` call on
> the chosen chest tables (same mechanism already used for scrolls). Pre-socketed artifacts: needs a tiny code
> helper to attach the `socket-sorcery:sockets` + `socket-sorcery:engraving` data components via a custom loot
> function — moderate, optional polish.

---

## Custom gems — intentionally creative-only

No acquisition is added for Ruby, Sapphire, Peridot, Amethyst, and Topaz. They remain a creative-tab prestige
tier on purpose. Because their patterns are fully covered by the craftable engravable gems (finding #2), a
survival player loses no capability — only the cosmetic variety. If that ever changes, the engraving/pattern
tables in [Patterns.java](src/main/java/tinytaru/socketsorcery/pattern/Patterns.java)
are where their behavior is wired, and a recipe or loot hook could be added then.

---

## Feasibility map (if these are ever built)

| Item | Source(s) | Technical hook | Effort |
|---|---|---|---|
| Necklace | Craft + heirloom loot | recipe JSON; `ModLoot` chest injection; (optional) component loot function | Low / Med |
| Bangle | Craft (gold ring) + champion loot | recipe JSON; `ModLoot` chest injection | Low |

Both reuse patterns already present in the codebase; the only non-trivial extra is the optional pre-socketed
artifact (attaching data components in a loot function).

---

## Verification (how to confirm, if implemented)

- **Recipes:** in survival, open the crafting table and confirm necklace (chain + gold) and bangle (gold ring)
  produce a working, empty-socket item that can be worn and socketed.
- **Loot injections:** use `/loot give @s loot <table>` on each target chest table and confirm the accessory
  appears at the expected rate; spot-check by opening the structure's chests / vaults live.
- **End-to-end (the point of the work):** craft a necklace → engrave a gem → socket it at the bench → wear it
  → confirm the passive effect ticks; repeat with a bangle and confirm the active effect fires on the R key.
  This is the loop that is currently impossible in survival.

---

## Implementation status

Implemented 2026-06-21; `compileJava` and full `build` pass.

**Recipes (new):**
- [necklace.json](src/main/resources/data/socket-sorcery/recipe/necklace.json) — a chain clasp over a gold
  setting (1 chain + 3 gold ingots).
- [bangle.json](src/main/resources/data/socket-sorcery/recipe/bangle.json) — a ring of 8 gold ingots.

**Treasure loot** (extended [ModLoot.java](src/main/java/tinytaru/socketsorcery/registry/ModLoot.java)): each
accessory is injected into themed high-tier tables via the existing `LootTableEvents.MODIFY` mechanism at a 10%
per-chest chance (`ACCESSORY_CHANCE`, tunable):
- Necklace → End City treasure, Woodland Mansion, Ancient City.
- Bangle → Bastion treasure, Nether Fortress, End City treasure.

Every treasure-found accessory arrives **pre-socketed as an "artifact"** — crafting is how you get blank ones,
so a treasure find is always a ready-to-wear prize. The artifact tier is the custom
[SocketArtifactFunction](src/main/java/tinytaru/socketsorcery/loot/SocketArtifactFunction.java): a registered
`LootItemFunction` that fills the accessory with 1–3 randomly engraved gems, built exactly the way the engraving
table + socketing bench would. Artifacts only ever use the five *craftable* engravable gems — never the
creative-only custom gems, since socketed gems can be removed at the bench and that would be a back-door to them.

One piece remains intentionally deferred: *trial-chamber vaults* as a source (their loot tables aren't exposed as
`BuiltInLootTables` constants, so adding them safely needs the exact 1.21.1 vault table id confirmed first).

**Verified:** `compileJava` and `build` pass; a dedicated server boots to *Done* with the mod loaded, the
`socket-sorcery:socket_artifact` loot function registered, and all recipes plus loot tables loaded and validated
with **no errors**. **Not exercised here:** a live treasure roll — in this headless dev server every
`ServerLevel`-accessing command (`/loot`, `/setblock`, `/data`, even `/seed`) throws "unexpected error" while
`/say` and `/list` work (a MC 1.21.1-on-Java-25 dedicated-server quirk), so a roll can only be confirmed in a
normal client. A `LOGGER.info("Generated pre-socketed artifact …")` fires on each artifact generation to make
that confirmation obvious in the log.

**To confirm in-game:** craft each accessory; engrave a gem → socket it at the bench → wear it → check the
passive (necklace) and active/R-key (bangle) effects fire; and spot-check drops with
`/loot give @s loot minecraft:chests/end_city_treasure` (and `bastion_treasure`, `ancient_city`, etc.).
