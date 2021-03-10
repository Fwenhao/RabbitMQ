package cn.fyyice.rabbitmq.utils;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQUtil {
    private static ConnectionFactory connectionFactory;
    static {
        connectionFactory=new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        // 设置端口号
        connectionFactory.setPort(5672);
        // 设置虚拟机的用户名和密码
        connectionFactory.setUsername("fwh");
        connectionFactory.setPassword("1234");
        connectionFactory.setConnectionTimeout(999999999);
    }
    public static Connection  getConnection(){
        // ConnectionFactory connectionFactory=new ConnectionFactory();
        // 设置主机名
        try {
            // 获取连接对象
            Connection connection=connectionFactory.newConnection();
            return connection;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }finally {

        }
        return  null;
    }

    public static void closeConnectionAndChanel(Channel channel,Connection connection){
        try {
            if(channel!=null){
                channel.close();
            }
            if(connection!=null){
                connection.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
