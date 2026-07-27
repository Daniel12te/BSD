package com.bavaria.smartdispatch.bsd.service.impl;

import com.bavaria.smartdispatch.bsd.entity.Cliente;
import com.bavaria.smartdispatch.bsd.entity.Enums.EstadoLogistico;
import com.bavaria.smartdispatch.bsd.entity.Pedido;
import com.bavaria.smartdispatch.bsd.repository.ClienteRepository;
import com.bavaria.smartdispatch.bsd.repository.PedidoRepository;
import com.bavaria.smartdispatch.bsd.service.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelServiceImpl implements ExcelService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;
    


    @Override
    @Transactional
    public void procesarArchivos(MultipartFile fileData, MultipartFile fileRuta, List<String> numerosTransportePiso) throws IOException {

        DataFormatter df = new DataFormatter();
        Map<String, Pedido> mapaPedidos = new HashMap<>(); 
        Map<String, String[]> infoConductores = new HashMap<>();


        List<String> transportesEnPisoLimpios = numerosTransportePiso.stream()
                .flatMap(s -> java.util.Arrays.stream(s.split("[,\\s]+")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (fileRuta != null && !fileRuta.isEmpty()) {
            try (InputStream isRuta = fileRuta.getInputStream();
                Workbook wbRuta = new XSSFWorkbook(isRuta)) {
                Sheet sRuta = wbRuta.getSheetAt(0);
                for (int i = 1; i <= sRuta.getLastRowNum(); i++) {
                    Row r = sRuta.getRow(i);
                    if (r == null) continue;
                    String placa = df.formatCellValue(r.getCell(0)).trim(); 
                    if (!placa.isEmpty()) {
                        infoConductores.put(placa, new String[]{
                            df.formatCellValue(r.getCell(4)).trim(), // Nombre
                            df.formatCellValue(r.getCell(5)).trim()  // Teléfono
                        });
                    }
                }
            }
        }

        try (InputStream isData = fileData.getInputStream();
            Workbook wbData = new XSSFWorkbook(isData)) {

            Sheet sheet = wbData.getSheet("RUTERO VENTAS");
            if (sheet == null) sheet = wbData.getSheetAt(2);

            LocalDateTime ahora = LocalDateTime.now();
            LocalDate hoy = LocalDate.now();

            Map<String, StringBuilder> acumuladorProductos = new HashMap<>();

            for (int i = 13; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String codClienteTxt = df.formatCellValue(row.getCell(1)).trim(); 
                String totalTxt = df.formatCellValue(row.getCell(15)).trim();    

                if (codClienteTxt.isEmpty()) continue;

                
                if (!codClienteTxt.toLowerCase().contains("total")) {
        
                    Cell cellProducto = row.getCell(10);
                    String nombreProducto = df.formatCellValue(cellProducto).trim();
                    String cantidadTxt = df.formatCellValue(row.getCell(13)).trim();
                    
                    if (cantidadTxt.isEmpty()) cantidadTxt = "1";
                    
                    if (!nombreProducto.isEmpty()) {
                        acumuladorProductos.putIfAbsent(codClienteTxt, new StringBuilder());
                        acumuladorProductos.get(codClienteTxt)
                            .append("- ").append(cantidadTxt).append("  ").append(nombreProducto).append("\\n");
                    }
                    
                }
            
                
                else {
                    String idReal = codClienteTxt.toLowerCase().replace("total", "").trim();
            
                    if (mapaPedidos.containsKey(idReal)) {
                        Pedido pedidoExistente = mapaPedidos.get(idReal);
                        pedidoExistente.setValorTotal(limpiarMoneda(totalTxt));
                        
                        
                        String misProductos = "";
                        if (acumuladorProductos.containsKey(idReal)) {
                            misProductos = acumuladorProductos.get(idReal).toString();
                            pedidoExistente.setListaProductos(misProductos);
                        }

                    }
                    continue; 
                }
                
                if (mapaPedidos.containsKey(codClienteTxt)) continue;
                String transporte = df.formatCellValue(row.getCell(0)).trim(); 
                String placa = df.formatCellValue(row.getCell(2)).trim();      
                String nombreEst = df.formatCellValue(row.getCell(6)).trim();  
                String telefonoCliente = extractClientPhone(row);
                Pedido p = pedidoRepository.findByNumeroEntrega(codClienteTxt)
                        .orElse(new Pedido());
                Cliente cliente = garantizarClienteConIdReal(codClienteTxt, nombreEst, telefonoCliente); 

                p.setCliente(cliente);
                if (p.getId() == null) {
                    p.setNumeroEntrega(codClienteTxt);
                    p.setFechaCarga(ahora);
                    p.setValorTotal(0.0); 
                }
                
                p.setNumeroTransporte(transporte); 
                p.setPlacaVehiculo(placa);
                p.setFechaEntrega(hoy);

                if (infoConductores.containsKey(placa)) {
                    p.setNombreConductor(infoConductores.get(placa)[0]);
                    p.setTelefonoConductor(infoConductores.get(placa)[1]);
                } else {
                    p.setNombreConductor("SIN ASIGNAR");
                    p.setTelefonoConductor("");
                }


                if (transportesEnPisoLimpios != null && transportesEnPisoLimpios.contains(transporte)) {
                    p.setEstadoLogistico(EstadoLogistico.EN_PISO);
                    System.out.println("📦 Marcado EN PISO: " + transporte);
                } else {
                    p.setEstadoLogistico(EstadoLogistico.ENTREGA_HOY);
                }
                mapaPedidos.put(codClienteTxt, p);
            }
        }

        if (!mapaPedidos.isEmpty()) {
            List<Pedido> pedidosAGuardar = new java.util.ArrayList<>(mapaPedidos.values());
            pedidoRepository.saveAll(pedidosAGuardar);
            System.out.println("✅ CARGA FINALIZADA: " + pedidosAGuardar.size() + " pedidos guardados.");
        }
    }


    private Cliente garantizarClienteConIdReal(String codigoTxt, String nombre, String telefono) {
        try {
            Long idReal = Long.parseLong(codigoTxt);
            
            return clienteRepository.findById(idReal)
                .map(existente -> {
                    
                    if (telefono != null && !telefono.isEmpty()) {
                        existente.setTelefono(telefono);
                        return clienteRepository.save(existente); 
                    }
                    return existente;
                })
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setId(idReal);
                    nuevo.setCodigoCliente(codigoTxt);
                    nuevo.setNombre(nombre);
                    nuevo.setTelefono(telefono);
                    return clienteRepository.save(nuevo);
                });
        } catch (NumberFormatException e) {
            return clienteRepository.findByCodigoCliente(codigoTxt).orElse(null);
        }
    }

    private Double limpiarMoneda(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            String limpio = val.replaceAll("[^0-9.,-]", "").trim();
            if (limpio.contains(",")) {
                limpio = limpio.replace(".", "").replace(",", ".");
            }
            return Double.parseDouble(limpio);
        } catch (Exception e) { return 0.0; }
    }

    private String extractClientPhone(Row row) {
        Cell cell = row.getCell(16); 
        if (cell == null) return null;
        DataFormatter formatter = new DataFormatter();
        String textoOriginal = formatter.formatCellValue(cell);
        String soloNumeros = textoOriginal.replaceAll("[^0-9]", "");
        if (soloNumeros.isEmpty()) return null;
        return soloNumeros;
    }
}