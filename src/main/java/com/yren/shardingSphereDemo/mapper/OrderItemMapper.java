package com.yren.shardingSphereDemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yren.shardingSphereDemo.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ChenYu ren
 * @date 2025/8/13
 */

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {}