package dev.che1s.litesigninbedrock.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import dev.che1s.litesigninbedrock.BedrockUtil;
import dev.che1s.litesigninbedrock.FormManager;
import dev.che1s.litesigninbedrock.LiteSignInBedrock;
import dev.che1s.litesigninbedrock.Scheduler;

public class PlayerJoinListener
    implements Listener
{
    private final LiteSignInBedrock plugin;
    private final FormManager formManager;

    public PlayerJoinListener(LiteSignInBedrock plugin, FormManager formManager) {
        this.plugin = plugin;
        this.formManager = formManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("auto-open-on-join", false)) return;
        Player player = event.getPlayer();
        long delayTicks = Math.max(0L, Math.min(86400L, plugin.getConfig().getLong("auto-open-delay", 1L))) * 20L;
        Scheduler.runTaskLater(plugin, player, () -> {
            if (player.isOnline() && player.hasPermission("litesigninbedrock.use") && BedrockUtil.isBedrock(player)) {
                formManager.openMainForm(player);
            }
        }, delayTicks);
    }
}
