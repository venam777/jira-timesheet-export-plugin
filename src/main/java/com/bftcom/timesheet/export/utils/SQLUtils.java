package com.bftcom.timesheet.export.utils;

import java.util.Collection;

/**
 *
 */
public class SQLUtils {

    public static  <T> String collectionToString(Collection<T> collection, Converter<T, String> converter) {
        if (collection == null || collection.size() ==0) {
            return "()";
        }
        StringBuilder builder = new StringBuilder("(");
        for (T value : collection) {
            builder.append(converter.convert(value)).append(", ");
        }
        return builder.substring(0, builder.length() - 2) + ")";
    }


}
