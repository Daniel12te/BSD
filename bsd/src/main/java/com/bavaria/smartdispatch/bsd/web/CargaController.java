package com.bavaria.smartdispatch.bsd.web;

import java.util.List; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import com.bavaria.smartdispatch.bsd.entity.Pedido;
import com.bavaria.smartdispatch.bsd.repository.PedidoRepository;
import com.bavaria.smartdispatch.bsd.service.ExcelService; // Tu servicio

@RestController
@RequestMapping("/api/carga")

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5175", "*"}) 

public class CargaController {

    @Autowired 
    private PedidoRepository pedidoRepository;

    @Autowired
    private ExcelService excelService; 

    
    @PostMapping("/pedidos")
    public ResponseEntity<?> cargarPedidos(
            @RequestParam("fileData") MultipartFile fileData,
            @RequestParam("fileRuta") MultipartFile fileRuta,
            @RequestParam("transportesPiso") String transportesPiso // Recibimos Texto
    ) {
        
        if (fileData.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Falta el archivo Data.");
        }

        try {
            List<String> listaTransportes = List.of(transportesPiso);

            
            excelService.procesarArchivos(fileData, fileRuta, listaTransportes);

            List<Pedido> listaActualizada = pedidoRepository.findAll();

            return ResponseEntity.ok(listaActualizada);

        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.internalServerError().body("Error crítico: " + e.getMessage());
        }
    }
    
    @GetMapping("/pedidos") 
    public ResponseEntity<List<Pedido>> listarPedidos() {
        try {
            List<Pedido> pedidos = pedidoRepository.findAll();
            return ResponseEntity.ok(pedidos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}