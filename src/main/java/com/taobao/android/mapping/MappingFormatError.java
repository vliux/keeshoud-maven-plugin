package com.taobao.android.mapping;

/**
 * Created by vliux on 15-11-26.
 */
public class MappingFormatError extends Exception{

    public MappingFormatError(String message) {
        super(message);
    }

    public MappingFormatError(String message, Throwable cause) {
        super(message, cause);
    }

    public MappingFormatError(Throwable cause) {
        super(cause);
    }

    /*public MappingFormatError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }*/
}