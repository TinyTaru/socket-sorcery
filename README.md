# Socket Sorcery

An RPG engraving-and-socketing mod for Fabric. Find ancient **pattern scrolls**, chisel their
symbols onto **gems** at the **Engraving Table** — with deeper cuts encoding **modifiers** — then
set your engraved gems into worn accessories at the **Socketing Bench**.

## The loop

1. **Find a scroll.** Pattern scrolls hide in structure loot (dungeons, mineshafts, temples,
   strongholds, mansions, buried treasure).
2. **Craft a gem.** Engravable gems are crafted from vanilla materials (diamond, redstone, lapis,
   emerald, quartz, amethyst, prismarine, glowstone, copper, ender pearl) in a plus shape.
3. **Engrave it.** At the Engraving Table, chisel the scroll's 16×16 symbol onto the gem.
   Cutting certain cells *deeper* adds modifiers — Power, Duration, Range, and four Aim
   directions — discovered by experimentation.
4. **Socket it.** At the Socketing Bench, set engraved gems into an accessory.
5. **Wear it.** Accessories equip via Trinkets slots.

## Accessories — three ways to cast

| Accessory | Sockets | Behaviour |
|---|---|---|
| **Necklace** | 5 | Gems act **passively** while worn |
| **Bangle** | 3 | Gems fire **on command** (default key: `R`), with a cooldown |
| **Ring** | 1 | Gem fires **automatically when you take a hit**, on its own cooldown |

All three are craftable, and pre-socketed **artifacts** appear rarely in high-tier treasure
(end cities, bastions, ancient cities, woodland mansions...).

## Patterns

Eleven patterns, each with a distinct passive and active effect: Fire, Frost, Healing, Lightning,
Leaping, Wind, Earth, Lifesteal, Blink, Haste, and Spikes. Which patterns a gem can hold depends
on the gem — see the in-game guidebook or EMI.

## Data-driven — add your own

Every pattern, modifier, and gem/scroll pairing is JSON, loaded through synced dynamic registries
(like vanilla enchantments). Datapacks can add new patterns, make vanilla items engravable, and
register new scrolls — no code. See **[DATA_DRIVEN.md](DATA_DRIVEN.md)** for the full schema
reference and a worked example.

## Requirements

- Minecraft **1.21.1**, Fabric Loader ≥ 0.19.3, Java ≥ 21
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Trinkets](https://modrinth.com/mod/trinkets) ≥ 3.10.0
- [Patchouli](https://modrinth.com/mod/patchouli) (in-game guidebook)
- [Cloth Config](https://modrinth.com/mod/cloth-config) (configuration)

Optional but recommended: [EMI](https://modrinth.com/mod/emi) (recipe viewer integration) and
[Mod Menu](https://modrinth.com/mod/modmenu) (config screen access).

## Building

```
./gradlew build
```

The jar lands in `build/libs/`.

## License

CC0-1.0 — free to learn from and build upon.
