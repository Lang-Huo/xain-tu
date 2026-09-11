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

-- 地图模板
CREATE TABLE IF NOT EXISTS t_map_template (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY       COMMENT '模板主键',
    code              VARCHAR(50)  NOT NULL UNIQUE                   COMMENT '模板 code（前端传这个）',
    name              VARCHAR(50)  NOT NULL                         COMMENT '模板展示名（如「后山竹林」）',
    recommended_realm VARCHAR(20)  NOT NULL DEFAULT 'LIANQI'        COMMENT '推荐境界 code',
    size              INT          NOT NULL DEFAULT 8                COMMENT 'N：地图边长（N×N 格）',
    max_steps         INT          NOT NULL DEFAULT 60               COMMENT '单次探索步数上限',
    obstacle_rate     INT          NOT NULL DEFAULT 12               COMMENT '障碍物占比 %（0-100）',
    monster_rate      INT          NOT NULL DEFAULT 10               COMMENT '野怪占比 %（0-100）',
    resource_rate     INT          NOT NULL DEFAULT 10               COMMENT '资源占比 %（0-100）',
    description       VARCHAR(500)                                  COMMENT '描述',
    created_at        TIMESTAMP                                    COMMENT '创建时间'
) COMMENT '地图模板表：定义秘境难度与生成参数';

-- 地图实例（每次进入一条）
CREATE TABLE IF NOT EXISTS t_map_instance (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY       COMMENT '实例主键',
    user_id           BIGINT       NOT NULL                         COMMENT '所属用户ID',
    template_id       BIGINT       NOT NULL                         COMMENT '模板ID',
    template_code     VARCHAR(50)  NOT NULL                         COMMENT '模板 code（冗余便于查）',
    seed              BIGINT       NOT NULL                         COMMENT '随机种子（地图布局由此唯一决定）',
    pos_x             INT          NOT NULL DEFAULT 0                COMMENT '玩家当前 X 坐标',
    pos_y             INT          NOT NULL DEFAULT 0                COMMENT '玩家当前 Y 坐标',
    step_count        INT          NOT NULL DEFAULT 0                COMMENT '已走步数',
    max_steps         INT          NOT NULL DEFAULT 60               COMMENT '步数上限（冗余模板字段）',
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'         COMMENT '状态：ACTIVE 进行中 / COMPLETED 通关 / ABANDONED 主动放弃',
    explored_json     VARCHAR(2000)                                 COMMENT '已探索过的格子 "x,y" 列表 JSON（用于记忆地形 / 迷雾）',
    consumed_json     VARCHAR(2000)                                 COMMENT '已采集 / 已清空的格子 "x,y" 列表 JSON（资源格变空后存这里）',
    created_at        TIMESTAMP                                    COMMENT '创建时间',
    updated_at        TIMESTAMP                                    COMMENT '更新时间'
) COMMENT '地图实例表：每个用户每次探索一条（最多一条 ACTIVE）';

-- 地图模板种子（一期只放后山竹林）
INSERT INTO t_map_template (code, name, recommended_realm, size, max_steps, obstacle_rate, monster_rate, resource_rate, description, created_at)
SELECT 'BACK_BAMBOO', '后山竹林', 'LIANQI', 8, 60, 12, 10, 10, '入门级秘境，林木森森、灵草散布。偶有低阶妖兽出没，适合练气期弟子初次历练。', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_map_template WHERE code = 'BACK_BAMBOO');