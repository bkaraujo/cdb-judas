package br.commons.tools;

import br.commons.Logger;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

@NullMarked
public abstract class Strings {

    public static final String EMPTY = "";
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String PARAGRAPH_SEPARATOR = LINE_SEPARATOR + LINE_SEPARATOR;

    public static boolean isEmpty(String prefix) {
        if (prefix.isBlank()) return true;
        return prefix.trim().isEmpty();
    }

    public static boolean isAscii(String desired) {
        for (int i = 0; i < desired.length(); i++) {
            if (desired.charAt(i) > 127) {
                return false;
            }
        }

        return true;
    }

    public static boolean isNumeric(String string) {
        return string.chars().allMatch(Character::isDigit);
    }

    public static boolean isUpperCase(String string) {
        return string.chars().allMatch(Character::isUpperCase);
    }

    public static boolean isLowerCase(String string) {
        return string.chars().allMatch(Character::isLowerCase);
    }

    public static String lpad(byte text, int size, char filter) {
        return lpad(String.valueOf(text), size, filter);
    }

    public static String lpad(short text, int size, char filter) {
        return lpad(String.valueOf(text), size, filter);
    }

    public static String lpad(int text, int size, char filter) {
        return lpad(String.valueOf(text), size, filter);
    }

    public static String lpad(long text, int size, char filter) {
        return lpad(String.valueOf(text), size, filter);
    }

    public static String lpad(String text, int size, char filler) {
        val length = text.length();
        if (length > size) return text.substring(0, size);
        if (length == size) return text;

        val result = new char[size];

        int index = 0;
        for (; index < size - length; index++) { result[index] = filler; }
        for (; index < length; index++) { result[index] = text.charAt(index); }

        return new String(result);
    }

    public static String rpad(byte text, int size, char filter) {
        return rpad(String.valueOf(text), size, filter);
    }

    public static String rpad(short text, int size, char filter) {
        return rpad(String.valueOf(text), size, filter);
    }

    public static String rpad(int text, int size, char filter) {
        return rpad(String.valueOf(text), size, filter);
    }

    public static String rpad(long text, int size, char filter) {
        return rpad(String.valueOf(text), size, filter);
    }

    public static String rpad(String text, int size, char filler) {
        val length = text.length();
        if (length > size) return text.substring(0, size);
        if (length == size) return text;

        val result = new char[size];
        int index = 0;
        for (; index < length; index++) { result[index] = text.charAt(index); }
        for (; index < size - length; index++) { result[index] = filler; }

        return new String(result);
    }

    public static String onlyNumeric(String number) {
        return number.replaceAll("\\D", EMPTY);
    }

    public static String read(File file) {
        return read(file.toPath());
    }

    public static String read(Path path) {
        val string = read(path, StandardCharsets.UTF_8);
        return string.isEmpty() ? read(path, StandardCharsets.ISO_8859_1) : string;
    }

    public static String read(Path path, Charset charset) {
        try {
            return Files.readString(path, charset);
        } catch (Exception ex) {
            return EMPTY;
        }
    }

    public static String read(InputStream stream) {
        try {
            return read(stream.readAllBytes());
        } catch (IOException exception) {
            Logger.error("Failed to read stream: %s", exception.toString());
            return EMPTY;
        }
    }

    public static String read(byte[] bytes) {
        val detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();

        val encoding = detector.getDetectedCharset();
        val charset = Charset.forName(encoding != null ? encoding : "UTF-8");
        return read(bytes, charset);
    }

    public static String read(byte[] bytes, Charset charset) {
        try {
            return new String(bytes, charset);
        } catch (Exception ex) {
            return EMPTY;
        }
    }

    public static boolean in(String string, String ... candidates){
        return Set.of(candidates).contains(string);
    }

    public static boolean containsAny(String text, String ... terms) {
        return containsAny(text, Arrays.asList(terms));
    }

    public static boolean containsAny(String text, Collection<String> terms) {
        return terms.stream().anyMatch(text::contains);
    }

    public static int count(String string, char token) {
        return (int) string.chars().filter(ch -> ch == token).count();
    }

    public static Set<String> matches(Pattern pattern, @Nullable String text) {
        val matches = new HashSet<String>();
        if (text == null || text.isEmpty()) return matches;

        val matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        return matches;
    }

    public static String floats(float [] floats) {
        if (floats.length == 0) return EMPTY;

        val builder = new StringBuilder("[");
        for (int i = 0; i < floats.length; i++) {
            builder.append(floats[i]);
            if (i < floats.length - 1) builder.append(", ");
        }

        builder.append("]");
        return builder.toString();
    }

    public static List<String> split(String text, int chunkSize) {
        val length = text.length();
        val numChunks = (length + chunkSize - 1) / chunkSize;
        val chunks = new ArrayList<String>(numChunks);

        val builder = new StringBuilder(chunkSize);

        for (int i = 0; i < length; i += chunkSize) {
            builder.setLength(0);

            val end = Math.min(i + chunkSize, length);
            builder.append(text, i, end);
            chunks.add(builder.toString());
        }

        return chunks;
    }

    public static String sha256(String text) {
        try {
            val digest = MessageDigest.getInstance("SHA-256");
            val hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            val hexString = new StringBuilder(64);
            for (val b : hash) {
                val hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception ex) {
            Logger.error("Failed to create SHA-256: %s", ex);
            return EMPTY;
        }
    }

    public static byte[] bytes(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            Logger.fatal("null or empty string passed to bytes");
            return new byte[]{0};
        }

        val tokens = name.split(",", -1);
        val values = new byte[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Byte.parseByte(tokens[i].trim());
        }

        return values;
    }

    public static short[] shorts(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            Logger.fatal("null or empty string passed to shorts");
            return new short[]{0};
        }

        val tokens = name.split(",", -1);
        val values = new short[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Short.parseShort(tokens[i].trim());
        }

        return values;
    }

    public static int[] ints(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            Logger.fatal("null or empty string passed to ints");
            return new int[]{0};
        }

        val tokens = name.split(",", -1);
        val values = new int[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.parseInt(tokens[i].trim());
        }

        return values;
    }

    public static long[] longs(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            Logger.fatal("null or empty string passed to long");
            return new long[]{0};
        }

        val tokens = name.split(",", -1);
        val values = new long[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Long.parseLong(tokens[i].trim());
        }

        return values;
    }

    public static float[] floats(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            Logger.fatal("null or empty string passed to floats");
            return new float[]{0};
        }

        val tokens = name.split(",", -1);
        val values = new float[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Float.parseFloat(tokens[i].trim());
        }

        return values;
    }

    public static double[] doubles(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            Logger.fatal("null or empty string passed to doubles");
            return new double[]{0};
        }

        val tokens = name.split(",", -1);
        val values = new double[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Double.parseDouble(tokens[i].trim());
        }

        return values;
    }

    public static String upper(Object desired) {
        String string = EMPTY;

        if (desired instanceof String s) { string = s; }
        else { string = desired.toString(); }

        if (string.trim().isEmpty()) return EMPTY;

        // Para ASCII, usa Locale.ENGLISH para evitar problemas com
        // mapeamentos sensíveis ao locale (ex: 'i' turco -> 'İ')
        if (isAscii(string)) return string.toUpperCase(Locale.ENGLISH);

        // Para Unicode, aplica toUpperCase com Locale.ROOT, que segue as
        // regras de caixa alta do Unicode sem interferências de locale
        return string.toUpperCase(Locale.ROOT);
    }

    public static String lower(Object desired) {
        String string = EMPTY;

        if (desired instanceof String s) { string = s; }
        else { string = desired.toString(); }
        if (string.trim().isEmpty()) return EMPTY;

        // Para ASCII, usa Locale.ENGLISH para evitar problemas com
        // mapeamentos sensíveis ao locale (ex: 'i' turco -> 'İ')
        if (isAscii(string)) return string.toLowerCase(Locale.ENGLISH);

        // Para Unicode, aplica toUpperCase com Locale.ROOT, que segue as
        // regras de caixa alta do Unicode sem interferências de locale
        return string.toLowerCase(Locale.ROOT);
    }

    public static String orEmpty(@Nullable String message) {
        if (message == null) return EMPTY;
        return message;
    }

    /** Anexa {@code part} ao buffer separando por um espaço, ignorando partes vazias. */
    public static void appendToken(StringBuilder buffer, String part) {
        if (part.isEmpty()) return;
        if (!buffer.isEmpty()) buffer.append(' ');
        buffer.append(part);
    }

    public static String or(@Nullable Object value, String fallback) {
        if (value == null) return fallback;
        return value.toString();
    }
}
