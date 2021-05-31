package us.donut.healthupgrade;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager implements Listener {

    private HealthUpgradePlugin plugin = HealthUpgradePlugin.getInstance();
    private Map<UUID, Double> healthUpgrades;
    private File playerDataFile;

    public PlayerDataManager() {
        playerDataFile = new File(plugin.getDataFolder(), "player-data.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        healthUpgrades = new HashMap<>();
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        for (String key : dataConfig.getKeys(false)) {
            healthUpgrades.put(UUID.fromString(key), dataConfig.getDouble(key) * 2);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        saveData(true);
    }

    public void setUpgrade(OfflinePlayer player, double value) {
        healthUpgrades.put(player.getUniqueId(), value);
    }

    public double getUpgrade(OfflinePlayer player) {
        return healthUpgrades.getOrDefault(player.getUniqueId(), 0.0);
    }

    public Map<UUID, Double> getHealthUpgrades() {
        return healthUpgrades;
    }

    public void saveData(boolean async) {
        if (async) {
            Map<UUID, Double> data = new HashMap<>(healthUpgrades);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveData(data));
        } else {
            saveData(healthUpgrades);
        }
    }

    private synchronized void saveData(Map<UUID, Double> data) {
        YamlConfiguration dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : data.entrySet()) {
            dataConfig.set(entry.getKey().toString(), entry.getValue() / 2);
        }
        try {
            dataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
