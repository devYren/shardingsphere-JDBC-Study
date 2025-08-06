package com.yren.shardingSphereDemo.controller;


import com.yren.shardingSphereDemo.entity.Order;
import com.yren.shardingSphereDemo.entity.User;
import com.yren.shardingSphereDemo.mapper.OrderMapper;
import com.yren.shardingSphereDemo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * @author ChenYu ren
 * @date 2025/7/18
 */

@RestController
@RequestMapping("/vertical")
public class ShardingSphereVerticalTestController {

    @Resource
    private UserMapper  userMapper;

    @Resource
    private OrderMapper orderMapper;

    @GetMapping("/list")
    public String list() {
        return userMapper.selectList(null) + "\n" + orderMapper.selectList(null);
    }

    @GetMapping("/add")
    public String add() {
        User user = new User();
        user.setUname("yren");
        userMapper.insert(user);
        Order order = new Order();
        order.setOrderNo("YREN10001");
        order.setUserId(user.getId());
        order.setAmount(new BigDecimal(100));
        orderMapper.insert(order);
        return "success";
    }

}
