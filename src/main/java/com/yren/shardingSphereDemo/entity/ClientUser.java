package com.yren.shardingSphereDemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author ChenYu ren
 * @date 2025/8/14
 */


@Data
@TableName("client_user")
public class ClientUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String nickName;


}
