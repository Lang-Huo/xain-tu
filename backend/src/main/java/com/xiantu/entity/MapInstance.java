package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiantu.common.handler.StringListJsonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 地图实例：用户每次「进入秘境」生成一条。
 *
 * <p>由于地图布局由 seed 唯一决定，实例表本身不需要存全量格子：
 * 每次读取时按 seed 重新生成网格，再叠加 consumed_json 即可。
 *
 * <p>explored_json / consumed_json 都是 "x,y" 形式的字符串列表，
 * 用 {@link StringListJsonTypeHandler} 与 VARCHAR 双向序列化。
 */
@Getter
@Setter
@TableName("t_map_instance")
public class MapInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("template_id")
    private Long templateId;

    @TableField("template_code")
    private String templateCode;

    /** 随机种子（决定本次地图布局） */
    private long seed;

    @TableField("pos_x")
    private int posX;

    @TableField("pos_y")
    private int posY;

    @TableField("step_count")
    private int stepCount;

    @TableField("max_steps")
    private int maxSteps;

    /** 状态：ACTIVE / COMPLETED / ABANDONED */
    private String status;

    /**
     * 已探索过的格子 "x,y" 列表，用于迷雾「记忆地形」：离开视野后仍以暗色显示已探索过的格子。
     */
    @TableField(value = "explored_json", typeHandler = StringListJsonTypeHandler.class)
    private List<String> explored = new ArrayList<>();

    /**
     * 已采集 / 已清空的格子 "x,y" 列表：走到资源格采集后格子视为空地，记录在这里；
     * 下次重新生成网格时将对应位置覆盖为 EMPTY。
     */
    @TableField(value = "consumed_json", typeHandler = StringListJsonTypeHandler.class)
    private List<String> consumed = new ArrayList<>();

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 状态常量 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_ABANDONED = "ABANDONED";
}