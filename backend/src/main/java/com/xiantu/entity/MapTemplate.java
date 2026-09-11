package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 地图模板：定义某张秘境的难度 / 尺寸 / 资源 / 野怪比例等参数。
 * 实例（{@link MapInstance}）每次进入会根据模板生成一张具体的网格（由 seed 决定）。
 */
@Getter
@Setter
@TableName("t_map_template")
public class MapTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板 code（前端入图参数） */
    private String code;

    /** 展示名（中文） */
    private String name;

    /** 推荐境界 code（LIANQI / JIANZHU / ...） */
    @TableField("recommended_realm")
    private String recommendedRealm;

    /** N：地图边长，N×N 格 */
    private int size;

    /** 单次探索步数上限 */
    @TableField("max_steps")
    private int maxSteps;

    /** 障碍物占比 %（0-100） */
    @TableField("obstacle_rate")
    private int obstacleRate;

    /** 野怪占比 %（0-100） */
    @TableField("monster_rate")
    private int monsterRate;

    /** 资源占比 %（0-100） */
    @TableField("resource_rate")
    private int resourceRate;

    /** 描述（前端展示） */
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 推荐境界中文名（由 service 层填充；非持久化字段） */
    @TableField(exist = false)
    private String recommendedRealmName;
}