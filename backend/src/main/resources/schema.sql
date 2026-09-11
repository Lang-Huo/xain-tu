-- 一期 M1：H2 内存库建表（MyBatis-Plus 不自动建表，启动由 spring.sql.init 执行）
-- 每个字段加 COMMENT 注释，说明其含义，便于 DDL 自文档化。
CREATE TABLE IF NOT EXISTS t_user (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '用户主键ID，自增',
    username          VARCHAR(50)  NOT NULL UNIQUE               COMMENT '登录账号（道号），全局唯一',
    nickname          VARCHAR(50)  NOT NULL                       COMMENT '昵称/展示名，可后续修改',
    user_number       VARCHAR(6)   NOT NULL UNIQUE               COMMENT '仙途编号：注册时系统分配的 6 位数字，全局唯一，首位非 0',
    password_hash     VARCHAR(100) NOT NULL                       COMMENT 'BCrypt 密码哈希（哈希中已含盐）',
    mana              INT          NOT NULL                       COMMENT '当前灵力：施放功法消耗',
    max_mana          INT          NOT NULL                       COMMENT '灵力上限',
    spirit_root_code  VARCHAR(1000)                               COMMENT '用户灵根 JSON 字符串数组，如 ["GOLD","WOOD"]；数组长度 = 灵根数量，无主副之分',
    realm_code        VARCHAR(20)  NOT NULL DEFAULT 'FAN_REN'      COMMENT '境界',
    realm_level       INT          NOT NULL DEFAULT 1             COMMENT '境界层级 1-9：1-3 初期 / 4-6 中期 / 7-9 后期',
    hp                INT          NOT NULL                       COMMENT '当前气血（生命值）',
    max_hp            INT          NOT NULL                       COMMENT '气血上限',
    attack            INT          NOT NULL                       COMMENT '攻击力',
    defense           INT          NOT NULL                       COMMENT '防御力',
    speed             INT          NOT NULL                       COMMENT '身法/速度，影响战斗出手顺序与逃跑成功率',
    spiritual_sense   INT          NOT NULL                       COMMENT '神识：决定迷雾可见半径 R = 基础值 + f(神识)，探图核心能力',
    exp               BIGINT       NOT NULL                       COMMENT '修为/经验值',
    created_at        TIMESTAMP                                    COMMENT '账号创建时间'
) COMMENT '用户表：账号、灵根、境界（code+level9层制）、战斗属性与修为';