package com.bavaria.smartdispatch.bsd.repository;


import com.bavaria.smartdispatch.bsd.entity.Pedido;
import com.bavaria.smartdispatch.bsd.entity.Enums.EstadoLogistico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumeroEntrega(String numeroEntrega);
    boolean existsByNumeroEntrega(String numeroEntrega);
    List<Pedido> findByEstadoLogistico(EstadoLogistico estadoLogistico);
    List<Pedido> findByCliente_CodigoCliente(String codigoCliente);
}


