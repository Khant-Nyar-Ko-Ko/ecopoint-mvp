package com.ecopoint.app.mqtt;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class DeviceCommandPublisher {
	
	 	private final MessageChannel mqttOutboundChannel;

	    public DeviceCommandPublisher(MessageChannel mqttOutboundChannel) {
	        this.mqttOutboundChannel = mqttOutboundChannel;
	    }

	    public void startSession(String machineCode, String sessionId, Long userId) {
	        String topic = "ecopoint/" + machineCode + "/cmd";
	        String payload = """
	            {"type":"start_session","sessionId":"%s","userId":%d}
	        """.formatted(sessionId, userId);
	        send(topic, payload);
	    }

	    public void endSession(String machineCode) {
	        String topic = "ecopoint/" + machineCode + "/cmd";
	        send(topic, "{\"type\":\"end_session\"}");
	    }

	    public void ping(String machineCode) {
	        String topic = "ecopoint/" + machineCode + "/cmd";
	        send(topic, "{\"type\":\"ping\"}");
	    }

	    private void send(String topic, String payload) {
	        Message<String> msg = MessageBuilder.withPayload(payload)
	                .setHeader("mqtt_topic", topic)
	                .setHeader("mqtt_qos", 1)
	                .build();
	        mqttOutboundChannel.send(msg);
	    }

}
