package com.yren.shardingSphereDemo.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yren.shardingSphereDemo.entity.Order;
import com.yren.shardingSphereDemo.entity.OrderItem;
import com.yren.shardingSphereDemo.mapper.OrderItemMapper;
import com.yren.shardingSphereDemo.mapper.OrderMapper;
import jakarta.annotation.Resource;
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
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

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

    @GetMapping("/getByUserId")
    public String getByUserId(@RequestParam("userId")Long userId) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery().eq(Order::getUserId,userId)).toString();
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

    @GetMapping("addOrderItem")
    public String addOrderItem() {
        for (long i = 1; i < 3; i++) {
            Order order = new Order();
            order.setOrderNo("YREN" + i);
            order.setUserId(1L);
            orderMapper.insert(order);
            for (long j = 1; j < 3; j++) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderNo("YREN" + i);
                orderItem.setUserId(1L);
                orderItem.setPrice(new BigDecimal(10));
                orderItem.setCount(2);
                orderItemMapper.insert(orderItem);
            }
        }

        for (long i = 5; i < 7; i++) {
            Order order = new Order();
            order.setOrderNo("YREN" + i);
            order.setUserId(2L);
            orderMapper.insert(order);
            for (long j = 1; j < 3; j++) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderNo("YREN" + i);
                orderItem.setUserId(2L);
                orderItem.setPrice(new BigDecimal(1));
                orderItem.setCount(3);
                orderItemMapper.insert(orderItem);
            }
        }
        return "success";
    }

}
