package com.budget.api.util;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FormatUtils {

    public static final DateTimeFormatter DATE_STORAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormatUtils() {
    }

    public static NumberFormat brazilianCurrency() {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    }
}
