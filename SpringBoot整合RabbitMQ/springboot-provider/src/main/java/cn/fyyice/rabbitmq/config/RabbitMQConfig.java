package cn.fyyice.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "my_topic_exchange";
    public static final String QUEUE_NAME = "my_queue";

    //交换机
    @Bean("myExchange")
    public Exchange myExchange(){
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }
    //队列
    @Bean("myQueue")
    public Queue myQueue(){
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    /**
     *  队列和交换机相互绑定
     *  with(routingkey)
     *  noargs 指定参数
     */
    @Bean
    public Binding bindingQueueExchange(@Qualifier("myQueue") Queue queue,@Qualifier("myExchange") Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with("boot.#").noargs();
    }
}
