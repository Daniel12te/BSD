package com.bavaria.smartdispatch.bsd.repository;


import com.bavaria.smartdispatch.bsd.entity.Pedido;
import com.bavaria.smartdispatch.bsd.entity.Enums.EstadoDelPedido;
import com.bavaria.smartdispatch.bsd.entity.Enums.EstadoLogistico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumeroEntrega(String numeroEntrega);
    // Buscar si un pedido ya existe por su número de RR/Transporte (para evitar duplicados)


    // Buscar pedidos pendientes de notificación (para el botón de "Enviar Mensajes")
    List<Pedido> findByEstadoConfirmacion(EstadoDelPedido estadoConfirmacion);

    // Buscar pedidos que están en PISO (para reportes)
    List<Pedido> findByEstadoLogistico(EstadoLogistico estadoLogistico);
    
    // Buscar pedidos de un cliente específico (para ver historial)
    List<Pedido> findByCliente_CodigoCliente(String codigoCliente);
}
    

