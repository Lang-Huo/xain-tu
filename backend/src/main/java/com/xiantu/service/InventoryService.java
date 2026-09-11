package com.xiantu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiantu.common.BizException;
import com.xiantu.entity.Inventory;
import com.xiantu.entity.InventoryItem;
import com.xiantu.entity.Item;
import com.xiantu.entity.User;
import com.xiantu.mapper.ItemMapper;
import com.xiantu.mapper.UserMapper;
import com.xiantu.web.dto.InventoryItemView;
import com.xiantu.web.dto.InventoryView;
import com.xiantu.web.dto.UseItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 背包业务编排：背包视图（含装备槽 + 容量加成）+ 物品使用（消耗品回血）。
 *
 * <p>行级别的数据搬运（getOrCreateInventory / addItem / removeItem / listItems）已下沉到
 * {@link InventoryCoreService}，本类不再直接持有 InventoryMapper / InventoryItemMapper，
 * 从而打破与 LoadoutService 的循环依赖。
 */
@Service
public class InventoryService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InventoryCoreService inventoryCoreService;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final LoadoutService loadoutService;

    public InventoryService(InventoryCoreService inventoryCoreService,
                            ItemMapper itemMapper,
                            UserMapper userMapper,
                            LoadoutService loadoutService) {
        this.inventoryCoreService = inventoryCoreService;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.loadoutService = loadoutService;
    }

    /** 背包完整视图。 */
    public InventoryView view(Long userId) {
        Inventory inv = inventoryCoreService.getOrCreateInventory(userId);
        List<InventoryItem> rows = inventoryCoreService.listItems(userId);
        List<InventoryItemView> views = new ArrayList<>();
        for (InventoryItem row : rows) {
            Item item = itemMapper.selectById(row.getItemId());
            if (item == null) continue;
            views.add(toItemView(item, row.getQuantity()));
        }
        // 装备槽
        java.util.List<com.xiantu.web.dto.LoadoutSlotView> loadout = loadoutService.getLoadout(userId);
        // 容量加成（来自储物类装备的 attrs_json.capacityBonus）
        int capacityBonus = 0;
        for (com.xiantu.web.dto.LoadoutSlotView slot : loadout) {
            if (slot.isEmpty()) continue;
            Item it = itemMapper.selectByCode(slot.getCode());
            if (it == null) continue;
            Attrs a = parseAttrs(it.getAttrsJson());
            capacityBonus += a.capacityBonus;
        }
        InventoryView v = new InventoryView();
        v.setCapacity(inv.getCapacity());
        v.setUsed(views.size());
        v.setItems(views);
        v.setLoadout(loadout);
        v.setCapacityBonus(capacityBonus);
        return v;
    }

    /**
     * 使用背包里的物品 1 个：扣数量并应用效果（当前支持 hpRestore）。
     * 非 consumable 物品直接报错。
     */
    @Transactional
    public UseItemResponse useItem(Long userId, String itemCode) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");

        Item item = itemMapper.selectByCode(itemCode);
        if (item == null) throw new BizException("未知道具: " + itemCode);

        Attrs attrs = parseAttrs(item.getAttrsJson());
        if (!attrs.consumable) throw new BizException("「" + item.getName() + "」不可直接使用");

        // 取当前数量（用于返回 quantityAfter）
        int qtyBefore = inventoryCoreService.listItems(userId).stream()
                .filter(r -> r.getItemId().equals(item.getId()))
                .mapToInt(InventoryItem::getQuantity)
                .findFirst().orElse(0);
        if (qtyBefore <= 0) {
            throw new BizException("背包中没有「" + item.getName() + "」");
        }

        // 应用效果
        int hpBefore = user.getHp();
        int hpAfter = Math.min(user.getMaxHp(), hpBefore + attrs.hpRestore);
        user.setHp(hpAfter);
        userMapper.updateById(user);

        // 扣 1 个（内部已校验数量并 delete/update + 更新背包 updatedAt）
        inventoryCoreService.removeItem(userId, itemCode, 1);

        long usedAfter = inventoryCoreService.listItems(userId).size();
        Inventory inv = inventoryCoreService.getOrCreateInventory(userId);

        UseItemResponse r = new UseItemResponse();
        r.setSuccess(true);
        r.setMessage("服用「" + item.getName() + "」+气血 " + (hpAfter - hpBefore));
        r.setHpBefore(hpBefore);
        r.setHpAfter(hpAfter);
        r.setHpChange(hpAfter - hpBefore);
        r.setQuantityAfter(qtyBefore - 1);
        r.setUsedAfter((int) usedAfter);
        r.setCapacity(inv.getCapacity());
        return r;
    }

    // ---------- helpers ----------

    /** 按 username 查 userId（供 Controller 简单桥接用，避免 Controller 直接依赖 UserMapper）。 */
    public Long resolveUserId(String username) {
        User u = userMapper.selectByUsername(username);
        if (u == null) throw new BizException("用户不存在");
        return u.getId();
    }

    private InventoryItemView toItemView(Item item, int quantity) {
        Attrs attrs = parseAttrs(item.getAttrsJson());
        InventoryItemView v = new InventoryItemView();
        v.setItemId(item.getId());
        v.setCode(item.getCode());
        v.setName(item.getName());
        v.setType(item.getType());
        v.setSubtype(item.getSubtype());
        v.setRarity(item.getRarity());
        v.setDescription(item.getDescription());
        v.setAttrsJson(item.getAttrsJson());
        v.setConsumable(attrs.consumable);
        v.setHpRestore(attrs.hpRestore);
        v.setCapacityBonus(attrs.capacityBonus);
        v.setQuantity(quantity);
        return v;
    }

    private Attrs parseAttrs(String json) {
        Attrs a = new Attrs();
        if (json == null || json.isEmpty()) return a;
        try {
            JsonNode node = MAPPER.readTree(json);
            a.consumable = node.path("consumable").asBoolean(false);
            a.hpRestore = node.path("hpRestore").asInt(0);
            a.capacityBonus = node.path("capacityBonus").asInt(0);
        } catch (Exception e) {
            // 容错：解析失败按默认
        }
        return a;
    }

    /** 物品 attrs 解析结果（私有小结构，避免每次造 JsonNode）。 */
    private static class Attrs {
        boolean consumable = false;
        int hpRestore = 0;
        int capacityBonus = 0;
    }
}