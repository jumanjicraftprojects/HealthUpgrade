package us.donut.healthupgrade;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class UpgradeManager implements Listener {

    private HealthUpgradePlugin plugin = HealthUpgradePlugin.getInstance();
    private PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
    private Map<ItemStack, HealthUpgrader> upgraders = new HashMap<>();
    private Map<String, Double> healthLimitPerms = new HashMap<>();
    private Inventory purchaseInv;
    private ItemStack confirmItem;
    private ItemStack cancelItem;
    private double defaultHealth;
    private double defaultHealthLimit;
    private boolean healOnConsume;

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        defaultHealth = config.getDouble("default-health", 20);
        defaultHealthLimit = config.getDouble("default-health-limit", 20);
        healOnConsume = config.getBoolean("consumable.heal-on-consume");
        Bukkit.getOnlinePlayers().forEach(this::updateHealth);
        confirmItem = createItem(Material.GREEN_WOOL, "&aConfirm");
        cancelItem = createItem(Material.RED_WOOL, "&cCancel");
        purchaseInv = Bukkit.createInventory(InvIdentifier.PURCHASE, 27, color(config.getString("menu.title")));
        addBorder(purchaseInv);
        ConfigurationSection slotsSection = config.getConfigurationSection("menu.slots");
        if (slotsSection != null) {
            for (String slotString : slotsSection.getKeys(false)) {
                ConfigurationSection slotSection = slotsSection.getConfigurationSection(slotString);
                if (slotSection != null) {
                    HealthUpgrader healthUpgrader = new HealthUpgrader(slotSection.getDouble("health"), slotSection.getDouble("cost"));
                    upgraders.put(healthUpgrader.getMenuItem(), healthUpgrader);
                    upgraders.put(healthUpgrader.getConsumableItem(), healthUpgrader);
                    purchaseInv.setItem(Integer.parseInt(slotString), healthUpgrader.getMenuItem());
                }
            }
        }
        ConfigurationSection healthLimitSection = config.getConfigurationSection("health-limit-perms");
        if (healthLimitSection != null) {
            for (String key : healthLimitSection.getKeys(false)) {
                healthLimitPerms.put("healthupgrade.limit." + key, healthLimitSection.getDouble(key));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getClickedInventory();
        if (inv != null && e.getWhoClicked() instanceof Player) {
            Player player = (Player) e.getWhoClicked();
            ItemStack item = e.getCurrentItem();
            if (inv.getHolder() == InvIdentifier.PURCHASE) {
                e.setCancelled(true);
                HealthUpgrader healthUpgrader = upgraders.get(item);
                if (healthUpgrader != null)  {
                    if (plugin.getEconomy().has(player, healthUpgrader.getCost())) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Inventory confirmInv = Bukkit.createInventory(InvIdentifier.CONFIRM, 36, e.getView().getTitle());
                            addBorder(confirmInv);
                            confirmInv.setItem(13, item.clone());
                            confirmInv.setItem(20, confirmItem.clone());
                            confirmInv.setItem(24, cancelItem.clone());
                            player.openInventory(confirmInv);
                        });
                    } else {
                        player.sendMessage(color("&cInsufficient funds."));
                        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1, 1);
                    }
                }
            } else if (inv.getHolder() == InvIdentifier.CONFIRM) {
                if (confirmItem.equals(item)) {
                    HealthUpgrader healthUpgrader = upgraders.get(inv.getItem(13));
                    if (healthUpgrader != null) {
                        if (plugin.getEconomy().has(player, healthUpgrader.getCost())) {
                            plugin.getEconomy().withdrawPlayer(player, healthUpgrader.getCost());
                            player.getInventory().addItem(healthUpgrader.getConsumableItem());
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                            Bukkit.getScheduler().runTask(plugin, player::closeInventory);
                        } else {
                            player.sendMessage(color("&cInsufficient funds."));
                            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1, 1);
                        }
                    }
                } else if (cancelItem.equals(item)) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(purchaseInv));
                }
                e.setCancelled(true);
            }
        }
        if (e.getInventory().getHolder() instanceof InvIdentifier) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (!e.isCancelled()) {
            ItemStack item = e.getItem().clone();
            item.setAmount(1);
            HealthUpgrader upgrader = upgraders.get(item);
            if (upgrader != null) {
                Player player = e.getPlayer();
                e.setCancelled(true);
                if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() < getHealthLimit(player) || player.hasPermission("healthupgrade.limit.bypass")) {
                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    playerDataManager.setUpgrade(player, upgrader.getHealth() + playerDataManager.getUpgrade(player));
                    updateHealth(player);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    if (healOnConsume) {
                        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    }
                } else {
                    player.sendMessage(color("&cYou have reached the health limit."));
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        updateHealth(e.getPlayer());
        e.getPlayer().setHealth(e.getPlayer().getHealth());
    }

    public void open(Player player) {
        player.openInventory(purchaseInv);
    }

    public void updateHealth(Player player) {
        double health = defaultHealth + playerDataManager.getUpgrade(player);
        double limit = getHealthLimit(player);
        if (health > limit && !player.hasPermission("healthupgrade.limit.bypass")) {
            health = limit;
        }
        if (health <= 0) {
            health = 1;
        }
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
    }

    public double getDefaultHealth() {
        return defaultHealth;
    }

    private double getHealthLimit(Player player) {
        double healthLimit = defaultHealthLimit;
        for (Map.Entry<String, Double> entry : healthLimitPerms.entrySet()) {
            if (player.hasPermission(entry.getKey()) && entry.getValue() > healthLimit) {
                healthLimit = entry.getValue();
            }
        }
        return healthLimit;
    }

    private void addBorder(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i > inv.getSize() - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, createItem(i % 2 == 0 ? Material.GREEN_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE, " "));
            }
        }
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(color(name));
            item.setItemMeta(itemMeta);
        }
        return item;
    }

    private String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    private enum InvIdentifier implements InventoryHolder {
        PURCHASE, CONFIRM;
        @Override
        public Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }
}
