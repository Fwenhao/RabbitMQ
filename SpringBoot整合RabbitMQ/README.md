# SpringBoot整合RabbitMQ

**步骤：**

1. 创建工程
2. 引入依赖坐标
3. 编写yml配置文件
4. 测试



## 创建工程

新建一个project，这里可以使用Spring Initalizr，或者Maven的方式都可以。这里就不多做概述。为体现消息中间件的解耦特性，这里我们使用两个工程（Provider、Consumer），一发一收的方式来展示。



## 引入依赖坐标

这里需要rabbitmq的相关依赖和SpringBoot的环境依赖

~~~xml
   <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.4.RELEASE</version>
    </parent>
        
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-amqp</artifactId>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-test</artifactId>
                <scope>test</scope>
            </dependency>
        </dependencies>
~~~

## 编写yml配置文件

rabbitmq目标相同，所以Privoder、Consumer 的rabbitmq配置是一样的

~~~yml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: fwh
    password: 1234
    virtual-host: /

~~~

## 项目工程

### Provider

消息发布者

> 编写配置类

~~~java
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

~~~

> junit测试

~~~java
package cn.fyyice.rabbitmq;

import cn.fyyice.rabbitmq.config.RabbitMQConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ProviderTest {

    @Autowired
    //rabbit通信
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSend(){
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,"boot.#","你好，编码人");
    }
}
~~~



### Consumer

作为消费者，我们需要明确消费的队列是谁，因此我们需要配置监听的队列，然后通过一个方法来进行回调内容

~~~java
package cn.fyyice.springbootconsumer.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQListener {

    @RabbitListener(queues = "my_queue")
    public void ListenerQueue(Message message){
        //字符集转换   防止乱码问题	
        String info = new String(message.getBody(),"utf-8");
        System.out.println("接收到消息"+info);
    }
}

~~~



### 测试

**Privoder**发送

~~~bash
rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,"boot.#","你好，编码人");
~~~

Consumer

~~~bash
接收到消息你好，编码人
~~~



Demo详情地址+文档：	https://github.com/Fwenhao/RabbitMQ   欢迎访问