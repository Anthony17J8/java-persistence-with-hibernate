package com.ico.ltd.concurrency.domain;

public class InvalidBidException extends Exception {

    public InvalidBidException(String message) {
        super(message);
    }
}