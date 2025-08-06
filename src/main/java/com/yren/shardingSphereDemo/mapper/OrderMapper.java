package com.yren.shardingSphereDemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yren.shardingSphereDemo.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ChenYu ren
 * @date 2025/8/1
 */

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
