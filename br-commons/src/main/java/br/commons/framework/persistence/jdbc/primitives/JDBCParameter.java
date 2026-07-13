package br.commons.framework.persistence.jdbc.primitives;

import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public record JDBCParameter(
        int index,
        @Nullable Object value
) {

    public static List<JDBCParameter> of (@Nullable Object ... values) {
        val parameters = new ArrayList<JDBCParameter>();

        for (int i = 0; i < values.length; i++) {
            val value = values[i];
            parameters.add(of(i + 1, value));
        }
        return parameters;
    }

    private static JDBCParameter of (int index, @Nullable Object value) {
        return new JDBCParameter(index, value);
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));

        return parameters;
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2,
            @Nullable Integer index3, @Nullable Object value3
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));
        if (index3 != null) parameters.add(of(index3, value3));

        return parameters;
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2,
            @Nullable Integer index3, @Nullable Object value3,
            @Nullable Integer index4, @Nullable Object value4
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));
        if (index3 != null) parameters.add(of(index3, value3));
        if (index4 != null) parameters.add(of(index4, value4));

        return parameters;
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2,
            @Nullable Integer index3, @Nullable Object value3,
            @Nullable Integer index4, @Nullable Object value4,
            @Nullable Integer index5, @Nullable Object value5
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));
        if (index3 != null) parameters.add(of(index3, value3));
        if (index4 != null) parameters.add(of(index4, value4));
        if (index5 != null) parameters.add(of(index5, value5));

        return parameters;
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2,
            @Nullable Integer index3, @Nullable Object value3,
            @Nullable Integer index4, @Nullable Object value4,
            @Nullable Integer index5, @Nullable Object value5,
            @Nullable Integer index6, @Nullable Object value6
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));
        if (index3 != null) parameters.add(of(index3, value3));
        if (index4 != null) parameters.add(of(index4, value4));
        if (index5 != null) parameters.add(of(index5, value5));
        if (index6 != null) parameters.add(of(index6, value6));

        return parameters;
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2,
            @Nullable Integer index3, @Nullable Object value3,
            @Nullable Integer index4, @Nullable Object value4,
            @Nullable Integer index5, @Nullable Object value5,
            @Nullable Integer index6, @Nullable Object value6,
            @Nullable Integer index7, @Nullable Object value7
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));
        if (index3 != null) parameters.add(of(index3, value3));
        if (index4 != null) parameters.add(of(index4, value4));
        if (index5 != null) parameters.add(of(index5, value5));
        if (index6 != null) parameters.add(of(index6, value6));
        if (index7 != null) parameters.add(of(index7, value7));

        return parameters;
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2,
            @Nullable Integer index3, @Nullable Object value3,
            @Nullable Integer index4, @Nullable Object value4,
            @Nullable Integer index5, @Nullable Object value5,
            @Nullable Integer index6, @Nullable Object value6,
            @Nullable Integer index7, @Nullable Object value7,
            @Nullable Integer index8, @Nullable Object value8
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));
        if (index3 != null) parameters.add(of(index3, value3));
        if (index4 != null) parameters.add(of(index4, value4));
        if (index5 != null) parameters.add(of(index5, value5));
        if (index6 != null) parameters.add(of(index6, value6));
        if (index7 != null) parameters.add(of(index7, value7));
        if (index8 != null) parameters.add(of(index8, value8));

        return parameters;
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2,
            @Nullable Integer index3, @Nullable Object value3,
            @Nullable Integer index4, @Nullable Object value4,
            @Nullable Integer index5, @Nullable Object value5,
            @Nullable Integer index6, @Nullable Object value6,
            @Nullable Integer index7, @Nullable Object value7,
            @Nullable Integer index8, @Nullable Object value8,
            @Nullable Integer index9, @Nullable Object value9
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));
        if (index3 != null) parameters.add(of(index3, value3));
        if (index4 != null) parameters.add(of(index4, value4));
        if (index5 != null) parameters.add(of(index5, value5));
        if (index6 != null) parameters.add(of(index6, value6));
        if (index7 != null) parameters.add(of(index7, value7));
        if (index8 != null) parameters.add(of(index8, value8));
        if (index9 != null) parameters.add(of(index9, value9));

        return parameters;
    }

    public static List<JDBCParameter> of (
            @Nullable Integer index, @Nullable Object value,
            @Nullable Integer index2, @Nullable Object value2,
            @Nullable Integer index3, @Nullable Object value3,
            @Nullable Integer index4, @Nullable Object value4,
            @Nullable Integer index5, @Nullable Object value5,
            @Nullable Integer index6, @Nullable Object value6,
            @Nullable Integer index7, @Nullable Object value7,
            @Nullable Integer index8, @Nullable Object value8,
            @Nullable Integer index9, @Nullable Object value9,
            @Nullable Integer index10, @Nullable Object value10
    ) {
        val parameters = new ArrayList<JDBCParameter>();

        if (index != null) parameters.add(of(index, value));
        if (index2 != null) parameters.add(of(index2, value2));
        if (index3 != null) parameters.add(of(index3, value3));
        if (index4 != null) parameters.add(of(index4, value4));
        if (index5 != null) parameters.add(of(index5, value5));
        if (index6 != null) parameters.add(of(index6, value6));
        if (index7 != null) parameters.add(of(index7, value7));
        if (index8 != null) parameters.add(of(index8, value8));
        if (index9 != null) parameters.add(of(index9, value9));
        if (index10 != null) parameters.add(of(index10, value10));

        return parameters;
    }
}
