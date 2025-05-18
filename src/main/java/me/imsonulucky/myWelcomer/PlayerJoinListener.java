package me.imsonulucky.myWelcomer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final MyWelcomer plugin;

    public PlayerJoinListener(MyWelcomer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.setLastJoinedPlayer(player);

        if (!plugin.getWelcomedConfig().contains("players." + player.getUniqueId() + ".firstJoinTime")) {
            plugin.getWelcomedConfig().set("players." + player.getUniqueId() + ".firstJoinTime", System.currentTimeMillis());
            plugin.saveWelcomedPlayers();
        }
    }
}
