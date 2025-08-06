package com.yren.shardingSphereDemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ChenYu ren
 * @date 2025/8/1
 */
@TableName("t_order")
@Data
public class Order {
//    @TableId(type = IdType.AUTO)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
}