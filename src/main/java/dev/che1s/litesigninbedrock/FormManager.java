package dev.che1s.litesigninbedrock;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;

import studio.trc.bukkit.litesignin.api.Storage;
import studio.trc.bukkit.litesignin.queue.SignInQueue;
import studio.trc.bukkit.litesignin.util.OnlineTimeRecord;
import studio.trc.bukkit.litesignin.util.PluginControl;
import studio.trc.bukkit.litesignin.util.SignInDate;

/**
 * Builds and sends the Bedrock forms, and wires form buttons to the sign-in
 * operations.
 */
public class FormManager
{

    private final LiteSignInBedrock plugin;

    public FormManager(LiteSignInBedrock plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    private String cfg(String path) {
        return plugin.lang().text(path);
    }

    /* ---------------------------------------------------------------- */
    /* Main form                                                         */
    /* ---------------------------------------------------------------- */

    public void openMainForm(Player player) {
        try {
            if (!canUseForm(player)) return;
            Storage data = Storage.getPlayer(player);
            if (data == null) {
                logError(plugin.lang().text("logs.storage-unavailable", "player", player.getName()), null);
                return;
            }
            boolean signedIn = data.alreadySignIn();
            String content = buildStatusContent(player, data);
            String signInButton = signedIn ? cfg("form.main.button-signed") : cfg("form.main.button-sign-in");

            SimpleForm.Builder form = SimpleForm.builder()
                .title(cfg("form.main.title"))
                .content(content)
                .button(signInButton)
                .button(cfg("form.main.button-retroactive"))
                .button(cfg("form.main.button-records"))
                .validResultHandler(response -> {
                    int id = response.clickedButtonId();
                    Scheduler.runTask(player, () -> {
                    if (!canUseForm(player)) return;
                        switch (id) {
                            case 0:
                                doSignIn(player);
                                break;
                            case 1:
                                openRetroactiveForm(player);
                                break;
                            case 2:
                                openRecordsForm(player);
                                break;
                            default:
                                break;
                        }
                    });
                });

            sendForm(player, form);
        } catch (Throwable t) {
            logError(cfg("logs.main-failed"), t);
        }
    }

    private String buildStatusContent(Player player, Storage data) {
        String format = cfg("form.main.status-format");
        String status = data.alreadySignIn() ? cfg("form.main.signed-in") : cfg("form.main.not-signed-in");
        int queue = SignInQueue.getInstance().getRank(player.getUniqueId());
        SignInDate today = SignInDate.getInstance(new Date());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("status", status);
        placeholders.put("continuous", String.valueOf(data.getContinuousSignIn()));
        placeholders.put("total", String.valueOf(data.getCumulativeNumber()));
        placeholders.put("month", String.valueOf(data.getCumulativeNumberOfMonth(today.getYear(), today.getMonth())));
        placeholders.put("cards", String.valueOf(data.getRetroactiveCard()));
        placeholders.put("queue", queue > 0 ? String.valueOf(queue) : cfg("form.main.not-signed-in"));
        return resolve(player, format, placeholders);
    }

    /* ---------------------------------------------------------------- */
    /* Sign-in action                                                    */
    /* ---------------------------------------------------------------- */

    private void doSignIn(Player player) {
        try {
            SignInService.Result result = SignInService.signIn(player);
            Storage data = Storage.getPlayer(player);
            switch (result) {
                case SUCCESS: {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("continuous", String.valueOf(data.getContinuousSignIn()));
                    sendMessage(player, "messages.sign-in-success", placeholders);
                    openMainForm(player);
                    break;
                }
                case CANCELLED:
                    sendMessage(player, "messages.sign-in-cancelled", null);
                    break;
                case ALREADY_SIGNED_IN:
                    sendMessage(player, "messages.sign-in-already", null);
                    break;
                case DISABLED_WORLD:
                    sendMessage(player, "messages.sign-in-disabled-world", null);
                    break;
                case INSUFFICIENT_ONLINE_TIME: {
                    long requirement = OnlineTimeRecord.getSignInRequirement(player);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("minute", String.valueOf(Math.max(1L, (requirement + 59999L) / 60000L)));
                    sendMessage(player, "messages.sign-in-insufficient-time", placeholders);
                    break;
                }
                case NO_PERMISSION:
                    sendMessage(player, "messages.no-permission", null);
                    break;
                default:
                    break;
            }
        } catch (Throwable t) {
            logError(cfg("logs.signin-failed"), t);
        }
    }

    /* ---------------------------------------------------------------- */
    /* Retroactive form                                                  */
    /* ---------------------------------------------------------------- */

    public void openRetroactiveForm(Player player) {
        openRetroactiveForm(player, 0);
    }

    private void openRetroactiveForm(Player player, int requestedPage) {
        try {
            if (!canUseForm(player)) return;
            Storage data = Storage.getPlayer(player);
            if (data == null) {
                logError(plugin.lang().text("logs.storage-unavailable", "player", player.getName()), null);
                return;
            }
            int days = Math.max(0, Math.min(3660, config().getInt("retroactive-days", 30)));
            int required = PluginControl.getRetroactiveCardQuantityRequired();

            List<SignInDate> available = collectRetroactiveDates(data, days);

            int pageCount = Math.max(1, (available.size() + 9) / 10);
            int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
            int start = page * 10;
            List<SignInDate> visible = available.subList(start, Math.min(start + 10, available.size()));
            List<Runnable> actions = new ArrayList<>();
            SimpleForm.Builder form = SimpleForm.builder().title(cfg("form.retroactive.title"));
            addButton(form, actions, cfg("form.retroactive.button-back"), () -> openMainForm(player));

            if (available.isEmpty()) {
                form.content(cfg("form.retroactive.empty"));
            } else {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("cards", String.valueOf(data.getRetroactiveCard()));
                placeholders.put("required", String.valueOf(required));
                placeholders.put("page", String.valueOf(page + 1));
                placeholders.put("pages", String.valueOf(pageCount));
                placeholders.put("count", String.valueOf(available.size()));
                form.content(resolve(player, cfg("form.retroactive.content"), placeholders) + "\n"
                    + resolve(player, cfg("form.retroactive.page-format"), placeholders));
                for (SignInDate date : visible) {
                    String fullDate = plugin.lang().text("date.full-format", "year", String.valueOf(date.getYear()), "date", formatDate(date));
                    addButton(form, actions, plugin.lang().text("form.retroactive.button-date", "date", fullDate, "required", String.valueOf(required)),
                        () -> doRetroactiveSignIn(player, date));
                }
            }
            if (page > 0) addButton(form, actions, cfg("form.retroactive.button-prev"), () -> openRetroactiveForm(player, page - 1));
            if (page + 1 < pageCount) addButton(form, actions, cfg("form.retroactive.button-next"), () -> openRetroactiveForm(player, page + 1));
            form.validResultHandler(response -> {
                int id = response.clickedButtonId();
                Scheduler.runTask(player, () -> {
                    if (canUseForm(player) && id >= 0 && id < actions.size()) actions.get(id).run();
                });
            });

            sendForm(player, form);
        } catch (Throwable t) {
            logError(cfg("logs.retroactive-failed"), t);
        }
    }

    private List<SignInDate> collectRetroactiveDates(Storage data, int days) {
        List<SignInDate> available = new ArrayList<>();
        SignInDate minimumDate = PluginControl.getRetroactiveCardMinimumDate();
        LocalDate today = LocalDate.now();
        Set<LocalDate> history = historyDates(data);
        LocalDate minimum = minimumDate == null ? LocalDate.of(1970, 1, 1) : localDate(minimumDate);
        for (int i = 1; i <= days; i++) {
            LocalDate candidate = today.minusDays(i);
            if (candidate.isBefore(minimum)) break;
            if (!history.contains(candidate)) {
                available.add(SignInDate.getInstance(candidate.getYear(), candidate.getMonthValue(), candidate.getDayOfMonth()));
            }
        }
        return available;
    }

    private void doRetroactiveSignIn(Player player, SignInDate date) {
        try {
            SignInService.Result result = SignInService.retroactiveSignIn(player, date);
            Storage data = Storage.getPlayer(player);
            switch (result) {
                case SUCCESS: {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("date", formatDate(date));
                    placeholders.put("continuous", String.valueOf(data.getContinuousSignIn()));
                    sendMessage(player, "messages.retroactive-success", placeholders);
                    openMainForm(player);
                    break;
                }
                case CANCELLED:
                    sendMessage(player, "messages.sign-in-cancelled", null);
                    break;
                case ALREADY_SIGNED_IN:
                    sendMessage(player, "messages.sign-in-already", null);
                    break;
                case RETROACTIVE_INVALID_DATE:
                    sendMessage(player, "messages.retroactive-invalid-date", null);
                    break;
                case RETROACTIVE_DISABLED:
                    sendMessage(player, "messages.retroactive-disabled", null);
                    break;
                case RETROACTIVE_COOLDOWN: {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("second", String.valueOf(data.getRetroactiveCardCooldown()));
                    sendMessage(player, "messages.retroactive-cooldown", placeholders);
                    break;
                }
                case RETROACTIVE_MINIMUM_DATE:
                    sendMessage(player, "messages.retroactive-minimum-date", null);
                    break;
                case RETROACTIVE_NEED_MORE_CARDS: {
                    Map<String, String> placeholders = new HashMap<>();
                    int needed = PluginControl.getRetroactiveCardQuantityRequired() - data.getRetroactiveCard();
                    placeholders.put("cards", String.valueOf(needed));
                    sendMessage(player, "messages.retroactive-need-more", placeholders);
                    break;
                }
                case DISABLED_WORLD:
                    sendMessage(player, "messages.sign-in-disabled-world", null);
                    break;
                case NO_PERMISSION:
                    sendMessage(player, "messages.no-permission", null);
                    break;
                default:
                    break;
            }
        } catch (Throwable t) {
            logError(cfg("logs.retroactive-action-failed"), t);
        }
    }

    /* ---------------------------------------------------------------- */
    /* Records form                                                      */
    /* ---------------------------------------------------------------- */

    public void openRecordsForm(Player player) {
        LocalDate today = LocalDate.now();
        openRecordsForm(player, today.getYear(), today.getMonthValue());
    }

    public void openRecordsForm(Player player, int year, int month) {
        openRecordsForm(player, YearMonth.of(year, month));
    }

    private void openRecordsForm(Player player, YearMonth requested) {
        try {
            if (!canUseForm(player)) return;
            Storage data = Storage.getPlayer(player);
            if (data == null) {
                logError(plugin.lang().text("logs.storage-unavailable", "player", player.getName()), null);
                return;
            }
            LocalDate today = LocalDate.now();
            YearMonth current = YearMonth.from(today);
            YearMonth earliest = MonthRecords.earliest(current, config().getInt("records-months", 12));
            YearMonth month = MonthRecords.clamp(requested, earliest, current);
            MonthRecords records = new MonthRecords(month, today, historyDates(data));
            Map<String, String> values = new HashMap<>();
            values.put("year", String.valueOf(month.getYear()));
            values.put("month", String.valueOf(month.getMonthValue()));
            values.put("count", String.valueOf(records.signed()));
            values.put("missed", String.valueOf(records.missed()));
            values.put("elapsed", String.valueOf(records.elapsed()));
            values.put("total", String.valueOf(records.total()));
            values.put("rate", String.valueOf(records.rate()));
            StringBuilder content = new StringBuilder(resolve(player,
                cfg("form.records.header"), values));
            content.append("\n").append(resolve(player, cfg("form.records.summary"), values));
            if (month.equals(current)) {
                values.put("date", String.valueOf(today.getDayOfMonth()));
                values.put("status", recordStatus(records.state(today.getDayOfMonth())));
                content.append("\n").append(resolve(player, cfg("form.records.today-format"), values));
            }

            List<Runnable> actions = new ArrayList<>();
            SimpleForm.Builder form = SimpleForm.builder().title(cfg("form.records.title"));
            addButton(form, actions, cfg("form.records.button-back"), () -> openMainForm(player));
            content.append("\n\n").append(records.grid(day -> plugin.lang().text("form.records.cell-format",
                "day", day < 10 ? "0" + day : String.valueOf(day), "mark", recordMark(records.state(day))),
                cfg("form.records.cell-separator")));
            Map<String, String> legend = new HashMap<>();
            legend.put("signed", recordSymbol(MonthRecords.State.SIGNED));
            legend.put("missed", recordSymbol(MonthRecords.State.MISSED));
            legend.put("pending", recordSymbol(MonthRecords.State.TODAY));
            legend.put("future", recordSymbol(MonthRecords.State.FUTURE));
            content.append("\n\n").append(resolve(player, cfg("form.records.legend"), legend));
            if (month.isAfter(earliest)) addButton(form, actions, cfg("form.records.button-prev"), () -> openRecordsForm(player, month.minusMonths(1)));
            if (month.isBefore(current)) addButton(form, actions, cfg("form.records.button-next"), () -> openRecordsForm(player, month.plusMonths(1)));
            if (!month.equals(current)) addButton(form, actions, cfg("form.records.button-current"), () -> openRecordsForm(player));
            form.content(content.toString());
            form.validResultHandler(response -> {
                int id = response.clickedButtonId();
                Scheduler.runTask(player, () -> {
                    if (canUseForm(player) && id >= 0 && id < actions.size()) actions.get(id).run();
                });
            });
            sendForm(player, form);
        } catch (Throwable t) {
            logError(cfg("logs.records-failed"), t);
        }
    }

    private String recordMark(MonthRecords.State state) {
        return recordSymbol(state);
    }

    private String recordSymbol(MonthRecords.State state) {
        String symbol;
        switch (state) {
            case SIGNED: symbol = cfg("form.records.signed"); break;
            case MISSED: symbol = cfg("form.records.missed"); break;
            case TODAY: symbol = cfg("form.records.pending"); break;
            default: symbol = cfg("form.records.future"); break;
        }
        return ChatColor.translateAlternateColorCodes('&', symbol) + ChatColor.RESET;
    }

    private String recordStatus(MonthRecords.State state) {
        switch (state) {
            case SIGNED: return recordSymbol(state) + " " + cfg("form.records.label-signed");
            case MISSED: return recordSymbol(state) + " " + cfg("form.records.label-missed");
            case TODAY: return recordSymbol(state) + " " + cfg("form.records.label-today");
            default: return recordSymbol(state) + " " + cfg("form.records.label-future");
        }
    }

    private void addButton(SimpleForm.Builder form, List<Runnable> actions, String label, Runnable action) {
        form.button(label);
        actions.add(action);
    }

    /* ---------------------------------------------------------------- */
    /* Helpers                                                           */
    /* ---------------------------------------------------------------- */

    private boolean canUseForm(Player player) {
        if (!player.isOnline()) return false;
        if (!player.hasPermission("litesigninbedrock.use")) {
            sendMessage(player, "messages.no-permission", null);
            return false;
        }
        if (!BedrockUtil.isBedrock(player)) {
            sendMessage(player, "messages.bedrock-only", null);
            return false;
        }
        return true;
    }

    private void sendForm(Player player, SimpleForm.Builder form) {
        if (!BedrockUtil.sendForm(player, form)) {
            logError(plugin.lang().text("logs.form-send-failed", "player", player.getName()), null);
        }
    }

    private static LocalDate localDate(SignInDate date) {
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    private Set<LocalDate> historyDates(Storage data) {
        Set<LocalDate> dates = new HashSet<>();
        for (SignInDate date : data.getHistory()) {
            if (date == null) continue;
            try {
                dates.add(localDate(date));
            } catch (java.time.DateTimeException invalid) {
                plugin.getLogger().warning(plugin.lang().text("logs.invalid-history", "date", date.toString()));
            }
        }
        return dates;
    }

    private String formatDate(SignInDate date) {
        int week = date.getWeek();
        String weekName = (week >= 1 && week <= 7) ? plugin.lang().weekdays().get(week - 1) : "";
        return plugin.lang().text("date.format", "year", String.valueOf(date.getYear()), "month", String.valueOf(date.getMonth()),
            "day", String.valueOf(date.getDay()), "weekday", weekName);
    }

    private void logError(String message, Throwable t) {
        if (t == null) {
            plugin.getLogger().log(Level.SEVERE, message);
        } else {
            plugin.getLogger().log(Level.SEVERE, message, t);
        }
    }

    private String resolve(Player player, String text, Map<String, String> placeholders) {
        String result = text == null ? "" : text;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && result.contains("%")) {
            result = PapiHook.setPlaceholders(player, result);
        }
        List<String> arguments = new ArrayList<>();
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                arguments.add(entry.getKey());
                arguments.add(entry.getValue());
            }
        }
        return dev.che1s.litesigninbedrock.i18n.LanguageManager.format(result, arguments.toArray(new String[0]));
    }

    private void sendMessage(Player player, String path, Map<String, String> placeholders) {
        String text = cfg(path);
        if (text == null || text.isEmpty()) {
            return;
        }
        Map<String, String> full = placeholders == null ? new HashMap<>() : new HashMap<>(placeholders);
        full.put("prefix", cfg("messages.prefix"));
        String resolved = resolve(player, text, full);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', resolved));
    }
}
