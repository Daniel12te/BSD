package com.bavaria.smartdispatch.bsd.web;

import com.bavaria.smartdispatch.bsd.entity.Cliente;
import com.bavaria.smartdispatch.bsd.entity.Pedido;
import com.bavaria.smartdispatch.bsd.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "*")
public class NotificacionController {


    @Autowired
    private PedidoRepository pedidoRepository;


    private static final String PHONE_NUMBER_ID = "930314530174401"; 
    private static final String META_TOKEN = "EAAWoXewFX1QBQzaHVtCk8ZCLDe44TpIGVGVHL6UnTTmQSHZBwURReb8TyIvnihdZA9XbqmhaYlhFYnCidz2D1ZCFndSpaV1h6wrhvXdwhA2tVyQ98cidsjBiX0SNtNTIWNv9jY9z2HUcElsZAvn9dYkKRXvhbAY4bpv3jr7zARochGpscQuOhtyLWy7BqMa8qjZBFT4ZCXO6H2bmeLT60qF6AlYaVMq0R1QhhrZCBsGiqsFFhPtm7PZCqFWTgm6h5hD5pPkfQUNqeERZBm4v5VaG7h8aiT";
    private static final String META_URL = "https://graph.facebook.com/v18.0/" + PHONE_NUMBER_ID + "/messages";
   
    @PostMapping("/enviar")
    public ResponseEntity<String> enviarMasivo(@RequestBody List<Long> ids) {
        int exitosos = 0;
        int fallidos = 0;

        for (Long id : ids) {
            if (id == null) continue;
            Optional<Pedido> optPedido = pedidoRepository.findById(id);
            
            if (optPedido.isPresent()) {
                Pedido pedido = optPedido.get();
                Cliente cliente = pedido.getCliente();
                
                if (cliente != null && cliente.getTelefono() != null && !cliente.getTelefono().isEmpty()) {
                    
                    String valorFormateado = String.format("$ %,.0f", pedido.getValorTotal());
                    boolean enviado = false;

                
                    String estadoLogistico = pedido.getEstadoLogistico() != null ? pedido.getEstadoLogistico().name() : "";

                    if ("EN_PISO".equalsIgnoreCase(estadoLogistico)) {
                        // 📦 Si se quedó en piso, mandamos la REPROGRAMACIÓN
                        enviado = enviarReprogramacion(
                            cliente.getTelefono(),
                            cliente.getNombre(),
                            valorFormateado
                        );
                    } else {
                    
                        enviado = enviarNotificacionDespacho(
                            cliente.getTelefono(),
                            cliente.getNombre(),
                            pedido.getPlacaVehiculo(),
                            pedido.getNombreConductor(),
                            pedido.getTelefonoConductor() != null ? pedido.getTelefonoConductor() : "N/A",
                            valorFormateado,
                            pedido.getListaProductos() != null ? pedido.getListaProductos() : "Productos"
                        );
                    }

                    if (enviado) {
                        pedido.setEstadoNotificacion("ENVIADO");
                        exitosos++;
                    } else {
                        pedido.setEstadoNotificacion("PENDIENTE");
                        fallidos++;
                    }
                    pedidoRepository.save(pedido);
                    
                } else {
                    pedido.setEstadoNotificacion("PENDIENTE");
                    pedidoRepository.save(pedido);
                    fallidos++; 
                }
            }
        }

        return ResponseEntity.ok("Mensajes procesados | Exitosos: " + exitosos + " | Fallidos/Sin celular: " + fallidos);
    }


    public boolean enviarNotificacionDespacho(
            String telefono, String nombreCliente, String placa, 
            String conductor, String celConductor, String valor, String listaProductos
    ) {
        try {
            if (telefono == null || telefono.length() < 10) return false;
            if (!telefono.startsWith("57")) telefono = "57" + telefono;

            System.out.println("📡 Enviando DESPACHO a: " + telefono);

            URL url = URI.create(META_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + META_TOKEN);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonBody = "{"
                    + "\"messaging_product\": \"whatsapp\","
                    + "\"to\": \"" + telefono + "\","
                    + "\"type\": \"template\","
                    + "\"template\": {"
                    + "    \"name\": \"despacho_pedido\","
                    + "    \"language\": { \"code\": \"es_CO\" },"
                    + "    \"components\": ["
                    
                    // 1️⃣ PARTE DEL ENCABEZADO
                    + "        {"
                    + "            \"type\": \"header\","
                    + "            \"parameters\": ["
                    + "                { \"type\": \"text\", \"text\": \"" + limpiarTexto(nombreCliente) + "\" }"
                    + "            ]"
                    + "        },"
                    
                    // 2️⃣ PARTE DEL CUERPO
                    + "        {"
                    + "            \"type\": \"body\","
                    + "            \"parameters\": ["
                    + "                { \"type\": \"text\", \"text\": \"" + limpiarTexto(placa) + "\" },"
                    + "                { \"type\": \"text\", \"text\": \"" + limpiarTexto(conductor) + "\" },"
                    + "                { \"type\": \"text\", \"text\": \"" + limpiarTexto(celConductor) + "\" },"
                    + "                { \"type\": \"text\", \"text\": \"" + limpiarTexto(valor) + "\" },"
                    + "                { \"type\": \"text\", \"text\": \"" + limpiarTexto(listaProductos) + "\" }"
                    + "            ]"
                    + "        }"
                    
                    + "    ]"
                    + "}"
                    + "}";

            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                System.out.println("✅ ÉXITO: Despacho entregado a " + nombreCliente);
                return true;
            } else {
                System.err.println("❌ FALLO META DESPACHO (Código " + responseCode + ")");
                try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) System.err.println(line); 
                }
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean enviarReprogramacion(String telefono, String nombreCliente, String valor) {
        try {
            if (telefono == null || telefono.length() < 10) return false;
            if (!telefono.startsWith("57")) telefono = "57" + telefono;


            URL url = java.net.URI.create(META_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + META_TOKEN);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonBody = "{"
                    + "\"messaging_product\": \"whatsapp\","
                    + "\"to\": \"" + telefono + "\","
                    + "\"type\": \"template\","
                    + "\"template\": {"
                    + "    \"name\": \"reprogramacion_pedido\","
                    + "    \"language\": { \"code\": \"es\" },"
                    + "    \"components\": ["
                    + "        {"
                    + "            \"type\": \"body\","
                    + "            \"parameters\": ["
                    + "                { \"type\": \"text\", \"text\": \"" + limpiarTexto(nombreCliente) + "\" },"
                    + "                { \"type\": \"text\", \"text\": \"" + limpiarTexto(valor) + "\" }"
                    + "            ]"
                    + "        }"
                    + "    ]"
                    + "}"
                    + "}";

            try(java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return true;
            } else {
                
                try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) System.err.println(line); 
                }
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private String limpiarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "N/A";
        }
        
        texto = texto.replace("\\n", ", ").replace("\\t", " ");
        texto = texto.replaceAll("\\s+", " ");
        texto = texto.replace("\"", "'");
        
        if (texto.length() > 900) {
            texto = texto.substring(0, 900) + "...";
        }
        
        return texto.trim();
    }
}