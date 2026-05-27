package br.commons.tools.platform.terminal;

import br.commons.tools.Meta;
import lombok.val;
import org.jspecify.annotations.NullMarked;

public enum TerminalColor {

    // --- Controle ---
    RESET("\033[0m"),

    // --- Texto (Foreground) ---
    BLACK(ofHex("#000000")),
    RED(ofHex("#C00000")),
    GREEN(ofHex("#00FF00")),
    YELLOW(ofHex("#C0C000")),
    BLUE(ofHex("#5555FF")),
    PURPLE(ofHex("#C000C0")),
    CYAN(ofHex("#00C0C0")),
    WHITE(ofHex("#C0C0C0")),

    // --- Texto em Negrito (Bold / Bright) ---
    BLACK_BOLD(ofHex("#808080")),
    RED_BOLD(ofHex("#F14C4C")),
    GREEN_BOLD(ofHex("#25D96B")),
    YELLOW_BOLD(ofHex("#F4D03F")),
    BLUE_BOLD(ofHex("#3B8EEA")),
    PURPLE_BOLD(ofHex("#FF00FF")),
    CYAN_BOLD(ofHex("#29B8DB")),
    WHITE_BOLD(ofHex("#FFFFFF")),

    // --- Fundo (Background) ---
    BLACK_BACKGROUND(ofHexBG("#000000")),
    RED_BACKGROUND(ofHexBG("#C00000")),
    GREEN_BACKGROUND(ofHexBG("#00C000")),
    YELLOW_BACKGROUND(ofHexBG("#C0C000")),
    BLUE_BACKGROUND(ofHexBG("#0000C0")),
    PURPLE_BACKGROUND(ofHexBG("#C000C0")),
    CYAN_BACKGROUND(ofHexBG("#00C0C0")),
    WHITE_BACKGROUND(ofHexBG("#C0C0C0"));

    public final String color;

    /**
     * Construtor Único (conforme solicitado).
     * Recebe apenas a String final já formatada para o terminal.
     */
    TerminalColor(String desired) { color = desired; }

    @Override
    public String toString() { return color; }
    public String colorize(String text) { return color + text + RESET.color; }

    public static String ofRGB(int r, int g, int b) {return Ansi.rgb(r, g, b); }
    public static String ofHex(String hex) { return Ansi.hex(hex); }
    public static String ofHexBG(String hex) { return Ansi.hexBg(hex); }

    // ========================================================================
    //  CLASSE INTERNA "FACTORY" (Necessária para evitar Forward Reference)
    // ========================================================================
    @NullMarked
    private static class Ansi {

        /** Gera código para TEXTO via HEX. */
        static String hex(String hex) { return fromHEX(hex, false); }

        /** Gera código para FUNDO via HEX. */
        static String hexBg(String hex) { return fromHEX(hex, true); }

        /** Gera código para TEXTO via RGB. */
        static String rgb(int r, int g, int b) {
            return fromRGB(r, g, b, false);
        }

        // --- Lógica de Conversão ---

        private static String fromHEX(String hex, boolean isBg) {
            if (!hex.startsWith("#")) {
                Meta.exit("Hexadecimal String must start with #");
                throw new RuntimeException("Unreachable");
            }

            val code = hex.substring(1);
            int r = Integer.valueOf(code.substring(0, 2), 16);
            int g = Integer.valueOf(code.substring(2, 4), 16);
            int b = Integer.valueOf(code.substring(4, 6), 16);

            return fromRGB(r, g, b, isBg);
        }

        private static String fromRGB(int r, int g, int b, boolean isBg) {
            return "\033[%d;2;%d;%d;%dm".formatted(
                    isBg ? 48 : 38,
                    Math.clamp(r, 0, 255),
                    Math.clamp(g, 0, 255),
                    Math.clamp(b, 0, 255)
            );
        }
    }
}
