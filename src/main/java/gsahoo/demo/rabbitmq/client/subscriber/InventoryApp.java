package gsahoo.demo.rabbitmq.client.subscriber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

import gsahoo.demo.rabbitmq.client.RabbitConfig;

@Component
public class InventoryApp {
	private static final Logger LOGGER = LoggerFactory.getLogger(InventoryApp.class);
	private static final long MAX_RETRY_COUNT = 3;

	@Autowired
	private final RabbitTemplate rabbitTemplate;

	@Autowired
	private RabbitConfig rabbitConfig;
	@Autowired
	RabbitListenerEndpointRegistry registry;
	List<String> messages = Collections.synchronizedList(new ArrayList<String>());

	@Autowired
	public InventoryApp(RabbitTemplate rabbitTemplate) {
		super();
		this.rabbitTemplate = rabbitTemplate;
		// this.confirmCallback = confirmCallback;
		// this.admin = admin;
	}

	

	public List<String> getMessages() {
		return messages;
	}



	public void setMessages(List<String> messages) {
		this.messages = messages;
	}



	@RabbitListener(id = "customq", autoStartup = "false", containerFactory = "rabbitListenerContainerFactoryCustom")
	public void processMessage(Message message, Channel channel) {
		long deliverytag = 0;
		try {
			int num = channel.getChannelNumber();

			boolean isadded = this.messages.add("CHANNEL-NUM:" + num + " message:" + message.toString());
			MessageProperties properties = message.getMessageProperties();
			deliverytag = properties.getDeliveryTag();
			String messageId = properties.getMessageId();

			System.out.println("messageId:" + messageId + " isadded:" + isadded);
			System.out.println("message: \n" + message);

			Map<String, Object> header = properties.getHeaders();
			String type = (String) header.get("order-type");

			processmessage(message, type);

			channel.basicAck(deliverytag, false);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			sendNack(message, channel, deliverytag);
		}

	}

	private void sendNack(Message message, Channel channel, long deliverytag) {
		try {
			boolean ismaxTryreached = false;
			MessageProperties props = message.getMessageProperties();
			List<Map<String, ?>> headers = props.getXDeathHeader();// getHeaders();
		
			if (headers != null)
				for (Map<String, ?> m : headers) {
					System.out.println(m);
					if (((String) m.get("queue")).equalsIgnoreCase(rabbitConfig.getOrderQ())) {
						long c = ((Long) m.get("count")).longValue();

						System.out.println("count is:" + c + " queue is:" + m.get("queue"));
						if (c == MAX_RETRY_COUNT)
							ismaxTryreached = true;
					}
				}

			if (ismaxTryreached) {
				channel.basicAck(deliverytag, false);
				CorrelationData correlationData = new CorrelationData("error");
				rabbitTemplate.send(RabbitConfig.EXCHANGE_ERROR, RabbitConfig.RTK_ERROR_TOPIC, message,
						correlationData);
			} else
				channel.basicNack(deliverytag, false, false);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private void processmessage(Message message, String type) throws InterruptedException {
		if (type.equalsIgnoreCase("mouse")) {
			// simulate succesful processing
			Thread.sleep(2000);
		} else {
			// simulate fail processing
			Thread.sleep(2000);
			throw new RuntimeException("No Stock Available Error");
		}

	}

	public void registerCustomQtoListner() throws InterruptedException {
		AbstractMessageListenerContainer listenerContainer = (AbstractMessageListenerContainer) registry
				.getListenerContainer("customq");

		// for (MessageListenerContainer listenerContainer :messageListenerContainers) {
		if (!(listenerContainer.getQueueNames().length > 0))
			listenerContainer.addQueueNames(rabbitConfig.getOrderQ());
		System.out.println(listenerContainer);
		if (!listenerContainer.isRunning())
			listenerContainer.start();
		Thread.sleep(100);

	}

	public void stopCustomQtoListner() {
		AbstractMessageListenerContainer listenerContainer = (AbstractMessageListenerContainer) registry
				.getListenerContainer("customq");

		// for (MessageListenerContainer listenerContainer :messageListenerContainers) {
		// listenerContainer.addQueueNames(rabbitConfig.getRmqcustomQ());
		System.out.println(listenerContainer);
		if (listenerContainer.isRunning())
			listenerContainer.stop();

	}

}
