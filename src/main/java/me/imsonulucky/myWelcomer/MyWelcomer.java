package me.imsonulucky.myWelcomer;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import java.util.*;

public class MyWelcomer extends JavaPlugin {

    private final Map<UUID, Integer> welcomeCounts = new HashMap<>();
    private Player lastJoinedPlayer;
    private FileConfiguration config;
    private FileConfiguration welcomedConfig;
    private File welcomedFile;

    @Override
    public void onEnable() {
        createConfig();
        createWelcomedFile();
        loadWelcomedPlayers();

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getCommand("welcome").setExecutor(new WelcomeCommand());
        getCommand("reload").setExecutor(new ReloadCommand());
    }

    private void createConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        reloadConfig();
        config = getConfig();

        if (config.contains("players")) {
            config.set("players", null);
            saveConfig();
        }
    }

    private void createWelcomedFile() {
        welcomedFile = new File(getDataFolder(), "welcomed.yml");
        if (!welcomedFile.exists()) {
            try {
                welcomedFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        welcomedConfig = YamlConfiguration.loadConfiguration(welcomedFile);
    }

    private void loadWelcomedPlayers() {
        if (welcomedConfig.contains("players")) {
            Set<String> welcomedUUIDs = welcomedConfig.getConfigurationSection("players").getKeys(false);
            for (String uuidStr : welcomedUUIDs) {
                UUID uuid = UUID.fromString(uuidStr);
                int welcomeCount = welcomedConfig.getInt("players." + uuidStr + ".welcomeCount", 0);
                welcomeCounts.put(uuid, welcomeCount);
            }
        }
    }

    public void saveWelcomedPlayers() {
        for (UUID uuid : welcomeCounts.keySet()) {
            welcomedConfig.set("players." + uuid + ".welcomeCount", welcomeCounts.get(uuid));
        }
        try {
            welcomedConfig.save(welcomedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Player getLastJoinedPlayer() {
        return lastJoinedPlayer;
    }

    public void setLastJoinedPlayer(Player player) {
        this.lastJoinedPlayer = player;
    }

    public Map<UUID, Integer> getWelcomeCounts() {
        return welcomeCounts;
    }

    public FileConfiguration getWelcomedConfig() {
        return welcomedConfig;
    }

    public void giveTieredReward(Player player, int welcomeCount) {
        if (config.contains("tiered-rewards." + welcomeCount)) {
            String message = config.getString("tiered-rewards." + welcomeCount + ".message");
            String command = config.getString("tiered-rewards." + welcomeCount + ".command");

            if (message != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
            if (command != null) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }
        }
    }

    public class WelcomeCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                return false;
            }

            Player player = (Player) sender;

            if (lastJoinedPlayer == null) {
                player.sendMessage(ChatColor.RED + "No new player has joined yet!");
                return true;
            }

            if (welcomeCounts.containsKey(player.getUniqueId()) && welcomeCounts.get(player.getUniqueId()) >= 1) {
                player.sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "MyWelcomer" + ChatColor.GRAY + "] "
                        + ChatColor.RED + "You Have Already Welcomed " + lastJoinedPlayer.getName() + "!");
                return true;
            }

            int currentCount = welcomeCounts.getOrDefault(player.getUniqueId(), 0) + 1;
            welcomeCounts.put(player.getUniqueId(), currentCount);

            giveTieredReward(player, currentCount);

            String soundName = config.getString("sound.welcome", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) config.getDouble("sound.volume", 1.0);
            float pitch = (float) config.getDouble("sound.pitch", 1.0);

            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid sound specified in the config. Using default sound.");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, volume, pitch);
            }

            String message = config.getString("welcome-send", "%player% has welcomed a new player #%unique_joins%");
            message = message.replace("%player%", player.getName())
                    .replace("%unique_joins%", String.valueOf(welcomeCounts.get(player.getUniqueId())));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

            saveWelcomedPlayers();

            return true;
        }
    }

    public class ReloadCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("mywelcomer.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            reloadConfig();
            createWelcomedFile();
            loadWelcomedPlayers();
            sender.sendMessage(ChatColor.GREEN + "MyWelcomer reloaded successfully!");
            return true;
        }
    }
}