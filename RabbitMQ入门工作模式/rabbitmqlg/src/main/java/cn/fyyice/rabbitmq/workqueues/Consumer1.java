package cn.fyyice.rabbitmq.workqueues;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer1 {
    private final static String QueueName = "Work_Queues";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        //获取连接
        connection = RabbitMQUtil.getConnection();
        //获取连接通道
        channel = connection.createChannel();
        //取得对应管道通信
        channel.queueDeclare(QueueName,true,false,false,null);
        //获取消息

        channel.basicConsume(QueueName,false,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println(new String(body));
            }
        });

    }
}
