package dev.che1s.litesigninbedrock;

import java.time.DateTimeException;
import java.time.LocalDate;

import org.bukkit.entity.Player;

import studio.trc.bukkit.litesignin.api.Storage;
import studio.trc.bukkit.litesignin.util.LiteSignInUtils;
import studio.trc.bukkit.litesignin.util.OnlineTimeRecord;
import studio.trc.bukkit.litesignin.util.PluginControl;
import studio.trc.bukkit.litesignin.util.SignInDate;

/**
 * Performs the actual sign-in / retroactive sign-in through the LiteSignIn
 * public API, replicating the checks done by LiteSignIn's "/signin click"
 * command.
 */
public final class SignInService
{
    private SignInService() {}

    public enum Result {
        SUCCESS,
        CANCELLED,
        ALREADY_SIGNED_IN,
        DISABLED_WORLD,
        INSUFFICIENT_ONLINE_TIME,
        NO_PERMISSION,
        RETROACTIVE_DISABLED,
        RETROACTIVE_COOLDOWN,
        RETROACTIVE_MINIMUM_DATE,
        RETROACTIVE_NEED_MORE_CARDS,
        RETROACTIVE_INVALID_DATE
    }

    public static Result signIn(Player player) {
        if (LiteSignInUtils.checkInDisabledWorlds(player.getUniqueId())) {
            return Result.DISABLED_WORLD;
        }
        if (!LiteSignInUtils.hasPermission(player, "Commands.Click")) {
            return Result.NO_PERMISSION;
        }
        Storage data = Storage.getPlayer(player);
        if (data.alreadySignIn()) {
            return Result.ALREADY_SIGNED_IN;
        }
        long requirement = OnlineTimeRecord.getSignInRequirement(player);
        if (requirement != -1) {
            return Result.INSUFFICIENT_ONLINE_TIME;
        }
        return recordSignIn(data);
    }

    public static Result retroactiveSignIn(Player player, SignInDate date) {
        LocalDate selected;
        try {
            if (date == null) return Result.RETROACTIVE_INVALID_DATE;
            selected = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
        } catch (DateTimeException invalid) {
            return Result.RETROACTIVE_INVALID_DATE;
        }
        LocalDate today = LocalDate.now();
        // Today must use normal sign-in checks and must not consume a card.
        if (selected.equals(today)) return signIn(player);
        if (selected.isAfter(today)) return Result.RETROACTIVE_INVALID_DATE;
        if (LiteSignInUtils.checkInDisabledWorlds(player.getUniqueId())) {
            return Result.DISABLED_WORLD;
        }
        if (!LiteSignInUtils.hasPermission(player, "Commands.Click")
            || !LiteSignInUtils.hasPermission(player, "Retroactive-Card.Use")) {
            return Result.NO_PERMISSION;
        }
        if (!PluginControl.enableRetroactiveCard()) {
            return Result.RETROACTIVE_DISABLED;
        }
        Storage data = Storage.getPlayer(player);
        if (data.isRetroactiveCardCooldown()) {
            return Result.RETROACTIVE_COOLDOWN;
        }
        SignInDate minimum = PluginControl.getRetroactiveCardMinimumDate();
        if (minimum != null && selected.isBefore(LocalDate.of(minimum.getYear(), minimum.getMonth(), minimum.getDay()))) {
            return Result.RETROACTIVE_MINIMUM_DATE;
        }
        if (data.alreadySignIn(date)) {
            return Result.ALREADY_SIGNED_IN;
        }
        int required = PluginControl.getRetroactiveCardQuantityRequired();
        if (data.getRetroactiveCard() >= required) {
            return recordRetroactiveSignIn(data, date, required);
        } else {
            return Result.RETROACTIVE_NEED_MORE_CARDS;
        }
    }

    static Result recordSignIn(Storage data) {
        data.signIn();
        return data.alreadySignIn() ? Result.SUCCESS : Result.CANCELLED;
    }

    static Result recordRetroactiveSignIn(Storage data, SignInDate date, int required) {
        data.takeRetroactiveCard(required);
        data.signIn(date);
        // LiteSignIn's void API can return without recording the date when
        // another plugin cancels its event. Restore the cards in that case.
        if (!data.alreadySignIn(date)) {
            data.giveRetroactiveCard(required);
            return Result.CANCELLED;
        }
        return Result.SUCCESS;
    }
}
