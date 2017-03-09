package com.bftcom.timesheet.export.utils;

/**
 *
 */
public interface Converter<FROM, TO> {

    TO convert(FROM source);

}
