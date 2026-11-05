package com.traceroute.validation;

/** Format checks only; these cannot establish whether an address or number exists. */
public final class ContactFormats {
    private ContactFormats() {}
    public static final String EMAIL = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}";
    public static boolean email(String value) {
        return value != null && value.length() <= 254 && value.matches(EMAIL)
            && value.indexOf('@') <= 64;
    }
    public static boolean sender(String value) {
        if (value == null || value.isBlank()) return true;
        String text = value.strip();
        if (email(text)) return true;
        if (!text.matches("\\+?[0-9]+(?:[ -]?(?:[0-9]+|\\([0-9]{2,5}\\)))*")) return false;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.charAt(0) == '0') return false;
        return text.startsWith("+") ? digits.length() >= 8 && digits.length() <= 15 : digits.length() == 10;
    }
}
