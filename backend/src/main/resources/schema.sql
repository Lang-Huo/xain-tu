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
SELECT 'BACK_BAMBOO', '后山竹林', 'LIAN_QI', 8, 60, 12, 10, 10, '入门级秘境，林木森森、灵草散布。偶有低阶妖兽出没，适合练气期弟子初次历练。', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_map_template WHERE code = 'BACK_BAMBOO');

-- 物品定义（M5 背包：一期只放灵草）
CREATE TABLE IF NOT EXISTS t_item (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY             COMMENT '物品主键',
    code          VARCHAR(50)  NOT NULL UNIQUE                         COMMENT '物品 code',
    name          VARCHAR(50)  NOT NULL                               COMMENT '物品名',
    type          VARCHAR(20)  NOT NULL DEFAULT 'MATERIAL'            COMMENT '类型：MATERIAL 材料 / EQUIPMENT 装备 / PILL 丹药 / TECHNIQUE 功法',
    subtype       VARCHAR(20)                                         COMMENT '子类型（仅 EQUIPMENT/TECHNIQUE 用）：WEAPON 法器 / ARMOR 护身 / STORAGE 储物 / TECHNIQUE 功法',
    rarity        VARCHAR(20)  NOT NULL DEFAULT 'COMMON'              COMMENT '品阶：COMMON / RARE / EPIC / LEGENDARY',
    description   VARCHAR(500)                                        COMMENT '描述',
    attrs_json    VARCHAR(1000)                                       COMMENT '属性 JSON（如 {"consumable":true,"hpRestore":30}）',
    created_at    TIMESTAMP                                          COMMENT '创建时间'
) COMMENT '物品定义表';

-- 兼容旧版 schema（已经建过 t_item 没 subtype 列）
ALTER TABLE t_item ADD COLUMN IF NOT EXISTS subtype VARCHAR(20);

-- 用户背包（每个用户一条）
CREATE TABLE IF NOT EXISTS t_inventory (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY             COMMENT '背包主键',
    user_id       BIGINT       NOT NULL UNIQUE                        COMMENT '所属用户ID',
    capacity      INT NOT NULL DEFAULT 30                              COMMENT '格子上限（按物品种类计）',
    created_at    TIMESTAMP                                          COMMENT '创建时间',
    updated_at    TIMESTAMP                                          COMMENT '更新时间'
) COMMENT '用户背包表';

-- 背包物品行
CREATE TABLE IF NOT EXISTS t_inventory_item (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY             COMMENT '行主键',
    inventory_id  BIGINT       NOT NULL                              COMMENT '所属背包ID',
    item_id       BIGINT       NOT NULL                              COMMENT '物品定义ID',
    quantity      INT NOT NULL DEFAULT 1                              COMMENT '数量',
    UNIQUE (inventory_id, item_id)
) COMMENT '背包物品行：同背包同物品唯一，数量累加';

-- 装备槽（每个 user 每个 slot 唯一）
CREATE TABLE IF NOT EXISTS t_user_loadout (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY             COMMENT '装备行主键',
    user_id       BIGINT       NOT NULL                              COMMENT '所属用户ID',
    slot          VARCHAR(20)  NOT NULL                              COMMENT '槽位：WEAPON 法器 / ARMOR 护身 / STORAGE 储物 / TECHNIQUE 功法',
    item_id       BIGINT       NOT NULL                              COMMENT '装备物品定义ID',
    UNIQUE (user_id, slot)
) COMMENT '用户装备槽：每个用户每个槽位最多 1 件';

-- 物品种子（灵草 / 青竹剑 / 祖布囊）
INSERT INTO t_item (code, name, type, subtype, rarity, description, attrs_json, created_at)
SELECT 'LING_CAO',     '灵草',   'MATERIAL',  NULL,        'COMMON', '后山常见的灵草，服用可恢复少量气血，亦可留作作炼作原料。',           '{"consumable":true,"hpRestore":30}',         CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_item WHERE code = 'LING_CAO');

INSERT INTO t_item (code, name, type, subtype, rarity, description, attrs_json, created_at)
SELECT 'QING_ZHU_JIAN','青竹剑', 'EQUIPMENT', 'WEAPON',    'COMMON', '初学者佩剑，轻巧锋利。',                                          NULL,                                          CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_item WHERE code = 'QING_ZHU_JIAN');

INSERT INTO t_item (code, name, type, subtype, rarity, description, attrs_json, created_at)
SELECT 'ZU_BU_NANG',   '祖布囊', 'EQUIPMENT', 'STORAGE',   'COMMON', '祖传布袋，装备后背包容量 +5 格。',                                '{"capacityBonus":5}',                          CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_item WHERE code = 'ZU_BU_NANG');

-- ============================================================
-- M4 战斗系统
-- ============================================================

-- 怪物定义（每个怪物一种；通过 code 引用）
CREATE TABLE IF NOT EXISTS t_monster (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY             COMMENT '怪物主键',
    code          VARCHAR(50)  NOT NULL UNIQUE                         COMMENT '怪物 code',
    name          VARCHAR(50)  NOT NULL                               COMMENT '怪物名',
    level         INT          NOT NULL DEFAULT 1                      COMMENT '怪物等级（影响数值与掉率）',
    hp            INT          NOT NULL                               COMMENT '气血上限',
    attack        INT          NOT NULL                               COMMENT '攻击力',
    defense       INT          NOT NULL                               COMMENT '防御力',
    speed         INT          NOT NULL                               COMMENT '身法（出手顺序 + 玩家逃跑概率）',
    exp_reward    INT          NOT NULL DEFAULT 0                      COMMENT '胜利获得修为',
    drop_item_code VARCHAR(50)                                        COMMENT '胜利固定掉落物品 code（NULL 表示不掉）',
    drop_quantity INT          NOT NULL DEFAULT 1                      COMMENT '掉落数量',
    flee_blocked  INT          NOT NULL DEFAULT 0                      COMMENT '是否禁止逃跑：1 禁止 / 0 允许',
    description   VARCHAR(500)                                        COMMENT '描述',
    created_at    TIMESTAMP                                          COMMENT '创建时间'
) COMMENT '怪物定义表';

-- 战斗状态（一期每次移动触发，最多一条 ACTIVE；结束后转为 FINISHED）
CREATE TABLE IF NOT EXISTS t_combat (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY         COMMENT '战斗主键',
    user_id           BIGINT       NOT NULL                           COMMENT '所属用户ID',
    map_instance_id   BIGINT       NOT NULL                           COMMENT '所属地图实例ID',
    monster_code      VARCHAR(50)  NOT NULL                           COMMENT '怪物 code',
    monster_name      VARCHAR(50)  NOT NULL                           COMMENT '怪物名（冗余便于展示）',
    monster_hp        INT          NOT NULL                           COMMENT '怪物当前 HP',
    monster_max_hp    INT          NOT NULL                           COMMENT '怪物 HP 上限（冗余）',
    monster_attack    INT          NOT NULL                           COMMENT '怪物攻击力（冗余）',
    monster_defense   INT          NOT NULL                           COMMENT '怪物防御力（冗余）',
    monster_speed     INT          NOT NULL                           COMMENT '怪物速度（冗余）',
    player_hp         INT          NOT NULL                           COMMENT '玩家当前 HP（战斗临时，不直接回写 User）',
    player_hp_snapshot INT          NOT NULL                           COMMENT '进入战斗时玩家 HP 快照（结束结算用）',
    player_attack     INT          NOT NULL                           COMMENT '玩家攻击（进入战斗时快照）',
    player_defense    INT          NOT NULL                           COMMENT '玩家防御（进入战斗时快照）',
    player_speed      INT          NOT NULL                           COMMENT '玩家速度（进入战斗时快照）',
    round             INT          NOT NULL DEFAULT 1                  COMMENT '当前回合数',
    current_turn      VARCHAR(20)  NOT NULL DEFAULT 'PLAYER'           COMMENT '本回合行动方 PLAYER / MONSTER',
    log_json         VARCHAR(4000)                                    COMMENT '战斗日志 JSON 数组（每回合一条）',
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'           COMMENT '状态：ACTIVE 进行中 / FINISHED 已结束',
    result            VARCHAR(20)                                      COMMENT '结果：VICTORY 胜利 / DEFEAT 失败 / FLED 逃跑；FINISHED 后才有',
    created_at        TIMESTAMP                                       COMMENT '创建时间',
    updated_at        TIMESTAMP                                       COMMENT '更新时间'
) COMMENT '战斗状态表：一期只在玩家移动到 MONSTER 格时创建，结束后置为 FINISHED';

-- 战斗记录（每场战斗一行，仅作历史）
CREATE TABLE IF NOT EXISTS t_combat_log (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY         COMMENT '记录主键',
    user_id           BIGINT       NOT NULL                           COMMENT '所属用户ID',
    map_instance_id   BIGINT       NOT NULL                           COMMENT '所属地图实例ID',
    monster_code      VARCHAR(50)  NOT NULL                           COMMENT '怪物 code',
    monster_name      VARCHAR(50)  NOT NULL                           COMMENT '怪物名（冗余）',
    result            VARCHAR(20)  NOT NULL                           COMMENT '结果：VICTORY / DEFEAT / FLED',
    rounds            INT          NOT NULL                           COMMENT '回合数',
    exp_gained        INT          NOT NULL DEFAULT 0                  COMMENT '获得修为',
    dropped_item_code VARCHAR(50)                                     COMMENT '掉落物品 code（NULL = 没掉）',
    dropped_quantity  INT          NOT NULL DEFAULT 0                  COMMENT '掉落数量',
    created_at        TIMESTAMP                                       COMMENT '创建时间'
) COMMENT '战斗记录表：每场战斗一行，永久保留';

-- 怪物种子（一期只放后山妖鼠）
INSERT INTO t_monster (code, name, level, hp, attack, defense, speed, exp_reward, drop_item_code, drop_quantity, flee_blocked, description, created_at)
SELECT 'HOU_SHAN_YAO_SHU', '后山妖鼠', 3, 80, 12, 5, 8, 10, 'LING_CAO', 1, 0,
       '后山竹林常见的小妖，獠牙锋利但行动略显迟缓。练气初期可胜之。',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_monster WHERE code = 'HOU_SHAN_YAO_SHU');