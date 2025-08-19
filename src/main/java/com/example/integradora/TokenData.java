package com.example.integradora;

public class TokenData {
    private int idCuenta;
    private String fechaSolicitud;

    public TokenData(int idCuenta, String fechaSolicitud) {
        this.idCuenta = idCuenta;
        this.fechaSolicitud = fechaSolicitud;
    }

    public int getIdCuenta() {
        return idCuenta;
    }

    public String getFechaSolicitud() {
        return fechaSolicitud;
    }
}
