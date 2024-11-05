package ru.azenizzka.xplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.azenizzka.xplugin.XPlugin;

import java.util.*;

public class BlockUtils {
	private static final Set<Block> processingBlocks = new HashSet<>();
	private static final String NUMBER_OF_BROKEN_BLOCKS_KEY = "BROKEN_BLOCKS_COUNT";

	public static void breakBlocks(Player player, List<Block> blocks) {
		ItemStack tool = player.getInventory().getItemInMainHand();
		ItemMeta meta = tool.getItemMeta();

		synchronized (processingBlocks) {
			blocks.removeIf(block -> !processingBlocks.add(block));
		}

		try {
			((Damageable) meta).setDamage(computeDamage(tool, blocks.size()));
		} catch (Exception e) {
			processingBlocks.removeIf(blocks::contains);
			return;
		}

		tool.setItemMeta(meta);

		for (int i = 0; i < blocks.size(); i++) {
			Block block = blocks.get(i);

			if (!block.isPreferredTool(tool) || block.isEmpty())
				continue;

			Bukkit.getScheduler().runTaskLater(XPlugin.instance, () -> {
				if (block.isEmpty())
					return;

				increaseBrokenBlocks(tool, 1L);
				block.breakNaturally(tool, true, true);

				synchronized (processingBlocks) {
					processingBlocks.remove(block);
				}
			}, 3L * i);
		}
	}

	// throws an exception if tool will be breaked.
	private static int computeDamage(ItemStack tool, int countOfBlocks) throws Exception {
		ItemMeta meta = tool.getItemMeta();

		int unbreakingCoeff = tool.getEnchantmentLevel(Enchantment.DURABILITY) + 1;
		int maxDamage = ((Damageable) meta).getDamage() + countOfBlocks / unbreakingCoeff;

		if (tool.getType().getMaxDurability() - maxDamage < 0)
			throw new Exception();

		maxDamage = ((Damageable) meta).getDamage() + countOfBlocks / unbreakingCoeff;

		return maxDamage;
	}

	public static void increaseBrokenBlocks(ItemStack tool, Long value) {
		ItemMeta meta = tool.getItemMeta();

		if (meta == null)
			return;

		NamespacedKey key = new NamespacedKey(XPlugin.instance, NUMBER_OF_BROKEN_BLOCKS_KEY);

		if (!meta.getPersistentDataContainer().has(key))
			meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, 0L);

		Long numberOfBrokenBlocks = meta.getPersistentDataContainer().get(key, PersistentDataType.LONG);
		numberOfBrokenBlocks += value;

		List<Component> lore = new ArrayList<>();

		lore.add(Component.text("Сломано блоков: ")
				.color(NamedTextColor.GRAY)
				.append(Component.text(numberOfBrokenBlocks).color(NamedTextColor.DARK_GREEN)));

		meta.lore(lore);

		meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, numberOfBrokenBlocks);

		tool.setItemMeta(meta);
	}
}
