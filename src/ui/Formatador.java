package ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class Formatador {

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private Formatador() {
    }

    public static String moeda(BigDecimal valor) {
        return MOEDA.format(valor);
    }

    public static String lista(List<?> itens) {
        return itens.isEmpty() ? "-" : itens.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}
