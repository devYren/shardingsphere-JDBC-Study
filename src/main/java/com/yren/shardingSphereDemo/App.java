package com.yren.shardingSphereDemo;

import com.yren.shardingSphereDemo.entity.User;
import com.yren.shardingSphereDemo.mapper.UserMapper;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author ChenYu ren
 * @date 2025/4/1
 */


@SpringBootApplication
public class App {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class, args);

//        DataSource dataSource = run.getBean(DataSource.class);
//        // 测试查询
//        try (Connection connection = dataSource.getConnection();
//             Statement statement = connection.createStatement()) {
//            ResultSet rs = statement.executeQuery("SELECT NOW()");
//            while (rs.next()) {
//                System.out.println("当前时间：" + rs.getString(1));
//            }
//        }
//
//        UserMapper userMapper = run.getBean(UserMapper.class);
//        User user = new User();
//        user.setUname("张三丰");
//        userMapper.insert(user);

    }

}
