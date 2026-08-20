# Engraving-table attachment guide

Open `engraving_table_attachments.json` in Blockbench. Keep it in the repository's `tools` folder;
the required local textures are supplied in `tools/textures`. It includes the real engraving-table
geometry and the real gem/chisel textures as editable guides. The two editable elements are:

- `GEM_ATTACHMENT` — the gem placeholder's position and rotation.
- `CHISEL_ATTACHMENT` — the chisel placeholder's position and rotation.

The project uses Minecraft block coordinates in a 16-unit block: `x` increases east, `y` increases
up, and `z` increases south. Move or rotate the elements, leave their names unchanged, save the
`.json`, and send that file back. The colored placeholder boxes are only guides; the renderer will keep
using the real live gem and chisel item models.

The table texture is 64×64, while the supplied gem and chisel textures are native 16×16 item sprites.
Their attachment faces use 0–16 UVs so Blockbench displays the same full sprites as the in-game item
renderer. The attachment boxes also include the renderer's scaled 1/16-unit item extrusion and are centered
at the same y position as the live item. Keep those UV ranges and the element origins when moving the
elements.
