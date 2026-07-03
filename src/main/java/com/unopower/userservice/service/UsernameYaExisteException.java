package com.unopower.userservice.service;

public class UsernameYaExisteException extends RuntimeException {
    public UsernameYaExisteException(String username) {
        super("El usuario '" + username + "' ya existe");
    }
}
