package com.yourapp.attendance.utils;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static String loggedInUser;
    private static final Map<String, String> sessionData = new HashMap<>();

    public static void setLoggedInUser(String username) {
        loggedInUser = username;
    }
    public static String getLoggedInUsername() {
        return loggedInUser;
    }

    public static String getLoggedInUser() {
        return loggedInUser;
    }

    public static void set(String key, String value) {
        sessionData.put(key, value);
    }

    public static String get(String key) {
        return sessionData.get(key);
    }

    public static void clearSession() {
        loggedInUser = null;
        sessionData.clear();
    }
}
