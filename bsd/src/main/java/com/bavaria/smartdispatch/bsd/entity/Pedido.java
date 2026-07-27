package com.bavaria.smartdispatch.bsd.entity;

import com.bavaria.smartdispatch.bsd.entity.Enums.EstadoDelPedido;
import com.bavaria.smartdispatch.bsd.entity.Enums.EstadoLogistico;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "numero_entrega", unique = true, nullable = false)
    private String numeroEntrega; 


    private String numeroTransporte; 

    private Double valorTotal;
    private String placaVehiculo;
    
    // DATOS CONDUCTOR 
    private String nombreConductor;
    private String telefonoConductor;

    private LocalDate fechaEntrega;
    private LocalDateTime fechaCarga;


    @Enumerated(EnumType.STRING)
    private EstadoLogistico estadoLogistico;

    @Enumerated(EnumType.STRING)
    private EstadoDelPedido estadoConfirmacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id") 
    private Cliente cliente;
    
    @Column(name = "estado_notificacion")
    private String estadoNotificacion;

    @Column(name = "fecha_envio_mensaje")
    private LocalDateTime fechaEnvioMensaje;
    
    @Column(name = "lista_productos", length = 4000)
    private String listaProductos;


    public Pedido() {}

    //  GETTERS Y SETTERS 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroEntrega() { return numeroEntrega; }
    public void setNumeroEntrega(String numeroEntrega) { this.numeroEntrega = numeroEntrega; }

    
    public String getNumeroTransporte() { return numeroTransporte; }
    public void setNumeroTransporte(String numeroTransporte) { this.numeroTransporte = numeroTransporte; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }

    public String getPlacaVehiculo() { return placaVehiculo; }
    public void setPlacaVehiculo(String placaVehiculo) { this.placaVehiculo = placaVehiculo; }

    public String getNombreConductor() { return nombreConductor; }
    public void setNombreConductor(String nombreConductor) { this.nombreConductor = nombreConductor; }

    public String getTelefonoConductor() { return telefonoConductor; }
    public void setTelefonoConductor(String telefonoConductor) { this.telefonoConductor = telefonoConductor; }

    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }

    public EstadoLogistico getEstadoLogistico() { return estadoLogistico; }
    public void setEstadoLogistico(EstadoLogistico estadoLogistico) { this.estadoLogistico = estadoLogistico; }

    public EstadoDelPedido getEstadoConfirmacion() { return estadoConfirmacion; }
    public void setEstadoConfirmacion(EstadoDelPedido estadoConfirmacion) { this.estadoConfirmacion = estadoConfirmacion; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public String getEstadoNotificacion() { return estadoNotificacion;}
    public void setEstadoNotificacion(String estadoNotificacion) {this.estadoNotificacion = estadoNotificacion;}

    public LocalDateTime getFechaEnvioMensaje() {return fechaEnvioMensaje;}
    public void setFechaEnvioMensaje(LocalDateTime fechaEnvioMensaje) {this.fechaEnvioMensaje = fechaEnvioMensaje;}

    public String getListaProductos() { return listaProductos; }
    public void setListaProductos(String listaProductos) { this.listaProductos = listaProductos; }
}