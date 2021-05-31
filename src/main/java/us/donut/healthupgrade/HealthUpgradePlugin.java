package us.donut.healthupgrade;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HealthUpgradePlugin extends JavaPlugin {

    private static HealthUpgradePlugin instance;
    private Economy economy;
    private PlayerDataManager playerDataManager;
    private UpgradeManager upgradeManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        getServer().getPluginManager().registerEvents(playerDataManager = new PlayerDataManager(), this);
        getServer().getPluginManager().registerEvents(upgradeManager = new UpgradeManager(), this);
        Bukkit.getScheduler().runTask(this, upgradeManager::reload);
    }

    @Override
    public void onDisable() {
        playerDataManager.saveData(false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("reloadhealthupgrade")) {
            reloadConfig();
            upgradeManager.reload();
            playerDataManager.saveData(true);
            sender.sendMessage(ChatColor.GREEN + "Successfully reloaded HealthUpgrade.");
        } else if (command.getName().equals("health")) {
            if (args.length > 0) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
                double health = (upgradeManager.getDefaultHealth() + playerDataManager.getUpgrade(player)) / 2;
                sender.sendMessage(ChatColor.GREEN + "Max health of " + player.getName() + " is " + String.valueOf(health).replaceAll("\\.0\\z", "") + ".");
                return true;
            }
            return false;
        } else if (command.getName().equals("sethealth")) {
            if (args.length > 1) {
                try {
                    double health = Integer.parseInt(args[1]);
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
                    playerDataManager.setUpgrade(player, health - upgradeManager.getDefaultHealth());
                    Player onlinePlayer = player.getPlayer();
                    if (onlinePlayer != null) {
                        upgradeManager.updateHealth(onlinePlayer);
                    }
                    sender.sendMessage(ChatColor.GREEN + "Set max health of " + player.getName() + " to " + args[1] + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number.");
                }
                return true;
            }
            return false;
        } else if (command.getName().equals("resethealth")) {
            if (args.length > 0) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
                playerDataManager.getHealthUpgrades().remove(player.getUniqueId());
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    upgradeManager.updateHealth(onlinePlayer);
                }
                sender.sendMessage(ChatColor.GREEN + "Reset max health of " + player.getName() + ".");
                return true;
            }
            return false;
        } else if (command.getName().equals("resetallhealth")) {
            playerDataManager.getHealthUpgrades().clear();
            Bukkit.getOnlinePlayers().forEach(upgradeManager::updateHealth);
            sender.sendMessage(ChatColor.GREEN + "Reset max health of all players.");
        } else if (sender instanceof Player) {
            upgradeManager.open((Player) sender);
        }
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public static HealthUpgradePlugin getInstance() {
        return instance;
    }
}
