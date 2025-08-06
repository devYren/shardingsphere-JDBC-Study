package com.yren.shardingSphereDemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yren.shardingSphereDemo.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ChenYu ren
 * @date 2025/4/10
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
