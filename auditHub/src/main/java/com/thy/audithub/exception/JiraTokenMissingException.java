package com.thy.audithub.exception;

public class JiraTokenMissingException extends RuntimeException {
    public JiraTokenMissingException(String message) {
        super(message);
    }
}
