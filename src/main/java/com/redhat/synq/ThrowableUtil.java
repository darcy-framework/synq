package com.redhat.synq;

public class ThrowableUtil {
    public static RuntimeException throwUnchecked(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else if (throwable instanceof Error) {
            throw (Error) throwable;
        } else {
            throw new RuntimeException(throwable);
        }
    }
}
