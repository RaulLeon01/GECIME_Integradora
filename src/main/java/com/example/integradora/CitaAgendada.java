package com.example.integradora;

import javafx.scene.control.Button;

public class CitaAgendada {
    private String matricula, nombre, apellidos, doctor, fecha, hora, malestar;
    private Button btnModificar;
    private int idCita, idPaciente, idMedico;

    public CitaAgendada(String matricula, String nombre, String apellidos, String doctor,
                        String fecha, String hora, String malestar,
                        Button btnModificar, int idCita, int idPaciente, int idMedico) {
        this.matricula = matricula;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.doctor = doctor;
        this.fecha = fecha;
        this.hora = hora;
        this.malestar = malestar;
        this.btnModificar = btnModificar;
        this.idCita = idCita;
        this.idPaciente = idPaciente;
        this.idMedico = idMedico;
    }

    // Getters
    public String getMatricula() { return matricula; }
    public String getNombre() { return nombre; }
    public String getApellidos() { return apellidos; }
    public String getDoctor() { return doctor; }
    public String getFecha() { return fecha; }
    public String getHora() { return hora; }
    public String getMalestar() { return malestar; }
    public Button getBtnModificar() { return btnModificar; }
    public int getIdCita() { return idCita; }
    public int getIdPaciente() { return idPaciente; }
    public int getIdMedico() { return idMedico; }
}
