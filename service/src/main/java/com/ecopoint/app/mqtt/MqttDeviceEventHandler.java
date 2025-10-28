package com.ecopoint.app.mqtt;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.ecopoint.app.model.entity.MachineSession;
import com.ecopoint.app.model.repo.MachineSessionRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MqttDeviceEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(MqttDeviceEventHandler.class);
    private final ObjectMapper om = new ObjectMapper();
    
    private final MachineSessionRepo sessionRepo;

    public MqttDeviceEventHandler(MachineSessionRepo sessionRepo) {
        this.sessionRepo = sessionRepo;
    }
    
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handle(Message<?> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = String.valueOf(message.getPayload());
        log.info("MQTT IN [{}] {}", topic, payload);

        // expects topic = ecopoint/{machineCode}/events
        String[] parts = topic.split("/");
        String machineCode = parts.length >= 2 ? parts[1] : null;

        try {
            Map<String, Object> evt = om.readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            String event = String.valueOf(evt.get("event"));

            switch (event) {
                case "session_started" -> {
                    log.info("Device ACK session start: {}", machineCode);
                }
                case "bottle_detected" -> {
                    log.info("Bottle detected from {}", machineCode);
                }
                case "session_closed" -> {
                    String reason = String.valueOf(evt.getOrDefault("reason", "device_end"));
                    sessionRepo.findFirstByMachineCodeAndStatus(machineCode, MachineSession.Status.ACTIVE)
                            .ifPresent(s -> {
                                s.setStatus(MachineSession.Status.CLOSED);
                                s.setClosedAt(java.time.LocalDateTime.now());
                                sessionRepo.save(s);
                                log.info("Closed by device [{}]: reason={}", machineCode, reason);
                            });
                }
                default -> log.debug("Unknown event: {}", event);
            }
        } catch (Exception e) {
            log.warn("Failed to parse MQTT payload: {}", e.getMessage());
        }
    }

}
