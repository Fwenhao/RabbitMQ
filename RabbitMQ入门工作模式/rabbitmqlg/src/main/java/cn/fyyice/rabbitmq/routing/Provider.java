package cn.fyyice.rabbitmq.routing;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.Random;

public class Provider {
    private final static String exchangeName = "RoutingDistribute";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        connection = RabbitMQUtil.getConnection();
        channel = connection.createChannel();
        // 交换机名称  交换机
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT,true,false,false,null);
        String [] routeKey = {"error","info","waring","debug"};

        // 发送信息
        for(int i=1;i <= 10;i++){
            int temp = new Random().nextInt(100)%4;
            // 参数一：交换机名称
            // 参数二：队列名称
            // 参数三: 传递消息额外设置
            // 参数四：消息的内容
            channel.basicPublish(exchangeName,routeKey[temp],null,("生产者第"+i+"次发送的日志信息为 "+routeKey[temp]).getBytes());
        }
        RabbitMQUtil.closeConnectionAndChanel(channel,connection);
    }
}
