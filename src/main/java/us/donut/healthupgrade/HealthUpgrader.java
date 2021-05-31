package us.donut.healthupgrade;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.stream.Collectors;

public class HealthUpgrader {

    private double health;
    private double cost;
    private ItemStack menuItem;
    private ItemStack consumableItem;

    public HealthUpgrader(double health, double cost) {
        this.health = health;
        this.cost = cost;
        FileConfiguration config = HealthUpgradePlugin.getInstance().getConfig();
        menuItem = createItem(config.getString("menu.item-name"), config.getStringList("menu.item-lore"));
        consumableItem = createItem(config.getString("consumable.item-name"), config.getStringList("consumable.item-lore"));
    }

    public double getHealth() {
        return health;
    }

    public double getCost() {
        return cost;
    }

    public ItemStack getMenuItem() {
        return menuItem.clone();
    }

    public ItemStack getConsumableItem() {
        return consumableItem.clone();
    }

    private ItemStack createItem(String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta itemMeta = (PotionMeta) item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setBasePotionData(new PotionData(PotionType.INSTANT_HEAL));
            itemMeta.setDisplayName(format(name));
            itemMeta.setLore(lore.stream().map(this::format).collect(Collectors.toList()));
            itemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            item.setItemMeta(itemMeta);
        }
        return item;
    }

    private String format(String string) {
        return ChatColor.translateAlternateColorCodes('&', string)
                .replace("{HEALTH}", String.valueOf(health / 2).replaceAll("\\.0\\z", ""))
                .replace("{COST}", HealthUpgradePlugin.getInstance().getEconomy().format(cost));
    }
}
