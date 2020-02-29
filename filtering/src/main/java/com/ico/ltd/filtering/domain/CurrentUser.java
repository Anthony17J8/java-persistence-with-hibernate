package com.ico.ltd.filtering.domain;

/**
 * Thread-local
 */
public class CurrentUser extends ThreadLocal<User> {

    public static CurrentUser INSTANCE = new CurrentUser();

    private CurrentUser() {
    }
}
