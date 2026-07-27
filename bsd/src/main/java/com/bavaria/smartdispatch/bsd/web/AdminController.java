package com.bavaria.smartdispatch.bsd.web;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.transaction.annotation.Transactional;



import com.bavaria.smartdispatch.bsd.repository.PedidoRepository;





@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @DeleteMapping("/limpiar-pedidos")
    @Transactional
    public ResponseEntity<Map<String, String>> limpiarBaseDeDatos() {
        try {

            pedidoRepository.deleteAll();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Tabla de pedidos vaciada correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}