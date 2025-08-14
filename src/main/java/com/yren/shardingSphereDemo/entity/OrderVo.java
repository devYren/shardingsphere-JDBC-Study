package com.yren.shardingSphereDemo.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ChenYu ren
 * @date 2025/8/13
 */

@Data
public class OrderVo {
    private String orderNo;
    private BigDecimal amount;
}
