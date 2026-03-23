package tn.edu.esprit.services;

import tn.edu.esprit.entities.Users;

public final class AppSession {

    private static Users currentUser;

    private AppSession() {
    }

    public static void setCurrentUser(Users user) {
        currentUser = user;
    }

    public static Users getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}