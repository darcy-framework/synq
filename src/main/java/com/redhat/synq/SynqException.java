package com.redhat.synq;

public class SynqException extends RuntimeException {
    private static final long serialVersionUID = -2422116428308819350L;
    
    public SynqException() {
        super();
    }
    
    public SynqException(String message) {
        super(message);
    }
    
    public SynqException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SynqException(Throwable cause) {
        super(cause);
    }
}
