package dev.che1s.litesigninbedrock;

import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.entity.Player;

/**
 * Isolated PlaceholderAPI access. This class is only loaded when PlaceholderAPI
 * is actually present, avoiding NoClassDefFoundError at runtime.
 */
public final class PapiHook
{
    private PapiHook() {}

    public static String setPlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
