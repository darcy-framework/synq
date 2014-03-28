package com.redhat.sync;

public class FailEventException extends RuntimeException {
    private static final long serialVersionUID = -7740040718087166163L;
    
    public FailEventException(Event<?> cause) {
        super(cause.toString());
    }
}
