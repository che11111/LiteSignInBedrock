package dev.che1s.litesigninbedrock.update;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import dev.che1s.litesigninbedrock.LiteSignInBedrock;
import dev.che1s.litesigninbedrock.Scheduler;

/** One daemon worker, cached join notices, no requests triggered by joining. */
public final class UpdateChecker implements Listener {
    private final LiteSignInBedrock plugin;
    private final GitHubReleaseClient client = new GitHubReleaseClient();
    private final String installed;
    private volatile Run active;

    public UpdateChecker(LiteSignInBedrock plugin) {
        this.plugin = plugin;
        installed = plugin.getDescription().getVersion();
    }

    public synchronized void reload() {
        if (!plugin.getConfig().getBoolean("check-for-updates", true)) { shutdown(); return; }
        if (active != null) return;
        Run run = new Run();
        active = run;
        run.worker.scheduleWithFixedDelay(() -> check(run), 5, 6 * 60 * 60, TimeUnit.SECONDS);
    }

    private void check(Run run) {
        if (active != run) return;
        try {
            GitHubReleaseClient.Release release = client.fetchLatest()
                .filter(value -> ReleaseVersion.isNewer(value.version, installed)).orElse(null);
            if (active != run) return;
            Scheduler.global(plugin, () -> {
                if (active != run) return;
                run.failed = false;
                GitHubReleaseClient.Release previous = run.available;
                run.available = release;
                if (release == null || previous != null && previous.version.equals(release.version)) return;
                plugin.getLogger().info(plugin.lang().text("logs.update-available", "latest", release.version, "current", installed, "url", release.url));
                for (Player player : Bukkit.getOnlinePlayers()) Scheduler.runTask(player, () -> notifyPlayer(player, run));
            });
        } catch (IOException | RuntimeException error) {
            if (active == run && !run.failed) {
                run.failed = true;
                plugin.getLogger().warning(plugin.lang().text("logs.update-failed", "error", error.toString()));
            }
        }
    }

    private void notifyPlayer(Player player, Run run) {
        GitHubReleaseClient.Release release = run.available;
        if (active != run || release == null || !player.hasPermission("litesigninbedrock.admin")) return;
        if (release.version.equals(run.notified.put(player.getUniqueId(), release.version))) return;
        player.sendMessage(plugin.lang().message("messages.update-available", "latest", release.version, "current", installed, "url", release.url));
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Run run = active;
        if (run != null && run.available != null) Scheduler.runTaskLater(plugin, event.getPlayer(), () -> notifyPlayer(event.getPlayer(), run), 60);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        Run run = active;
        if (run != null) run.notified.remove(event.getPlayer().getUniqueId());
    }
    public synchronized void shutdown() {
        Run run = active;
        active = null;
        if (run != null) run.worker.shutdownNow();
    }
    private static final class Run {
        final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "LiteSignInBedrock-update"); thread.setDaemon(true); return thread;
        });
        final Map<UUID, String> notified = new ConcurrentHashMap<>();
        volatile GitHubReleaseClient.Release available;
        volatile boolean failed;
    }
}
