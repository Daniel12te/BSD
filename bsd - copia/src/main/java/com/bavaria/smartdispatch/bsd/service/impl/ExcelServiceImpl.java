package com.bavaria.smartdispatch.bsd.service.impl;

import com.bavaria.smartdispatch.bsd.entity.Cliente;
import com.bavaria.smartdispatch.bsd.entity.Pedido;
import com.bavaria.smartdispatch.bsd.entity.Enums.EstadoDelPedido;
import com.bavaria.smartdispatch.bsd.entity.Enums.EstadoLogistico;
import com.bavaria.smartdispatch.bsd.repository.ClienteRepository;
import com.bavaria.smartdispatch.bsd.repository.PedidoRepository;
import com.bavaria.smartdispatch.bsd.service.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExcelServiceImpl implements ExcelService {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;

    // --- INDICES ARCHIVO PRINCIPAL (DATA) ---
    private static final int COL_DATA_TRANSPORTE = 0;   // A
    private static final int COL_DATA_COD_CLIENTE = 1;  // B
    private static final int COL_DATA_NOM_CLIENTE = 2;  // C
    private static final int COL_DATA_ENTREGA = 8;      // I
    private static final int COL_DATA_TELEFONO = 13;    // N
    private static final int COL_DATA_PLACA = 15;       // P
    private static final int COL_DATA_VALOR = 23;       // X

    // --- INDICES ARCHIVO RUTA (DATOS RUTA) ---
    // Basado en tu archivo: A=Placa, B=DT, C=RR, D=TelRR, E=Cond, F=TelCond
    private static final int COL_RUTA_DT = 1;           // B (Transporte)
    private static final int COL_RUTA_NOM_COND = 4;     // E (Conductor)
    private static final int COL_RUTA_TEL_COND = 5;     // F (Tel Conductor)

    // Clase auxiliar simple para guardar datos del conductor temporalmente
    private static class DatosConductor {
        String nombre;
        String telefono;
        public DatosConductor(String n, String t) { this.nombre = n; this.telefono = t; }
    }

    @Override
    public void procesarArchivos(MultipartFile fileData, MultipartFile fileRuta, List<String> transportesEnPiso) {
        
        // 1. Preparar lista de PISO
        Set<String> setPiso = new HashSet<>();
        for (String t : transportesEnPiso) if(t != null && !t.isEmpty()) setPiso.add(t.trim());

        try {
            // 2. LEER ARCHIVO RUTA PRIMERO (Crear Mapa: DT -> InfoConductor)
            Map<String, DatosConductor> mapaConductores = procesarArchivoRuta(fileRuta);

            // 3. LEER ARCHIVO DATA Y CRUZAR
            procesarArchivoPedidos(fileData, setPiso, mapaConductores);

        } catch (IOException e) {
            throw new RuntimeException("Error procesando archivos: " + e.getMessage());
        }
    }

    // --- LOGICA LECTURA RUTA ---
    private Map<String, DatosConductor> procesarArchivoRuta(MultipartFile fileRuta) throws IOException {
        Map<String, DatosConductor> mapa = new HashMap<>();
        if (fileRuta == null || fileRuta.isEmpty()) return mapa;

        try (Workbook workbook = new XSSFWorkbook(fileRuta.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // Hoja "Datos"
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Saltar cabecera

                String dt = getCellString(row.getCell(COL_RUTA_DT)); // El ID de transporte
                if (dt.isEmpty()) continue;

                String nombre = getCellString(row.getCell(COL_RUTA_NOM_COND));
                String telefono = getCellString(row.getCell(COL_RUTA_TEL_COND));

                // Limpiar telefono conductor
                telefono = telefono.replaceAll("[^0-9]", "");
                
                mapa.put(dt, new DatosConductor(nombre, telefono));
            }
        }
        return mapa;
    }

    // --- LOGICA LECTURA DATA (PEDIDOS) ---
    private void procesarArchivoPedidos(MultipartFile fileData, Set<String> setPiso, Map<String, DatosConductor> mapaConductores) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(fileData.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Pedido> pedidosMap = new HashMap<>();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; 

                String transporte = getCellString(row.getCell(COL_DATA_TRANSPORTE));
                String entrega = getCellString(row.getCell(COL_DATA_ENTREGA)); 

                if (transporte.isEmpty() || entrega.isEmpty()) continue;

                EstadoLogistico estado = setPiso.contains(transporte) ? EstadoLogistico.EN_PISO : EstadoLogistico.ENTREGA_HOY;
                Double valorFila = getCellNumeric(row.getCell(COL_DATA_VALOR));

                // Buscamos info del conductor en el mapa que creamos antes
                DatosConductor infoCond = mapaConductores.getOrDefault(transporte, new DatosConductor("Sin Asignar", ""));

                if (pedidosMap.containsKey(entrega)) {
                    Pedido p = pedidosMap.get(entrega);
                    p.setValorTotal(p.getValorTotal() + valorFila);
                    p.setEstadoLogistico(estado); 
                    // Refrescamos conductor por si acaso
                    p.setNombreConductor(infoCond.nombre);
                    p.setTelefonoConductor(infoCond.telefono);
                } else {
                    Pedido nuevo = crearPedido(row, transporte, entrega, estado, valorFila, infoCond);
                    pedidosMap.put(entrega, nuevo);
                }
            }
            guardarEnBD(pedidosMap.values());
        }
    }

    private Pedido crearPedido(Row row, String transporte, String entrega, EstadoLogistico estado, Double valor, DatosConductor infoCond) {
        Pedido p = new Pedido();
        p.setNumeroTransporte(transporte);
        p.setNumeroEntrega(entrega);
        p.setEstadoLogistico(estado);
        p.setValorTotal(valor);
        p.setEstadoConfirmacion(EstadoDelPedido.PENDIENTE_NOTIFICACION);
        p.setFechaCarga(LocalDateTime.now());
        p.setFechaEntrega(LocalDate.now().plusDays(1));
        p.setPlacaVehiculo(getCellString(row.getCell(COL_DATA_PLACA)));
        
        // ASIGNAR CONDUCTOR
        p.setNombreConductor(infoCond.nombre);
        p.setTelefonoConductor(infoCond.telefono);

        // CLIENTE
        String codCli = getCellString(row.getCell(COL_DATA_COD_CLIENTE));
        String nomCli = getCellString(row.getCell(COL_DATA_NOM_CLIENTE));
        String telCli = getCellString(row.getCell(COL_DATA_TELEFONO)).replaceAll("[^0-9]", "");
        if (telCli.length() > 10) telCli = telCli.substring(telCli.length() - 10);

        final String telefonoFinal = telCli;
        Cliente cliente = clienteRepository.findByCodigoCliente(codCli).orElseGet(() -> {
            Cliente c = new Cliente();
            c.setCodigoCliente(codCli);
            c.setNombre(nomCli);
            c.setTelefono(telefonoFinal.isEmpty() ? "0000000000" : telefonoFinal);
            return clienteRepository.save(c);
        });
        
        // Actualizar teléfono cliente si llega uno nuevo mejor
        if(!telefonoFinal.isEmpty() && (cliente.getTelefono().equals("0000000000") || cliente.getTelefono().isEmpty())) {
            cliente.setTelefono(telefonoFinal);
            clienteRepository.save(cliente);
        }

        p.setCliente(cliente);
        return p;
    }

    private void guardarEnBD(Collection<Pedido> pedidos) {
        for (Pedido p : pedidos) {
            Optional<Pedido> existe = pedidoRepository.findByNumeroEntrega(p.getNumeroEntrega());
            if (existe.isPresent()) {
                Pedido update = existe.get();
                update.setValorTotal(p.getValorTotal());
                update.setEstadoLogistico(p.getEstadoLogistico());
                update.setNumeroTransporte(p.getNumeroTransporte());
                update.setNombreConductor(p.getNombreConductor());
                update.setTelefonoConductor(p.getTelefonoConductor());
                update.setFechaCarga(LocalDateTime.now());
                pedidoRepository.save(update);
            } else {
                pedidoRepository.save(p);
            }
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        try {
            return cell.getCellType() == CellType.NUMERIC ? String.format("%.0f", cell.getNumericCellValue()) : cell.getStringCellValue().trim();
        } catch (Exception e) { return ""; }
    }
    private Double getCellNumeric(Cell cell) {
        if (cell == null) return 0.0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
            String val = cell.getStringCellValue().replace(",", ".");
            return Double.parseDouble(val);
        } catch (Exception e) { return 0.0; }
    }
}