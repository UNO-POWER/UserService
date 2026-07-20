package com.unopower.userservice.service;

public class EmailYaExisteException extends RuntimeException {

    public EmailYaExisteException(String email) {
        super("Ya existe una cuenta con el correo '" + email + "'");
    }
}
