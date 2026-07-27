package com.bavaria.smartdispatch.bsd.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable; 

@Entity
@Table(name = "clientes")
public class Cliente implements Persistable<Long> { 
    @Id
    private Long id; 
    private String codigoCliente;
    private String nombre;
    private String telefono;


    @Transient
    private boolean isNew = true;

    public Cliente() {}
    @Override
    public boolean isNew() {
        return isNew; 
    }

    @PostLoad 
    void markNotNew() {
        this.isNew = false; 
    }
    //  GETTERS Y SETTERS 
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoCliente() { return codigoCliente; }
    public void setCodigoCliente(String codigoCliente) { this.codigoCliente = codigoCliente; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
} 
