package cn.fyyice.rabbitmq.pojo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class User implements Serializable {
    private String userName;
    private String[] likes;
    private Integer age;
}
