package com.unopower.userservice.service;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Usuario o contrasena invalidos");
    }
}
