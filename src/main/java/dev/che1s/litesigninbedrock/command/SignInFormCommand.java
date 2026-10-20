package dev.che1s.litesigninbedrock.command;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import dev.che1s.litesigninbedrock.FormManager;
import dev.che1s.litesigninbedrock.LiteSignInBedrock;

public class SignInFormCommand implements CommandExecutor, TabCompleter {
    private final LiteSignInBedrock plugin;
    private final FormManager formManager;
    public SignInFormCommand(LiteSignInBedrock plugin, FormManager formManager) {
        this.plugin = plugin; this.formManager = formManager;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission("litesigninbedrock.admin")) {
                sender.sendMessage(plugin.lang().message("command.no-permission"));
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(plugin.lang().message(plugin.reloadSettings() ? "command.reload-success" : "command.reload-failed"));
            } else sender.sendMessage(plugin.lang().message("command.usage", "label", label));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.lang().text("command.players-only"));
        } else if (!sender.hasPermission("litesigninbedrock.use")) {
            sender.sendMessage(plugin.lang().message("messages.no-permission"));
        } else formManager.openMainForm((Player) sender);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && sender.hasPermission("litesigninbedrock.admin") && "reload".startsWith(args[0].toLowerCase(java.util.Locale.ROOT))
            ? Collections.singletonList("reload") : Collections.emptyList();
    }
}
