package com.unopower.userservice.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 40)
    private String username;

    // Nullable: cuentas creadas antes de exigir correo no lo tienen (unique permite varios NULL).
    @Column(length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "partidas_jugadas", nullable = false)
    private int partidasJugadas = 0;

    @Column(name = "partidas_ganadas", nullable = false)
    private int partidasGanadas = 0;

    @Column(name = "racha_actual", nullable = false)
    private int rachaActual = 0;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected Usuario() {
        // JPA
    }

    public Usuario(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Usuario(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    /** HU perfil: el username es editable desde Configuracion (el JWT se re-emite al cambiarlo). */
    public void cambiarUsername(String nuevoUsername) {
        this.username = nuevoUsername;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getPartidasJugadas() {
        return partidasJugadas;
    }

    public int getPartidasGanadas() {
        return partidasGanadas;
    }

    public int getRachaActual() {
        return rachaActual;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    /** HU-07/HU-03: llamado por Game Service al terminar una partida. */
    public void registrarResultadoPartida(boolean gano) {
        this.partidasJugadas++;
        if (gano) {
            this.partidasGanadas++;
            this.rachaActual++;
        } else {
            this.rachaActual = 0;
        }
    }
}
