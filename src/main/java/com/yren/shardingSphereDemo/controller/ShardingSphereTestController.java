package com.yren.shardingSphereDemo.controller;

import com.alibaba.fastjson.JSONObject;
import com.yren.shardingSphereDemo.entity.User;
import com.yren.shardingSphereDemo.mapper.DeptMapper;
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
@RequestMapping
public class ShardingSphereTestController {

    @Resource
    private UserMapper  userMapper;

    @Resource
    private DeptMapper deptMapper;

    @GetMapping("/test")
    @Transactional(rollbackFor = Exception.class)
    public String test(@RequestParam("username") String username) {
        User user = new User();
        user.setUname(username);
        userMapper.insert(user);
        return "success";
    }

    @GetMapping("/list")
    @Transactional(rollbackFor = Exception.class)
    public String list() {
        return JSONObject.toJSONString(userMapper.selectList(null));
    }

    @GetMapping("/list/dept")
    public String listDept() {
        return JSONObject.toJSONString(deptMapper.selectList(null));
    }

}
