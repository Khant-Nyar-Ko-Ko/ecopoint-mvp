package com.ecopoint.app;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttConfig {
	
	@Value("${ecopoint.mqtt.broker}")
	private String broker;
	@Value("${ecopoint.mqtt.clientId}")
	private String clientId;
	@Value("${ecopoint.mqtt.username}")
	private String username;
	@Value("${ecopoint.mqtt.password}")
	private String password;
	@Value("${ecopoint.topics.subFilter}")
	private String subFilter;
	
	public static final String PUB_CHANNEL = "mqttOutboundChannel";
	public static final String SUB_CHANNEL = "mqttInboundChannel";
	
	
	@Bean
	public MqttConnectOptions mqttConnectOptions() {
	    var o = new MqttConnectOptions();
	    o.setServerURIs(new String[]{broker});
	    o.setAutomaticReconnect(false);
	    o.setCleanSession(true);
	    if (!username.isBlank()) o.setUserName(username);
	    if (!password.isBlank()) o.setPassword(password.toCharArray());
	    return o;
	 }
	
	@Bean
	public MqttPahoClientFactory mqttClientFactory() {
	    var f = new DefaultMqttPahoClientFactory();
	    f.setConnectionOptions(mqttConnectOptions());
	    return f;
	}
	
	
	@Bean(name = PUB_CHANNEL)
	public MessageChannel mqttOutboundChannel() { return new DirectChannel(); }

	@Bean
	@ServiceActivator(inputChannel = PUB_CHANNEL)
	  public MessageHandler mqttOutbound() {
	    var h = new MqttPahoMessageHandler(clientId + "-pub", mqttClientFactory());
	    h.setAsync(true);
	    h.setDefaultQos(1);
	    return h;
	  }

	  @Bean(name = SUB_CHANNEL)
	  public MessageChannel mqttInboundChannel() {
		  return new DirectChannel(); }

	  @Bean
	  public MessageProducer inboundAdapter() {
	    var a = new MqttPahoMessageDrivenChannelAdapter(
	        clientId + "-sub", mqttClientFactory(), subFilter);
	    a.setQos(1);
	    a.setConverter(new DefaultPahoMessageConverter());
	    a.setOutputChannel(mqttInboundChannel());
	    a.setCompletionTimeout(5000);
	    return a;
	  }
	


}
