package com.yren.shardingSphereDemo.controller;

import com.yren.shardingSphereDemo.entity.Dict;
import com.yren.shardingSphereDemo.entity.OrderVo;
import com.yren.shardingSphereDemo.mapper.DictMapper;
import com.yren.shardingSphereDemo.mapper.OrderMapper;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ChenYu ren
 * @date 2025/8/13
 */

@RestController
@RequestMapping("/horizontal/broadcast")
public class ShardingSphereHorizontalBroadcastController {

    @Resource
    private DictMapper dictMapper;

    @GetMapping("/add")
    public String broadcastTableAdd(){
        Dict dict = new Dict();
        dict.setDictType("type1");
        dictMapper.insert(dict);
        return "success";
    }

    @GetMapping("/findAll")
    public String findAll(){
        return dictMapper.selectList(null).toString();
    }


    @GetMapping("updateById")
    public String updateById(@RequestParam("id")Long id) {
        Dict dict = new Dict();
        dict.setId(id);
        dict.setDictType(String.valueOf(System.currentTimeMillis()));
        dictMapper.updateById(dict);
        return "success";
    }


    @GetMapping("delById")
    public String delById(@RequestParam("id")Long id) {
        Dict dict = new Dict();
        dict.setId(id);
        dictMapper.deleteById(dict);
        return "success";
    }

}
