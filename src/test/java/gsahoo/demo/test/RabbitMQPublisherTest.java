package gsahoo.demo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import gsahoo.demo.rabbitmq.client.RabbitConfig;
import gsahoo.demo.rabbitmq.client.RabbitConfig.ConfirmCallbackBasicImplementation;
import gsahoo.demo.rabbitmq.client.dto.Order;
import gsahoo.demo.rabbitmq.client.publisher.OrderApp;

@RunWith(SpringRunner.class)
@PropertySource("classpath:application.properties")
@ContextConfiguration(classes = RabbitConfig.class)
//@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@Configuration
@Import({OrderApp.class, RabbitConfig.class})
//@WebAppConfiguration
@ActiveProfiles("test")

public class RabbitMQPublisherTest {

	
		//@Autowired
	
		OrderApp publisher;
		
		@Mock
		Order order;
		/*
		@Mock
		RabbitTemplate rabbitTemplate;
		
		@Mock
		RabbitTemplate rabbitTemplate;
		
		@Mock
		ConfirmCallbackBasicImplementation confirmCallback;
		@Before
		public void setup() {
			publisher = new SWATPublisher();
			ReflectionTestUtils.setField(rabbitTemplate, rmq.port, 8071);
		}
		*/
		/*
		@Test
		public void testGetConfirmationCount() {
			
			Order s1 = mock(Order.class);
			s1.setOrderNumber("111");
			int count =0;
			
			
		//	when(publisher.getConfirmationCount()).thenReturn(count);

			// test
		//	publisher.resetConfirmationMap();
			try {
				publisher.sendOrder(order, 1, 4);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			count= publisher.getConfirmationCount();
			System.out.println("************************"+publisher.getConfirmationMap());
			assertNotNull(count);
					
		}
		*/
		@Test
		public void testsendOrder() {
		}

			
		
		@Test
		public void testsendOrderRejctPub() {
			
			
			
		//	when(publisher.getConfirmationCount()).thenReturn(count);
		}
			
}
