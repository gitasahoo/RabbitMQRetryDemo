																																																										package gsahoo.demo.rabbitmq.client;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.amqp.core.*;
import org.springframework.amqp.core.BindingBuilder.HeadersExchangeMapConfigurer.HeadersExchangeKeysBindingCreator;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.ErrorHandler;

//import com.rabbitmq.client.ConnectionFactory;

/*
 * AMQP default
 * spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.initial-interval=2000
spring.rabbitmq.listener.simple.retry.max-attempts=5
spring.rabbitmq.listener.simple.retry.multiplier=2
spring.rabbitmq.listener.simple.max-concurrency= same as concurrency
spring.rabbitmq.listener.simple.concurrency=1
DEFAULT_PREFETCH_COUNT	250
DEFAULT_RECEIVE_TIMEOUT	1000L
DEFAULT_FRAME_MAX_HEADROOM	20000
DEFAULT_RECOVERY_INTERVAL	5000L
DEFAULT_SHUTDOWN_TIMEOUT	5000L

cannot set both publisherconfirmation and simplePublisherConfirmation
 */
@Configuration
public class RabbitConfig implements RabbitListenerConfigurer {

	public static final String EXCHANGE_ORDER = "order-exchange";
	public static final String EXCHANGE_RETRY = "retry-exchange";
	public static final String EXCHANGE_ERROR = "error-exchange";
	
	public static final String RTK_TOPIC = "order*";
	public static final String RTK_RETRY_TOPIC = "retry*";
	public static final String RTK_ERROR_TOPIC="error";
	
	@Value("${rmq.host}")
	private String rmqhost;
	
	@Value("${rmq.port}")
    private String rmqport;
	@Value("${rmq.username}")
    private String rmqusername;
	@Value("${rmq.password}")
    private String rmqpassword;
	@Value("${rmq.vhost}")
    private String rmqvhost;
	@Value("${rmq.protocol}")
    private String rmqprotocol;
	
	
	@Value("${rmq.order.queue.name}")
    private String orderQ;
	
	@Value("${rmq.retry.queue.name}")
    private String retryQ;
	
	@Value("${rmq.error.queue.name}")
    private String errorQ;
	@Value("${rmq.custom.consumer.count}")
    private String rmqCustomQconsumerCount;
	

	
	public String getOrderQ() {
		return orderQ;
	}
	public void setOrderQ(String orderQ) {
		this.orderQ = orderQ;
	}
	public String getRetryQ() {
		return retryQ;
	}
	public void setRetryQ(String retryQ) {
		this.retryQ = retryQ;
	}
	public String getErrorQ() {
		return errorQ;
	}
	public void setErrorQ(String errorQ) {
		this.errorQ = errorQ;
	}
	public String getRmqCustomQconsumerCount() {
		return rmqCustomQconsumerCount;
	}
	public void setRmqCustomQconsumerCount(String rmqCustomQconsumerCount) {
		this.rmqCustomQconsumerCount = rmqCustomQconsumerCount;
	}

	@Bean
	Queue autoOrderQueue() {

		return QueueBuilder.durable(orderQ).withArgument("x-dead-letter-exchange", EXCHANGE_RETRY)
				.withArgument("x-dead-letter-routing-key", RTK_RETRY_TOPIC).build();
	}
	@Bean
	Queue autoRetryQueue() {

		return QueueBuilder.durable(retryQ).withArgument("x-message-ttl", 4000).withArgument("x-dead-letter-exchange", EXCHANGE_ORDER)
				.withArgument("x-dead-letter-routing-key", RTK_TOPIC)			
				.build();
	}
	@Bean
	Queue autoErrorQueue() {

		return QueueBuilder.durable(errorQ)		
				.build();
	}
	@Bean
	Exchange autoOrderExchange() {
		return ExchangeBuilder.topicExchange(EXCHANGE_ORDER).build();
	}
	@Bean
	Exchange autoRetryExchange() {
		return ExchangeBuilder.topicExchange(EXCHANGE_RETRY).build();
	}
	@Bean
	Exchange autoErrorExchange() {
		return ExchangeBuilder.topicExchange(EXCHANGE_ERROR).build();
	}
	
	@Bean
	Binding binding(Queue autoOrderQueue, TopicExchange autoOrderExchange) {
		return BindingBuilder.bind(autoOrderQueue).to(autoOrderExchange).with(RTK_TOPIC);
	}
	@Bean
	Binding bindingretry(Queue autoRetryQueue, TopicExchange autoRetryExchange) {
		return BindingBuilder.bind(autoRetryQueue).to(autoRetryExchange).with(RTK_RETRY_TOPIC);
	}
	@Bean
	Binding bindingerror(Queue autoErrorQueue, TopicExchange autoErrorExchange) {
		return BindingBuilder.bind(autoErrorQueue).to(autoErrorExchange).with(RTK_ERROR_TOPIC);
	}
	
	
	@Bean
	org.springframework.amqp.rabbit.core.RabbitAdmin   RabbitAdmin(final ConnectionFactory connectionFactory) {
		final org.springframework.amqp.rabbit.core.RabbitAdmin rabbitadmin = new org.springframework.amqp.rabbit.core.RabbitAdmin(connectionFactory);
		return rabbitadmin;
	}
	
	
	@Primary
	@Bean
	public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
		try {
			final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
			rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());

			//rabbitTemplate.setConfirmCallback(confirmCallback());
		//	rabbitTemplate.setReturnCallback(confirmCallback());
			
		//	rabbitTemplate.setMandatory(true);

			CachingConnectionFactory connectionFactory2 = (CachingConnectionFactory) rabbitTemplate
					.getConnectionFactory();

			System.out.println("cachemode:" + connectionFactory2.getCacheMode());
			System.out.println("Default close time out:" + CachingConnectionFactory.DEFAULT_CLOSE_TIMEOUT
					+ "channel cache size:" + connectionFactory2.getChannelCacheSize());
			Properties p = connectionFactory2.getCacheProperties();
			Enumeration<?> keys = p.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				String value = (String) p.get(key);
				System.out.println(key + ": " + value);
			}
		
			System.out.println("routingKey" + rabbitTemplate.getRoutingKey());
			//System.out.println("isreturnListener:" + rabbitTemplate.isReturnListener());
			//System.out.println("isConfirmListener-" + rabbitTemplate.isConfirmListener());
			return rabbitTemplate;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Bean
	public ConnectionFactory connectionFactory() throws KeyManagementException, NoSuchAlgorithmException {
		com.rabbitmq.client.ConnectionFactory connectionFactory = new com.rabbitmq.client.ConnectionFactory();
		/*
		 * connectionFactory.setHost("paas-rmq-som-sit02.us.dell.com");
		 * connectionFactory.setPort(8071);
		 * connectionFactory.setUsername("paas-rmq-demo");
		 * connectionFactory.setPassword("paas-rmq-demo");
		 * connectionFactory.setVirtualHost("paas-rmq-demo");
		 */
		System.out.println(rmqhost);
		System.out.println(rmqport);
		connectionFactory.setHost(rmqhost);
		connectionFactory.setPort(Integer.parseInt(rmqport.trim()));
		connectionFactory.setUsername(rmqusername);
		connectionFactory.setPassword(rmqpassword);
		connectionFactory.setVirtualHost(rmqvhost);
		connectionFactory.setConnectionTimeout(0);
		
		if(rmqprotocol.trim().equalsIgnoreCase("amqps"))
			connectionFactory.useSslProtocol("TLSV1.2");

	
		
		/*
		TrustManager trustAllCerts = 
			new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		 };
		*/
		
	
		System.out.println("connectiontimeout:" + connectionFactory.getConnectionTimeout());
		Map<String, Object> m1 = connectionFactory.getClientProperties();
		System.out.println("printing connection factory BasicClientProperties");
		for (String key : m1.keySet()) {
			System.out.println(key + " = " + m1.get(key));
		}

		CachingConnectionFactory cc = new CachingConnectionFactory(connectionFactory);
		cc.setChannelCacheSize(40);
		//cc.setCacheMode(CacheMode.CONNECTION);
		cc.setCacheMode(CacheMode.CHANNEL);
		// cc.setConnectionCacheSize(20);
		cc.setConnectionLimit(500);
		cc.setChannelCheckoutTimeout(1000L);

		 cc.setPublisherConfirms(true);
		 cc.setPublisherReturns(true);

		cc.setConnectionNameStrategy(f -> "arabbitmq-demo-publisher-subscriber");
		return cc;
	}

	

	
	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactoryCustom()
			throws KeyManagementException, NoSuchAlgorithmException {
		SimpleRabbitListenerContainerFactory listenerContainerFcatory = new SimpleRabbitListenerContainerFactory();
		
		System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ inside  rabbitListenerContainerFactory");
		listenerContainerFcatory.setConnectionFactory(connectionFactory());
		String s= this.rmqCustomQconsumerCount;
		int ccount=0;
		if (null != s)
				ccount= Integer.valueOf(s);
		listenerContainerFcatory.setConcurrentConsumers(ccount);
		listenerContainerFcatory.setMaxConcurrentConsumers(10);
		listenerContainerFcatory.setPrefetchCount(4);
		listenerContainerFcatory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		return listenerContainerFcatory;
	}

	

	@Bean
	public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	MessageHandlerMethodFactory messageHandlerMethodFactory() {
		DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
		messageHandlerMethodFactory.setMessageConverter(consumerJackson2MessageConverter());
		return messageHandlerMethodFactory;
	}

	@Override
	public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
		registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
	}

	@Bean
	public MappingJackson2MessageConverter consumerJackson2MessageConverter() {
		return new MappingJackson2MessageConverter();
	}
	/*
	@Bean
	public ConfirmCallbackBasicImplementation confirmCallback() {
		return new ConfirmCallbackBasicImplementation();
	}
	
	public static class ConfirmCallbackBasicImplementation implements ConfirmCallback, ReturnCallback {

		private volatile Map<String, Boolean> confirmations = new HashMap<>();

		private volatile HashMap<String, CountDownLatch> expectationLatches = new HashMap<>();

		
		@Override
		public void confirm(CorrelationData correlationData, boolean ack, String cause) {
			System.out.println("inside confirm:correlationData.getId():" + correlationData.getId());
			System.out.println("inside confirm:success:" + ack);
			System.out.println("inside confirm:string:" + cause);
			if (!(confirmations.containsKey(correlationData.getId())))
			confirmations.put(correlationData.getId(), ack);
			//expectationLatches.get(correlationData.getId()).countDown();
		}

		public CountDownLatch expect(String correlationId) {
			//System.out.println("inside expect:correlationId:" + correlationId);
			CountDownLatch latch = new CountDownLatch(1);
			this.expectationLatches.put(correlationId, latch);
			return latch;
		}
		public int getLatchCount() {
			return this.expectationLatches.size();
		}
		public int getConfirmationCount() {
			return this.confirmations.size();
		}
		public Map<String, Boolean> getConfirmationMap() {
			return this.confirmations;
		}

		
		

		@Override
		public void returnedMessage(Message message, int replyCode, String replyText, String exchange,
				String routingKey) {
			System.out.println("================");
			System.out.println("message = " + message);
			System.out.println("replyCode = " + replyCode);
			System.out.println("replyText = " + replyText);
			System.out.println("exchange = " + exchange);
			System.out.println("routingKey = " + routingKey);
			
			 Map<String, Object> headers = message.getMessageProperties().getHeaders();
			 String correlationid = (String)headers.get("spring_returned_message_correlation");
	            for (Map.Entry<String, Object> header : headers.entrySet())
	            {
	                System.out.println(header.getKey() + " : " + header.getValue());

	            }
			System.out.println("================");
			confirmations.size();
            confirmations.put(correlationid, null);
			// confirmations.put(correlationData.getId(), ack);
			// expectationLatches.get(correlationData.getId()).countDown();

		}

	}
*/
	/*
	public void createSSLContext(){
		SSLContext sslcontext = SSLContext.getInstance( "TLS" );
		sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
		  public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
		  public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
		  public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }

		}}, new java.security.SecureRandom());

		Client client = ClientBuilder.newBuilder().sslContext(sslcontext).hostnameVerifier(new HostnameVerifier() {
		  @Override
		  public boolean verify(String requestedHost, SSLSession remoteServerSession) {
		    return true;  // Noncompliant
		  }
		}).build();
	}
*/

}
