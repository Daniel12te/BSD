package com.bavaria.smartdispatch.bsd.controller;

import com.bavaria.smartdispatch.bsd.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/carga")
public class CargaController {

    @Autowired
    private ExcelService excelService;

    @PostMapping("/pedidos")
    public ResponseEntity<?> cargarPedidos(
            @RequestParam("fileData") MultipartFile fileData,  // El archivo principal
            @RequestParam("fileRuta") MultipartFile fileRuta,  // El archivo de conductores
            @RequestParam(value = "transportesPiso", required = false, defaultValue = "") String transportesPisoTexto) {
        
        try {
            // Convertir texto manual a lista
            List<String> listaPiso = Arrays.stream(transportesPisoTexto.split("[\n, ]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            excelService.procesarArchivos(fileData, fileRuta, listaPiso);
            
            return ResponseEntity.ok("¡Carga exitosa! Se procesaron pedidos y se asignaron conductores.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error crítico: " + e.getMessage());
        }
    }
}