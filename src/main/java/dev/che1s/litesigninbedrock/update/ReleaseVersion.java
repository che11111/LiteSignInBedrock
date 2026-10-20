package dev.che1s.litesigninbedrock.update;

import java.math.BigInteger;
import java.util.regex.*;

/** Stable numeric tags: v1.2.3 and 1.2.3; build metadata does not affect ordering. */
public final class ReleaseVersion {
    private static final Pattern VERSION = Pattern.compile("[vV]?(\\d+(?:\\.\\d+){0,7})(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?");
    private ReleaseVersion() {}
    private static Matcher parse(String text) {
        if (text == null || text.length() > 128) return null;
        Matcher matcher = VERSION.matcher(text);
        return matcher.matches() ? matcher : null;
    }
    public static boolean isStable(String text) {
        Matcher parsed = parse(text);
        return parsed != null && parsed.group(2) == null;
    }
    public static boolean isNewer(String candidate, String installed) {
        Matcher next = parse(candidate), current = parse(installed);
        if (next == null || current == null || next.group(2) != null) return false;
        String[] left = next.group(1).split("\\."), right = current.group(1).split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            BigInteger a = i < left.length ? new BigInteger(left[i]) : BigInteger.ZERO;
            BigInteger b = i < right.length ? new BigInteger(right[i]) : BigInteger.ZERO;
            int result = a.compareTo(b);
            if (result != 0) return result > 0;
        }
        return current.group(2) != null;
    }
}
