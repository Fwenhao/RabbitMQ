package cn.fyyice.rabbitmq.simple;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer {

    private final static String QueueName = "GxinTa_CoaWRo";
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
        /**
         *  public String basicConsume(String queue, boolean autoAck, Consumer callback) throws IOException
         *  queue：队列名称
         *  autoAck：自动确认
         *  callback：回调对象
         */
        channel.basicConsume(QueueName,false,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                /**
                 * super.handleDelivery(consumerTag, envelope, properties, body);
                 * consumerTag：标识
                 * envelope：获取一些信息
                 * properties：配置信息
                 * body：数据
                 */
                System.out.println(new String(body));
            }
        });

    }
}
