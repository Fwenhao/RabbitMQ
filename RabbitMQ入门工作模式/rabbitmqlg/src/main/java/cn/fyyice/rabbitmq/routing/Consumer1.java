package cn.fyyice.rabbitmq.routing;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer1 {

    private final static String exchangeName = "RoutingDistribute";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        connection = RabbitMQUtil.getConnection();
        // 获取连接中通道
        channel=connection.createChannel();
        //创建交换机
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT,true,false,false,null);
        // 创建队列
        String queneName= "Center_Of_Error";
        channel.queueDeclare(queneName,true,false,false,null);
        // 绑定交换机和队列
        channel.queueBind(queneName,exchangeName,"error");
        // 消费信息
        channel.basicConsume(queneName,true,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("Consumer1 已接收到消息类型 --------- "+new String(body));
            }
        });
    }
}
