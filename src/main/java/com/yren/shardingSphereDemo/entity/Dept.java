package com.yren.shardingSphereDemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author ChenYu ren
 * @date 2025/7/18
 */

@TableName("t_dept")
@Data
public class Dept {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String deptName;
}
