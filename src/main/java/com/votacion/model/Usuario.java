package com.votacion.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Usuario implements Serializable {

    private int id;
    private String username;
    private String passwordHash;
    private String email;
    private String rol; // 'ADMIN' o 'VOTANTE'
    private LocalDateTime fechaCreacion;

    public Usuario() {}

    public Usuario(int id, String username, String passwordHash, String email, String rol, LocalDateTime fechaCreacion) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.rol = rol;
        this.fechaCreacion = fechaCreacion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
