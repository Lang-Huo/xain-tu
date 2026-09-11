package com.xiantu.web.dto;

import com.xiantu.common.Realms;
import com.xiantu.entity.User;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserProfile {

    private Long id;
    private String username;
    private String nickname;        // 昵称（展示名）
    private String userNumber;      // 仙途编号（6 位全局唯一）
    private List<SpiritRootInfo> spiritRoots = new ArrayList<>(); // 用户持有的全部灵根（无主副之分）
    /** 境界 code（如 LIANQI） */
    private String realmCode;
    /** 境界层级 1-9 */
    private int realmLevel;
    /** 境界展示名（自动拼装，如「练气初期」「金丹后期」） */
    private String realmDisplayName;
    private int hp;
    private int maxHp;
    private int attack;
    private int defense;
    private int speed;
    private int spiritualSense;     // 神识
    private int mana;               // 灵力
    private int maxMana;            // 灵力上限
    private long exp;

    public static UserProfile of(User u) {
        UserProfile p = new UserProfile();
        p.setId(u.getId());
        p.setUsername(u.getUsername());
        p.setNickname(u.getNickname());
        p.setUserNumber(u.getUserNumber());
        // spiritRoots 由 service 层按 user.spiritRoots (JSON) 解析后填充
        p.setRealmCode(u.getRealmCode());
        p.setRealmLevel(u.getRealmLevel());
        p.setRealmDisplayName(Realms.displayName(u.getRealmCode(), u.getRealmLevel()));
        p.setHp(u.getHp());
        p.setMaxHp(u.getMaxHp());
        p.setAttack(u.getAttack());
        p.setDefense(u.getDefense());
        p.setSpeed(u.getSpeed());
        p.setSpiritualSense(u.getSpiritualSense());
        p.setMana(u.getMana());
        p.setMaxMana(u.getMaxMana());
        p.setExp(u.getExp());
        return p;
    }
}