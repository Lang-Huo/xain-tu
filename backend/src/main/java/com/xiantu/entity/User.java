package com.xiantu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiantu.common.Realms;
import com.xiantu.common.handler.StringListJsonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@TableName("t_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号（道号），唯一 */
    private String username;

    /** 昵称（展示名），注册时填写，可后续修改 */
    private String nickname;

    /** 仙途编号：系统分配的 6 位数字，全局唯一，首位非 0 */
    @TableField("user_number")
    private String userNumber;

    /** BCrypt 哈希（哈希中已包含盐，无需单独 salt 字段） */
    private String passwordHash;

    /**
     * 灵根：JSON 字符串数组存于 t_user.spirit_root_code，如 ["GOLD","WOOD"]。
     * 元素个数即灵根数量（1=N 单灵根，2=双灵根，…），无主副之分。
     */
    @TableField(value = "spirit_root_code", typeHandler = StringListJsonTypeHandler.class)
    private List<String> spiritRoots = new ArrayList<>();

    /** 境界 code（LIANQI 练气 / JIANZHU 筑基 / ...），由 common.Realms 静态映射为中文名 */
    @TableField("realm_code")
    private String realmCode = Realms.DEFAULT_CODE;

    /** 境界层级 1-9：1-3 初期 / 4-6 中期 / 7-9 后期 */
    private int realmLevel = Realms.DEFAULT_LEVEL;

    /** 战斗属性 */
    private int hp = 100;
    private int maxHp = 100;
    private int attack = 10;
    private int defense = 5;
    private int speed = 8;

    /** 神识：探图核心能力，决定迷雾可见半径 */
    private int spiritualSense = 5;

    private long exp = 0;

    /** 灵力：施放功法消耗，战斗与修炼相关 */
    private int mana = 50;
    private int maxMana = 50;

    private LocalDateTime createdAt;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
}