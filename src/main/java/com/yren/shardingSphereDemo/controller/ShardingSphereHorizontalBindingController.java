package com.yren.shardingSphereDemo.controller;

import com.yren.shardingSphereDemo.entity.OrderVo;
import com.yren.shardingSphereDemo.mapper.OrderItemMapper;
import com.yren.shardingSphereDemo.mapper.OrderMapper;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ChenYu ren
 * @date 2025/8/13
 */

@RestController
@RequestMapping("/horizontal/binding")
public class ShardingSphereHorizontalBindingController {

    @Resource
    private OrderMapper orderMapper;

    @GetMapping("/find")
    public String bindingFind(){
        List<OrderVo> orderAmount = orderMapper.getOrderAmount();
        return orderAmount.toString();
    }

}
