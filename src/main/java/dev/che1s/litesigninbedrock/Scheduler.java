package dev.che1s.litesigninbedrock;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.IllegalPluginAccessException;

/** All form callbacks access the player on its owning thread. */
public final class Scheduler {
    private Scheduler() {}

    public static void runTask(Player player, Runnable task) {
        LiteSignInBedrock plugin = LiteSignInBedrock.getInstance();
        if (plugin != null) runTaskLater(plugin, player, task, 1L);
    }

    public static void runTaskLater(Plugin plugin, Player player, Runnable task, long delayTicks) {
        if (!plugin.isEnabled()) return;
        Runnable guarded = () -> {
            if (plugin.isEnabled() && player.isOnline()) task.run();
        };
        long delay = Math.max(1L, delayTicks);
        try {
            Bukkit.getScheduler().runTaskLater(plugin, guarded, delay);
        } catch (IllegalPluginAccessException shutdown) {
            return;
        } catch (UnsupportedOperationException ex) {
            // Reflect on the public interface, as the implementation may be inaccessible.
            // Never run on the callback/global thread if entity scheduling fails.
            try {
                Object scheduler = Player.class.getMethod("getScheduler").invoke(player);
                Class<?> type = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
                type.getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class)
                    .invoke(scheduler, plugin, guarded, null, delay);
            } catch (ReflectiveOperationException failure) {
                plugin.getLogger().log(Level.SEVERE, ((LiteSignInBedrock) plugin).lang().text("logs.player-task-failed"), failure);
            }
        }
    }

    /** Run server-wide collection work on the main/global region thread. */
    public static void global(Plugin plugin, Runnable task) {
        if (!plugin.isEnabled()) return;
        try {
            Bukkit.getScheduler().runTask(plugin, () -> { if (plugin.isEnabled()) task.run(); });
        } catch (IllegalPluginAccessException shutdown) {
            return;
        } catch (UnsupportedOperationException folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Class<?> type = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
                type.getMethod("execute", Plugin.class, Runnable.class).invoke(scheduler, plugin,
                    (Runnable) () -> { if (plugin.isEnabled()) task.run(); });
            } catch (ReflectiveOperationException failure) {
                plugin.getLogger().log(Level.SEVERE, ((LiteSignInBedrock) plugin).lang().text("logs.global-task-failed"), failure);
            }
        }
    }
}
