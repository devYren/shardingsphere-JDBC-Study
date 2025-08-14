package com.yren.shardingSphereDemo.controller;

import com.alibaba.fastjson.JSONObject;
import com.yren.shardingSphereDemo.entity.ClientUser;
import com.yren.shardingSphereDemo.entity.User;
import com.yren.shardingSphereDemo.mapper.ClientUserMapper;
import com.yren.shardingSphereDemo.mapper.DeptMapper;
import com.yren.shardingSphereDemo.mapper.OrderMapper;
import com.yren.shardingSphereDemo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ChenYu ren
 * @date 2025/7/18
 */


@RestController
@RequestMapping("test2")
public class ShardingSphereTest2Controller {

    @Resource
    private ClientUserMapper clientUserMapper;

    @Resource
    private OrderMapper orderMapper;

    @GetMapping("/findAll")
    public String findAll() {
        return clientUserMapper.selectList(null).toString();
    }

    @GetMapping("/add")
    public String add() {
        ClientUser clientUser = new ClientUser();
        clientUser.setNickName(System.currentTimeMillis() + "");
        clientUserMapper.insert(clientUser);
        return "success";
    }

    @GetMapping("/findAllO")
    public String findAllO() {
        return orderMapper.selectList(null).toString();
    }



}
