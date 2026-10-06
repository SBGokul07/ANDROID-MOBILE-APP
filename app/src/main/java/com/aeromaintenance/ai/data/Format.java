package com.aeromaintenance.ai.data;

/** Number formatting used everywhere in the app: fixed decimals and thousands separators. */
public final class Format {

    private Format() {
    }

    /** {@code number(8421, 0)} → "8,421"; {@code number(0.9134, 2)} → "0.91". */
    public static String number(double value, int decimals) {
        double factor = Math.pow(10.0, decimals);
        double rounded = Math.round(value * factor) / factor;
        if (decimals == 0) {
            return groupThousands(Math.round(rounded));
        }
        long whole = (long) rounded;
        long frac = Math.abs(Math.round((rounded - whole) * factor));
        String sign = (rounded < 0 && whole == 0L) ? "-" : "";
        StringBuilder fracText = new StringBuilder(Long.toString(frac));
        while (fracText.length() < decimals) fracText.insert(0, '0');
        return sign + groupThousands(whole) + "." + fracText;
    }

    /** Signed version: "+0.052", "-1.3". */
    public static String signed(double value, int decimals) {
        return (value >= 0 ? "+" : "") + number(value, decimals);
    }

    private static String groupThousands(long n) {
        String s = Long.toString(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) sb.append(',');
            sb.append(s.charAt(i));
        }
        return (n < 0 ? "-" : "") + sb;
    }
}
