package com.bavaria.smartdispatch.bsd.repository;
import com.bavaria.smartdispatch.bsd.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCodigoCliente(String codigoCliente);
}