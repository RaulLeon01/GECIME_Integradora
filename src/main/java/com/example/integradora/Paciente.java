package com.example.integradora;

public class Paciente {

    private String nombre;
    private String matricula;
    private String correo;
    private String curp;
    private String telefono;
    private String telefonoEmergencia;
    private String fechaNacimiento;

    public Paciente(String nombre, String matricula, String correo, String curp, String telefono, String telefonoEmergencia, String fechaNacimiento) {
        this.nombre = nombre;
        this.matricula = matricula;
        this.correo = correo;
        this.curp = curp;
        this.telefono = telefono;
        this.telefonoEmergencia = telefonoEmergencia;
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getNombre() { return nombre; }
    public String getMatricula() { return matricula; }
    public String getCorreo() { return correo; }
    public String getCurp() { return curp; }
    public String getTelefono() { return telefono; }
    public String getTelefonoEmergencia() { return telefonoEmergencia; }
    public String getFechaNacimiento() { return fechaNacimiento; }
}
