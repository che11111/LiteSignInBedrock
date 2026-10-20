package dev.che1s.litesigninbedrock;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.function.IntFunction;

/** A per-form snapshot. Each calendar day belongs to exactly one state. */
final class MonthRecords {
    enum State { SIGNED, MISSED, TODAY, FUTURE }

    private final YearMonth month;
    private final State[] days;
    private final int elapsed;
    private int signed;
    private int missed;

    MonthRecords(YearMonth month, LocalDate today, Set<LocalDate> history) {
        this.month = month;
        this.days = new State[month.lengthOfMonth()];
        this.elapsed = month.isBefore(YearMonth.from(today)) ? days.length
            : month.equals(YearMonth.from(today)) ? today.getDayOfMonth() : 0;
        for (int day = 1; day <= days.length; day++) {
            LocalDate date = month.atDay(day);
            State state = date.isAfter(today) ? State.FUTURE
                : history.contains(date) ? State.SIGNED
                : date.equals(today) ? State.TODAY : State.MISSED;
            days[day - 1] = state;
            if (state == State.SIGNED) signed++;
            if (state == State.MISSED) missed++;
        }
    }

    State state(int day) { return days[day - 1]; }
    int signed() { return signed; }
    int missed() { return missed; }
    int elapsed() { return elapsed; }
    int total() { return days.length; }
    int rate() { return elapsed == 0 ? 0 : (int) Math.round(signed * 100.0 / elapsed); }
    /** All dates in chronological order, five cells per row, including a final partial row. */
    String grid(IntFunction<String> cell, String separator) {
        StringBuilder text = new StringBuilder();
        for (int day = 1; day <= days.length; day++) {
            if (day > 1) text.append((day - 1) % 5 == 0 ? "\n" : separator);
            text.append(cell.apply(day));
        }
        return text.toString();
    }

    static YearMonth earliest(YearMonth current, int monthsBack) {
        long available = (current.getYear() - 1970L) * 12 + current.getMonthValue();
        return current.minusMonths(Math.min(Math.max(1L, monthsBack), available) - 1);
    }

    static YearMonth clamp(YearMonth requested, YearMonth earliest, YearMonth current) {
        return requested.isBefore(earliest) ? earliest : requested.isAfter(current) ? current : requested;
    }
}
