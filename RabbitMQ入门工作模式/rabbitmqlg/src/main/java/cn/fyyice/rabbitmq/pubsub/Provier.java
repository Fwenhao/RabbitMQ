package cn.fyyice.rabbitmq.pubsub;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class Provier {
    private final static String QueueName = "Sub_Queues";
    private final static String exchangeName = "QiankunDLoY";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        connection = RabbitMQUtil.getConnection();
        channel = connection.createChannel();

        /**
         *  exchangeDeclare(String exchange, BuiltinExchangeType type, boolean durable, boolean autoDelete, boolean internal, Map<String, Object> arguments)
         *  type: 交换机类型
         *      DIRECT("direct")    定向
         *      FANOUT("fanout")    广播
         *      TOPIC("topic"),     通配符
         *      HEADERS("headers"); 参数匹配
         *  durable：是否持久化
         *  autoDelete：自动删除
         *  internal：内部使用，一般false
         *  arguments：参数
         */
        //创建交换机
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT,true,false,false,null);
        //创建队列
        channel.queueDeclare(QueueName+1,true,false,false,null);
        channel.queueDeclare(QueueName+2,true,false,false,null);
        /**
         * queueBind(String queue, String exchange, String routingKey)
         * routingKey:路由键---绑定规则
         */
        //将创建的队列和队列进行绑定
        channel.queueBind(QueueName+1,exchangeName,"");
        channel.queueBind(QueueName+2,exchangeName,"");
        //发布消息
        String message = "管理员发布了全局消息，所有人员请注意";
        channel.basicPublish(exchangeName,"",null,message.getBytes());

        RabbitMQUtil.closeConnectionAndChanel(channel,connection);
    }
}
