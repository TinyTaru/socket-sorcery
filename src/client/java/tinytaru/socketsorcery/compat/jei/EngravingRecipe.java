package tinytaru.socketsorcery.compat.jei;

import net.minecraft.world.item.ItemStack;

/**
 * One (gem, pattern) engraving, as JEI displays it: the gem plus the scroll that teaches the
 * pattern, with a chisel as catalyst, producing the gem engraved with that pattern (no modifiers —
 * the base engraving). Synthetic: the real process is the chiselling minigame at the table.
 *
 * @param gem      the blank gem
 * @param scroll   the scroll teaching the pattern
 * @param engraved the resulting gem, carrying a real engraving component so the composited icon
 *                 renders exactly as it does in-game
 */
public record EngravingRecipe(ItemStack gem, ItemStack scroll, ItemStack engraved) {
}
