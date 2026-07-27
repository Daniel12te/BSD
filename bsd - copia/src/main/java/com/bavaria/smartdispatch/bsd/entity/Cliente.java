package com.bavaria.smartdispatch.bsd.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String codigoCliente;
        private String nombre;
        private String telefono;
    
        // Getters y Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCodigoCliente() { return codigoCliente; }
        public void setCodigoCliente(String codigoCliente) { this.codigoCliente = codigoCliente; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
    }

