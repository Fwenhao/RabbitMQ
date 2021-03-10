package cn.fyyice.springbootconsumer.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Component
public class RabbitMQListener {

    @RabbitListener(queues = "my_queue")
    public void ListenerQueue(Message message) throws UnsupportedEncodingException {
        System.out.println("接收到消息"+new String(message.getBody(),"utf-8"));
    }
}
