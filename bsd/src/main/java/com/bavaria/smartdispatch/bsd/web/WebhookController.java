package com.bavaria.smartdispatch.bsd.web;

import com.bavaria.smartdispatch.bsd.entity.Pedido;
import com.bavaria.smartdispatch.bsd.repository.PedidoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/webhook")
@CrossOrigin(origins = "*")
public class WebhookController {

    @Autowired
    private PedidoRepository pedidoRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

 
    @GetMapping
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        
        
        String miTokenSecreto = "BAVARIA_SECRET_2026"; 
        
        if ("subscribe".equals(mode) && miTokenSecreto.equals(token)) {
            System.out.println("✅ WEBHOOK VERIFICADO EXITOSAMENTE");
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping
    public ResponseEntity<Void> recibirRespuesta(@RequestBody String strJson) {
        try {
            JsonNode node = objectMapper.readTree(strJson);
            
            System.out.println("📩 JSON RECIBIDO: " + strJson);

            JsonNode message = node.at("/entry/0/changes/0/value/messages/0");

            if (!message.isMissingNode() && message.has("button")) {
                String respuestaCliente = message.at("/button/text").asText(); 
                String telefonoFull = message.at("/from").asText(); 
                
            
                String celular10Digitos = telefonoFull.substring(telefonoFull.length() - 10);

                System.out.println("📱 Respuesta de: " + celular10Digitos + " -> " + respuestaCliente);

                
                List<Pedido> pedidos = pedidoRepository.findAll(); 
                Pedido ultimoPedido = pedidos.stream()
                    .filter(p -> p.getCliente() != null && p.getCliente().getTelefono() != null)
                    .filter(p -> p.getCliente().getTelefono().contains(celular10Digitos))
                    .reduce((first, second) -> second) // El más reciente
                    .orElse(null);

                if (ultimoPedido != null) {
                    if (respuestaCliente.toUpperCase().contains("SI")) {
                        ultimoPedido.setEstadoNotificacion("CONFIRMADO");
                    } else {
                        ultimoPedido.setEstadoNotificacion("RECHAZADO");
                    }
                    pedidoRepository.save(ultimoPedido);
                    System.out.println("✅ BD ACTUALIZADA: " + ultimoPedido.getEstadoNotificacion() + " para " + celular10Digitos);
                } else {
                    System.out.println("⚠️ No se encontró pedido para el celular: " + celular10Digitos);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR WEBHOOK: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    
    }
}