# Socket Sorcery — Data-Driven Content Guide

Patterns, modifiers, and gem/scroll compatibility are **fully data-driven**: every built-in
definition is a JSON file, loaded through synced dynamic registries exactly like vanilla 1.21
enchantments. A datapack (or another mod's bundled data) can add new patterns and modifiers, make
any item engravable, and repurpose any item as a teaching scroll — no Java required for gameplay.

The mod's own definitions double as reference examples:

- `data/socket-sorcery/socket-sorcery/pattern/*.json` — the 11 built-in patterns
- `data/socket-sorcery/socket-sorcery/modifier/*.json` — the 7 built-in modifiers

## Where files go

Registry ids are namespaced `socket-sorcery:pattern` / `socket-sorcery:modifier`, so inside YOUR
datapack the directory carries the registry's namespace **and** path:

```
data/<your_namespace>/socket-sorcery/pattern/<name>.json
data/<your_namespace>/socket-sorcery/modifier/<name>.json
```

**Reload behaviour:** definitions load at world/server start and sync to clients automatically.
`/reload` does **not** re-read them — leave and re-enter the world after editing.

## Pattern schema

```json
{
  "mask": [
    "................",
    "......####......",
    "  ... exactly 16 rows of 16 chars, '.' or '#' ..."
  ],
  "color": "#FF5722",
  "cooldown": 40,
  "cast_feedback": {
    "sound": "minecraft:entity.blaze.shoot",
    "particle": { "type": "minecraft:flame" },
    "skip_on_miss": false
  },
  "gems": ["minecraft:emerald", "socket-sorcery:engravable_redstone"],
  "scroll": "minecraft:paper",
  "necklace_effects": [ ...components... ],
  "bangle_effects": [ ...components... ]
}
```

| field | required | meaning |
|---|---|---|
| `mask` | yes | The 16×16 symbol the player must chisel. At least one `#`. Keep the shape inside rows/cols 1–14 with lit cells at the bounding box's top/bottom-centre and mid-row edges, or some modifiers won't be carvable on it (they gracefully disappear). |
| `color` | yes | `"#RRGGBB"` or int. Tints the symbol, names, tooltip grid. |
| `cooldown` | yes | Base activation cooldown in ticks per socketed gem of this pattern. |
| `cast_feedback` | no | Played once per activation: `sound` at the caster, 12 `particle`s at the impact. The particle uses vanilla's object form (`{"type": "minecraft:flame"}` — complex particles like `dust` take their extra options too). `skip_on_miss` suppresses both when nothing was hit. |
| `gems` | no | Item ids that can receive this pattern — **any** item, yours or vanilla. Unknown ids are tolerated (logged at server start), so cross-mod compat entries are safe. |
| `scroll` | no | The item that teaches this pattern at the Engraving Table. Any item; one pattern per item (duplicates warn, first wins). |
| `necklace_effects` | no | Components run each second while socketed in a worn necklace (no target). |
| `bangle_effects` | no | Components run on bangle activation / ring retaliation, against the hit target. |

**Names:** the display name is the translation key `pattern.<namespace>.<path>` — ship a resource
pack (or mod lang file) defining it, or players see the raw key.

## Effect components

Every entry in `necklace_effects` / `bangle_effects` is `{ "type": "<id>", ...fields }`. Common
optional fields on most types: `target` (`self` | `hit_entity` | `hit_entity_or_self` | `area`),
`when` gate (`always` | `hit_entity` | `no_hit_entity` | `hit_block` | `hit_any`), `filter` for
areas (`monsters` default | `living`), `radius` for areas (Range modifier adds to it).

| type | fields (defaults) | does |
|---|---|---|
| `socket-sorcery:mob_effect` | `effect`, `duration`, `amplifier` (0), `target` (self), `radius` (5.0), `filter`, `ambient` (false), `show_particles` (true), `when` | Applies a status effect. Duration/Power modifiers scale duration/amplifier. |
| `socket-sorcery:damage` | `amount`, `target` (hit_entity), `radius` (3.0), `filter`, `knockback` (0.0), `when` | Magic damage; optional knockback away from the wearer. Power scales amount. |
| `socket-sorcery:heal` | `amount`, `target` (hit_entity_or_self), `when` | Heals. Power scales amount. |
| `socket-sorcery:freeze` | `target` (hit_entity), `radius`, exactly one of `add_ticks` / `set_ticks`, `max_ticks` (140) | Powder-snow freezing. `set_ticks` scales with Duration. |
| `socket-sorcery:ignite` | `seconds`, `place_fire_on_block` (false) | Sets the hit entity on fire (Duration & Power scale it), or kindles fire on a struck block face. |
| `socket-sorcery:launch` | `direction` (`look`\|`up`), `magnitude`, `y_boost` (0), `aim_scale` (0), `when` | Propels the wearer. Power scales magnitude; `aim_scale` lets Aim modifiers bias a `look` dash. |
| `socket-sorcery:teleport` | `aim_scale` (0) | Teleports the wearer to the hit location (or bangle-reach ahead on a miss). |
| `socket-sorcery:summon_lightning` | — | Lightning bolt at the hit location; silent on a miss. |

Java mods can add new component types: implement `PatternEffectComponent` and register its
`MapCodec` into `ModRegistries.EFFECT_TYPE` during mod init.

## Modifier schema

```json
{
  "color": "#FF5252",
  "cell_rule": { "type": "socket-sorcery:center_block" },
  "power_bonus": 1,
  "duration_multiplier": 1,
  "range_bonus": 0.0,
  "aim": [0.0, 0.0, 0.0]
}
```

The `cell_rule` derives which cells must be cut to depth 2, from the pattern's own symbol — so one
gesture works on every pattern. Built-in rules: `center_block`, `top_bottom`, `left_right`,
`extension` (`"direction": "up"|"down"|"left"|"right"`). Java mods can register more rules into
`ModRegistries.CELL_RULE_TYPE`.

Knobs fold together across an engraving's modifiers: `power_bonus` sums (raises effect amplifiers,
and any power > 0 also multiplies magnitudes ×1.5), `duration_multiplier` multiplies,
`range_bonus` sums onto area radii, `aim` vectors sum (x=right, y=up, z=forward of the caster).
Each applied modifier also adds +40% to the gem's cooldown.

**Cell sets must stay pairwise disjoint on every pattern** — overlapping rules make those modifier
combinations uncarvable (the server logs a warning per conflict at startup).

## Worked example — a datapack pattern on a vanilla item

`data/mypack/socket-sorcery/pattern/venom.json`:

```json
{
  "mask": [
    "................", "................", "................", "....##....##....",
    ".....##..##.....", "......####......", ".......##.......", ".......##.......",
    ".......##.......", ".......##.......", "......####......", ".....##..##.....",
    "....##....##....", "................", "................", "................"
  ],
  "color": "#7CB342",
  "cooldown": 60,
  "cast_feedback": { "particle": { "type": "minecraft:item_slime" } },
  "gems": ["minecraft:emerald"],
  "scroll": "minecraft:slime_ball",
  "necklace_effects": [
    { "type": "socket-sorcery:mob_effect", "effect": "minecraft:strength", "duration": 60,
      "target": "self", "ambient": true, "show_particles": false }
  ],
  "bangle_effects": [
    { "type": "socket-sorcery:mob_effect", "effect": "minecraft:poison", "duration": 100,
      "amplifier": 1, "target": "hit_entity", "when": "hit_entity" }
  ]
}
```

With just this file: slime balls slot into the table's scroll slot, emeralds into its gem slot, the
venom symbol is chiselled (with all seven modifiers available), and the engraved emerald sockets
into any accessory. Everything appears in EMI automatically.

## Limitations

- **Engraved-icon rendering** for items that aren't Socket Sorcery gems can't be data-driven: the
  engraved emerald above works fully but keeps its plain emerald icon. A Java mod can opt items in
  via `SocketSorceryClientApi.registerEngravableGem(item)` plus a model override on the
  `socket-sorcery:engraving` predicate (copy any `*_engraved.json` model).
- **The guidebook** documents built-in patterns only; datapack patterns won't get book pages.
- **`/reload` doesn't apply** — re-enter the world.
