package com.yren.shardingSphereDemo.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yren.shardingSphereDemo.entity.Order;
import com.yren.shardingSphereDemo.entity.User;
import com.yren.shardingSphereDemo.mapper.OrderMapper;
import com.yren.shardingSphereDemo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.aspectj.weaver.ast.Or;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * @author ChenYu ren
 * @date 2025/7/18
 */

@RestController
@RequestMapping("/horizontal")
public class ShardingSphereHorizontalTestController {

    @Resource
    private UserMapper  userMapper;

    @Resource
    private OrderMapper orderMapper;

    @GetMapping("/add")
    public String add(@RequestParam("userId")Long userId, @RequestParam("orderNo")String orderNo) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAmount(new BigDecimal(100));
        orderMapper.insert(order);
        return "success";
    }

    @GetMapping("/get")
    public String getById(@RequestParam("Long")Long id) {
        return orderMapper.selectById(id).toString();
    }


    @GetMapping("/list")
    public String findAll() {
        return orderMapper.selectList(null).toString();
    }


    @GetMapping("/page")
    public String findAll(@RequestParam("pageNum")Long pageNum,
                          @RequestParam("pageSize")Long pageSize,
                          @RequestParam("userId")Long userId) {
        return orderMapper.selectPage(new Page<>(pageNum,pageSize),
                Wrappers.<Order>lambdaQuery()
                        .eq(Order::getUserId, userId))
                .toString();
    }

}
