package com.example.integradora;

public class Usuario {
    private String nombre;
    private String apellidos;
    private String matricula;

    public Usuario(String nombre, String apellidos, String matricula) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.matricula = matricula;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }

    public String getMatricula() {
        return matricula;
    }
}
