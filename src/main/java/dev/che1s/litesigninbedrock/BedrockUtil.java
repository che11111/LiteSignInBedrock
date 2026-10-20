package dev.che1s.litesigninbedrock;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Wrapper around the Floodgate API so the rest of the plugin never has to deal
 * with raw Floodgate calls or potential NoClassDefFoundError.
 */
public final class BedrockUtil
{
    private BedrockUtil() {}

    public static boolean isBedrock(Player player) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean sendForm(Player player, FormBuilder<?, ?, ?> formBuilder) {
        try {
            return FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder);
        } catch (Throwable t) {
            return false;
        }
    }
}
