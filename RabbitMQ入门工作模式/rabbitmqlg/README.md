# RabbitMQ深入浅出

生产者 → 消费者

## 引言

### 什么是rabbitMQ

​	**MQ**(Message Quene)：翻译为消息队列,通**过典型的生产者和消费者模型生产者不断向消息队列中生产消息，消费者不断的从队列中获取消息**。因为==消息的生产和消费都是异步的==，而且只关心消息的发送和接收，没有业务逻辑的侵入轻松的实现系统间解耦。别名为**消息中间件**、通过利用高效可靠的消息传递机制进行平台无关的数据交流，并基于数据通信来进行分布式系统的集成。

### 消息队列如何解耦

​	比如我们在京东商城买东西，我们买了一双鞋子，那么库存系统则要进行减少一双的动作操作（这里是一个复杂的过程，我们这里只是简单的描述一下应用场景，复杂的场景不仔细讲，比如下单支付，支付扣款，客户接受扣款成功然后减库存），如果传统的做法我们订单成功之后，那么我们库存进行减少，此操作我们必须保证在一个事物里，这是一个原子操作，这样做在当今高并发的场景中并不使用，效率极其的低，并且应用系统之间耦合度较高。那么消息中间件可以解决我们这类的问题，我们首先将订单消息发送给mq的服务队列中，那么库存系统可以接受对应队列中进行消息处理，那么此时我们的应用系统之间并没有什么耦合联系，而都是通过相对应的队列进行消息的接收和处理，这样我们就可以将我们的系统之间的耦合度进行拆分，并且消息机制的异步处理机制可以提高我们的效率，并且集群的使用可以进行并发访问的处理等。

​	因而我们只需要去关注各个系统的业务，而无需去关注他们的联系，将这一些的联系都交给消息队列去处理，从而达到一个解耦的效果。

### 不同MQ之间的比较

~~~bash
# ActiveMQ
	Active、是4pache出品，最流行的，能力强劲的开源消息总线。它是一个完全支持N规范的的消息中间件。丰富的APT,多种集群架构模式让hctiveKA在业界成为老牌的消息中间件，在中小型企业颇受欢迎!
	
# Kafka
	Kafka是LinkedIn开源的分布式发布-订阅消息系统，目前归属于Apache顶级项目。Kafka主要特点是基于Pull的模式来处理消息消费,追求高吞吐量（广泛应用于大数据领域），一开始的目的就是用于日志收集和传输。8.8版本开始支持复制，不支持事务，对消息的重复、丢失、错误没有严格要求,适合产生大量数据的互联网服务的数据收集业务。

# RocketMQ
	RocketMQ是阿里开源的消息中间件，它是纯Java开发，具有高吞吐量、高可用性、适合大规模分布式系统应用的特点。RocketMQ思路起源于Kafka，但并不是Kafka的一个Copy，它对消息的可靠传输及事务性做了优化，目前在阿里集团被广泛应用于交易、充值、流计算、消息推送、日志流式处理、binglog分发等场景。

# RabbitMQ
	RabbitMQ是使用Erlang语言开发的开源消息队列系统，基于AMQP协议来实现。AMQP的主要特征是面向消息、队列、路由(包括点对点和发布/订阅)、可靠性、 安全。AMQP协议更多用在企业系统内对数据一致性、稳定性和可靠性要求很高的场景，对性能和吞吐量的要求还在其次。
~~~



## 环境配置

​	由于RabbitMQ是基于erlang语言的，因此除了基于系统需求的RabbitMQ安装外，还需要安装对应erlang依赖。

​	官方地址：https://www.rabbitmq.com/download.html

~~~xml
    <dependencies>
        <dependency>
            <groupId>com.rabbitmq</groupId>
            <artifactId>amqp-client</artifactId>
            <version>5.9.0</version>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.1</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
~~~



## 工作模式

### 简单模式

​	Provider → Queue→ Consumer

![rabbitmq直连.png](http://img.fyyice.cn/rabbitmq直连.png)

**Provider**（生产者）

~~~java
package cn.fyyice.rabbitmq.simple;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class Provider {

    private final static String QueueName = "GxinTa_CoaWRo";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        // 获取连接对象
        connection = RabbitMQUtil.getConnection();
        // 获取连接中通道
        channel = connection.createChannel();

        /**
         * 创建队列
         * 参数一：队列名字，队列不存在自动创建
         * 参数二，是否持久化  当mq重启之后还在
         * 参数三：是否独占队列 true/false
         		* 独占队列(只能由一个消费者监听这个队列)
         		* 当Connection关闭时，是否删除队列
         * 参数四: 是否在消费完成后删除队列
         * 参数五：额外附加参数
         */
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
~~~



**Consumer**（消费者）

这里踩坑：由于消费者不确定生产者什么时候会发布消息，所以需要保持和管道的连接，不要释放掉资源！！**一直保持监听状态**

~~~java
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
~~~

**RabbitMQUtils**

这里由于连接的MQ都是统一的地址，因此做了封装。后面所有示例都是用此方式进行连接。

~~~java
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
        // host 如果不设置默认值为localhost
        connectionFactory.setHost("127.0.0.1");
        // 设置端口号  默认5672
        connectionFactory.setPort(5672);
        // 设置虚拟机的用户名和密码   默认都是guest
        connectionFactory.setUsername("fwh");
        connectionFactory.setPassword("1234");
        //设置连接超时
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
~~~



### 工作队列模式

**Work queues**

​	与简单模式相比，工作队列模式多了一些消费端。多个消费端共同消费一个队列中的信息。相当于一个消息的分发。

Provider → Queue  → Consumer1 

​								  → Consumer2

​								  →      。。。

**应用场景**

​	对于人物过重或者任务较多情况使用工作队列可以提高任务的处理速度。

​	在这里我们模拟两个消费端去共同消费一个生产者。两个消费端共同监听一个生产者队列，观察控制台结果不难发现他们之间是==顺序分配==，你一条我一条这样的模式。

**注意：**所有消费端都是出于==竞争状态==

**Provider**

~~~java
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
~~~

**Consumer1**

~~~java
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
~~~

**Consumer2**

~~~java
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

~~~

### Pub/Sub 订阅模式

Provider → Exchange → Queue1 →Consumer1

​										→ Queue2 →Consumer2

​										→ Queue* →Consumer*

**Exchange：交换机。**一方面，接收生产者发送的消息。另一方面，知道如何处理消息，例如递交给某个特别队列、递交给所有队列、或是将消息丢地。具体如何操作取决于交换机的类型。

- Fanout：广播，将消息交给所有绑定到交换机的队列
- Direct：定向，把消息交给符合指定routing key的队列
- Topic：通配符，把消息交给符合routing pattern（路由模式）的队列

**注意：**交换机只负责转发消息，不具备存储消息的能力。



**Provider**

~~~java
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

~~~

**Consumer1**

~~~java
package cn.fyyice.rabbitmq.pubsub;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer1 {

    private final static String QueueName = "Sub_Queues";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        //获取连接
        connection = RabbitMQUtil.getConnection();
        //获取连接通道
        channel = connection.createChannel();
        //取得对应管道通信
        channel.queueDeclare(QueueName+1,true,false,false,null);
        //获取消息
        /**
         *  public String basicConsume(String queue, boolean autoAck, Consumer callback) throws IOException
         *  queue：队列名称
         *  autoAck：自动确认
         *  callback：回调对象
         */
        channel.basicConsume(QueueName+1,false,new DefaultConsumer(channel){
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
                System.out.println(QueueName+1+"已收到通知");
            }
        });

    }
}
~~~

**Consumer2**

~~~java
package cn.fyyice.rabbitmq.pubsub;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer2 {

    private final static String QueueName = "Sub_Queues";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        //获取连接
        connection = RabbitMQUtil.getConnection();
        //获取连接通道
        channel = connection.createChannel();
        //取得对应管道通信
        channel.queueDeclare(QueueName+2,true,false,false,null);
        //获取消息
        /**
         *  public String basicConsume(String queue, boolean autoAck, Consumer callback) throws IOException
         *  queue：队列名称
         *  autoAck：自动确认
         *  callback：回调对象
         */
        channel.basicConsume(QueueName+2,false,new DefaultConsumer(channel){
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
                System.out.println(QueueName+2+"已收到通知");
            }
        });
    }
}
~~~

**控制台信息展示：**

~~~bash
#Consumer1 
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
管理员发布了全局消息，所有人员请注意
Sub_Queues1已收到通知

#Consumer2
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
管理员发布了全局消息，所有人员请注意
Sub_Queues2已收到通知
~~~

### Routing 路由模式

Provider → Exchange → Routing key1 → Queue1 → Consumer1

​										→ Routing key2 → Queue2 → Consumer2

​										→ Routing key3 → Queue3 → Consumer3

​										 → Routing key* → Queue* → Consumer*

**模式说明**

- 队列与交换机的绑定，不能是任意绑定了，而是要指定一个Routing Key(路由key)。
- 消息的发送方在向Exchange 发送消息时，也必须指定消息的Routing Key。
- Exchange 不再把消息交给每一个绑定的队列，而是根据消息的Routing Key进行判断，只有队列的Routingkey与消息的 Routing key完全一致，才会接收到消息。

**这里以日志分发来进行演示**：

**Provider**

这里构建了一个String类型的Routeing key数组。用Random来进随机分发

~~~java
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
~~~

**Consumer1**

肢解收日志Error类型信息

~~~java
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
~~~

**Consumer2**

接收除Error外的所有类型消息

~~~java
package cn.fyyice.rabbitmq.routing;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer2 {

    private final static String exchangeName = "RoutingDistribute";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        connection= RabbitMQUtil.getConnection();
        // 获取连接中通道
        channel=connection.createChannel();
        channel.exchangeDeclare(exchangeName,BuiltinExchangeType.DIRECT, true, false, false, null);
        // 创建队列
        String queneName = "Center_Of_Others";
        channel.queueDeclare(queneName,true,false,false,null);
        // 绑定交换机和队列
        channel.queueBind(queneName,exchangeName,"info");
        channel.queueBind(queneName,exchangeName,"waring");
        channel.queueBind(queneName,exchangeName,"debug");

        // 消费信息
        channel.basicConsume(queneName,true,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("Consumer2已接收到消息类型 -------- "+new String(body));
            }
        });
    }
}
~~~

**控制台展示：**

~~~bash
# Consumer1
Consumer1 已接收到消息类型 --------- 生产者第3次发送的日志信息为 error

# Consumer2
Consumer2已接收到消息类型 -------- 生产者第1次发送的日志信息为 waring
Consumer2已接收到消息类型 -------- 生产者第2次发送的日志信息为 waring
Consumer2已接收到消息类型 -------- 生产者第4次发送的日志信息为 waring
Consumer2已接收到消息类型 -------- 生产者第5次发送的日志信息为 info
Consumer2已接收到消息类型 -------- 生产者第6次发送的日志信息为 info
Consumer2已接收到消息类型 -------- 生产者第7次发送的日志信息为 debug
Consumer2已接收到消息类型 -------- 生产者第8次发送的日志信息为 info
Consumer2已接收到消息类型 -------- 生产者第9次发送的日志信息为 info
Consumer2已接收到消息类型 -------- 生产者第10次发送的日志信息为 debug
~~~



### Topics 通配符模式

Provider → Exchange（type=topic） → 通配符表达式( *   # )→ Queue1 → Consumer1

与Routing路由模式相比，Topics通配符模式对消息的分配更加 的灵活，我们可以根据业务系统的需求，将相对应的信息进行准确分发。

*** 和 # 都是占位符，\* 代表一个单词，# 代表0个或者多个单词，单词直接用 . 分隔**

**Provider**

我加入了一个businessKey的字符串数组，通过Random方法，随机获取一个下标对应的字符串和routeKey随机获取的字符串进行合并，模拟 业务.日志级别 的信息，进行消息的分发。

~~~java
package cn.fyyice.rabbitmq.topic;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.Random;

public class Provider {
    private final static String exchangeName = "TopicsDistribute";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        connection = RabbitMQUtil.getConnection();
        channel = connection.createChannel();
        // 交换机名称  交换机
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC,true,false,false,null);
        String [] routeKey = {"error","info","waring","debug"};
        String [] businessKey = {"goods","orders","message"};
        // 发送信息
        for(int i=1;i <= 10;i++){
            int temp = new Random().nextInt(100)%4;
            int bs = new Random().nextInt(10)%3;
            // 参数一：交换机名称
            // 参数二：队列名称
            // 参数三: 传递消息额外设置
            // 参数四：消息的内容
            channel.basicPublish(exchangeName,businessKey[bs]+"."+routeKey[temp],null,("生产者第"+i+"次发送的日志信息为 "+businessKey[bs]+"."+routeKey[temp]).getBytes());
        }
        RabbitMQUtil.closeConnectionAndChanel(channel,connection);
    }
}
~~~

**Comsumer1**

`"#.error"`对所有以.error类型的信息进行接收

~~~java
package cn.fyyice.rabbitmq.topic;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer1 {

    private final static String exchangeName = "TopicsDistribute";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        connection = RabbitMQUtil.getConnection();
        // 获取连接中通道
        channel=connection.createChannel();
        //创建交换机
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC,true,false,false,null);
        // 创建队列
        String queneName= "CenterTopic_Of_Error";
        channel.queueDeclare(queneName,true,false,false,null);
        // 绑定交换机和队列
        channel.queueBind(queneName,exchangeName,"#.error");
        // 消费信息
        channel.basicConsume(queneName,true,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("Consumer1 已接收到消息类型 --------- "+new String(body));
            }
        });
    }
}
~~~

**Consumer2**

~~~java
package cn.fyyice.rabbitmq.topic;

import cn.fyyice.rabbitmq.utils.RabbitMQUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer2 {

    private final static String exchangeName = "TopicsDistribute";
    private static Connection connection ;
    private static Channel channel;

    public static void main(String[] args) throws IOException {
        connection = RabbitMQUtil.getConnection();
        // 获取连接中通道
        channel=connection.createChannel();
        //创建交换机
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC,true,false,false,null);
        // 创建队列
        String queneName= "CenterTopic_Of_Others";
        channel.queueDeclare(queneName,true,false,false,null);
        // 绑定交换机和队列
        channel.queueBind(queneName,exchangeName,"#.info");
        channel.queueBind(queneName,exchangeName,"#.waring");
        channel.queueBind(queneName,exchangeName,"#.debug");
        // 消费信息
        channel.basicConsume(queneName,true,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("Consumer2 已接收到消息类型 --------- "+new String(body));
            }
        });
    }
}
~~~

**控制台展示：**

~~~bash
# Consumer1
Consumer1 已接收到消息类型 --------- 生产者第4次发送的日志信息为 goods.error
Consumer1 已接收到消息类型 --------- 生产者第5次发送的日志信息为 orders.error
Consumer1 已接收到消息类型 --------- 生产者第6次发送的日志信息为 goods.error
Consumer1 已接收到消息类型 --------- 生产者第8次发送的日志信息为 orders.error
Consumer1 已接收到消息类型 --------- 生产者第9次发送的日志信息为 goods.error
Consumer1 已接收到消息类型 --------- 生产者第10次发送的日志信息为 message.error


# Consumer2
Consumer2 已接收到消息类型 --------- 生产者第1次发送的日志信息为 orders.info
Consumer2 已接收到消息类型 --------- 生产者第2次发送的日志信息为 message.waring
Consumer2 已接收到消息类型 --------- 生产者第3次发送的日志信息为 goods.info
Consumer2 已接收到消息类型 --------- 生产者第7次发送的日志信息为 orders.info
~~~

### 总结

自己看官网吧   https://www.rabbitmq.com/getstarted.html

