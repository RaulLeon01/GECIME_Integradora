package com.example.integradora;

public class CitaHistorial {
    private String matricula;
    private String nombre;
    private String apellidos;
    private String doctor;
    private String fecha;
    private String hora;
    private String malestar;
    private String observaciones;

    public CitaHistorial(String matricula, String nombre, String apellidos, String doctor,
                         String fecha, String hora, String malestar, String observaciones) {
        this.matricula = matricula;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.doctor = doctor;
        this.fecha = fecha;
        this.hora = hora;
        this.malestar = malestar;
        this.observaciones = observaciones;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getDoctor() {
        return doctor;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHora() {
        return hora;
    }

    public String getMalestar() {
        return malestar;
    }

    public String getObservaciones() {
        return observaciones;
    }
}
