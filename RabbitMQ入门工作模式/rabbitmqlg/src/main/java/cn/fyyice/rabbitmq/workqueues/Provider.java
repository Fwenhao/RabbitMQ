package cn.fyyice.rabbitmq.workqueues;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class Provider {

    private final static String QueueName = "Work_Queues";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        // 获取连接对象
        connection = RabbitMQUtil.getConnection();
        // 获取连接中通道
        channel = connection.createChannel();

        channel.queueDeclare(QueueName,true,false,false,null);
        String msg = "Task";
        for (int i = 1; i <= 20; i++) {

            channel.basicPublish("",QueueName,null, (msg+i).getBytes());
            System.out.println("已发布Task"+i);
        }
        System.out.println("已发布完所有:"+msg);
        RabbitMQUtil.closeConnectionAndChanel(channel, connection);
    }
}
