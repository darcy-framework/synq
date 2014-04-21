package com.redhat.synq;

public class TimeoutException extends SynqException {
    private static final long serialVersionUID = 7194182399119358208L;
    
    public TimeoutException(Event<?> event) {
        super("Timed out waiting for event to occur: " + event.toString());
    }
}
