package com.xiantu.service;

import com.xiantu.common.BizException;
import com.xiantu.entity.Item;
import com.xiantu.entity.UserLoadout;
import com.xiantu.mapper.ItemMapper;
import com.xiantu.mapper.UserLoadoutMapper;
import com.xiantu.web.dto.LoadoutSlotView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 装备槽服务：4 个固定槽位（WEAPON/ARMOR/STORAGE/TECHNIQUE）。
 *
 * <p>规则：
 * <ul>
 *   <li>背包里 EQUIPMENT 或 TECHNIQUE 类型物品可通过 {@link #equip} 装到对应 subtype 的槽</li>
 *   <li>装备时如果槽位已占，先把旧装备退回背包</li>
 *   <li>卸下时（{@link #unequip}）物品回到背包（容量满则报错）</li>
 * </ul>
 *
 * <p>本服务只依赖 {@link InventoryCoreService}（行级数据搬运），不再注入 InventoryService，
 * 从而避免与 InventoryService 的循环依赖。
 */
@Service
public class LoadoutService {

    private static final List<String> SLOTS = List.of(
            UserLoadout.SLOT_WEAPON,
            UserLoadout.SLOT_ARMOR,
            UserLoadout.SLOT_STORAGE,
            UserLoadout.SLOT_TECHNIQUE
    );

    private final UserLoadoutMapper loadoutMapper;
    private final InventoryCoreService inventoryCoreService;
    private final ItemMapper itemMapper;

    public LoadoutService(UserLoadoutMapper loadoutMapper,
                          InventoryCoreService inventoryCoreService,
                          ItemMapper itemMapper) {
        this.loadoutMapper = loadoutMapper;
        this.inventoryCoreService = inventoryCoreService;
        this.itemMapper = itemMapper;
    }

    /** 4 个槽位当前装备（空槽 item=null, empty=true）。 */
    public List<LoadoutSlotView> getLoadout(Long userId) {
        List<UserLoadout> rows = loadoutMapper.selectByUserId(userId);
        // 用 slot 索引
        Map<String, UserLoadout> bySlot = new HashMap<>();
        for (UserLoadout r : rows) bySlot.put(r.getSlot(), r);

        List<LoadoutSlotView> out = new ArrayList<>();
        for (String slot : SLOTS) {
            LoadoutSlotView v = new LoadoutSlotView();
            v.setSlot(slot);
            v.setSlotName(UserLoadout.slotName(slot));
            UserLoadout r = bySlot.get(slot);
            if (r == null) {
                v.setEmpty(true);
            } else {
                Item item = itemMapper.selectById(r.getItemId());
                v.setEmpty(false);
                v.setItemId(item.getId());
                v.setCode(item.getCode());
                v.setName(item.getName());
                v.setRarity(item.getRarity());
                v.setDescription(item.getDescription());
            }
            out.add(v);
        }
        return out;
    }

    /**
     * 把背包里的某件物品装到对应槽位。自动处理旧装备退回。
     *
     * <p>事务顺序：先扣背包 1 个新装备（数量不足抛 BizException 整体回滚）→ 再退回旧装备 +
     * 删旧 loadout 行 → 最后写新 loadout 行。任何中间步骤失败外层事务都会回滚，
     * 背包 + 装备槽状态保持一致。
     */
    @Transactional
    public void equip(Long userId, String itemCode) {
        Item item = itemMapper.selectByCode(itemCode);
        if (item == null) throw new BizException("未知道具: " + itemCode);

        // 仅 EQUIPMENT / TECHNIQUE 可装备
        if (!Item.TYPE_EQUIPMENT.equals(item.getType()) && !Item.TYPE_TECHNIQUE.equals(item.getType())) {
            throw new BizException("「" + item.getName() + "」不是装备");
        }

        String slot = slotForSubtype(item.getSubtype());
        if (slot == null) {
            throw new BizException("「" + item.getName() + "」没有对应装备槽");
        }

        // 1. 先扣背包 1 个新装备（数量不足抛 BizException，整体事务回滚）
        inventoryCoreService.removeItem(userId, itemCode, 1);

        // 2. 如果槽位已有物品，先退回背包 + 删旧 loadout 行
        UserLoadout existing = loadoutMapper.selectByUserAndSlot(userId, slot);
        if (existing != null) {
            Item old = itemMapper.selectById(existing.getItemId());
            // addItem 容量满则抛 BizException；外层事务回滚，刚刚 removeItem 的也会恢复
            inventoryCoreService.addItem(userId, old.getCode(), 1);
            loadoutMapper.deleteById(existing.getId());
        }

        // 3. 写入新装备槽
        UserLoadout nl = new UserLoadout();
        nl.setUserId(userId);
        nl.setSlot(slot);
        nl.setItemId(item.getId());
        loadoutMapper.insert(nl);
    }

    /** 卸下指定槽位的装备：放回背包。 */
    @Transactional
    public void unequip(Long userId, String slot) {
        UserLoadout existing = loadoutMapper.selectByUserAndSlot(userId, slot);
        if (existing == null) {
            throw new BizException("该槽位本就空着");
        }
        Item item = itemMapper.selectById(existing.getItemId());
        // 退回背包（addItem 容量满时抛 BizException，外层事务整体回滚）
        inventoryCoreService.addItem(userId, item.getCode(), 1);
        loadoutMapper.deleteById(existing.getId());
    }

    /** 给定物品 subtype 返回对应槽位 code；不匹配返回 null。 */
    public static String slotForSubtype(String subtype) {
        if (subtype == null) return null;
        switch (subtype) {
            case Item.SUBTYPE_WEAPON:    return UserLoadout.SLOT_WEAPON;
            case Item.SUBTYPE_ARMOR:     return UserLoadout.SLOT_ARMOR;
            case Item.SUBTYPE_STORAGE:   return UserLoadout.SLOT_STORAGE;
            case Item.SUBTYPE_TECHNIQUE: return UserLoadout.SLOT_TECHNIQUE;
            default: return null;
        }
    }

    /** 注册时给新用户装上初始装备（青竹剑 → 法器、祖布囊 → 储物）。 */
    @Transactional
    public void equipStarterLoadout(Long userId) {
        equip(userId, "QING_ZHU_JIAN");
        equip(userId, "ZU_BU_NANG");
    }

    /** 给前端用：当前槽位 code 列表。 */
    public static List<String> slotCodes() {
        return SLOTS;
    }
}