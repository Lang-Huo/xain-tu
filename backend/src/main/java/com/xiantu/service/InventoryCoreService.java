package com.xiantu.service;

import com.xiantu.common.BizException;
import com.xiantu.entity.Inventory;
import com.xiantu.entity.InventoryItem;
import com.xiantu.entity.Item;
import com.xiantu.mapper.InventoryItemMapper;
import com.xiantu.mapper.InventoryMapper;
import com.xiantu.mapper.ItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 背包核心数据操作：从原 {@link InventoryService} 拆出来，给 LoadoutService 也复用，
 * 避免 InventoryService ↔ LoadoutService 的循环依赖。
 *
 * <p>本类只做"行级别"的数据搬运（取/建背包、增删物品行、列原始行），不带 View/DTO 转换，
 * 不读装备槽，不解析 attrsJson。
 */
@Service
public class InventoryCoreService {

    private final InventoryMapper inventoryMapper;
    private final InventoryItemMapper inventoryItemMapper;
    private final ItemMapper itemMapper;

    public InventoryCoreService(InventoryMapper inventoryMapper,
                               InventoryItemMapper inventoryItemMapper,
                               ItemMapper itemMapper) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryItemMapper = inventoryItemMapper;
        this.itemMapper = itemMapper;
    }

    /** 取或建用户背包。 */
    @Transactional
    public Inventory getOrCreateInventory(Long userId) {
        Inventory inv = inventoryMapper.selectByUserId(userId);
        if (inv != null) return inv;
        inv = new Inventory();
        inv.setUserId(userId);
        inv.setCapacity(30);
        LocalDateTime now = LocalDateTime.now();
        inv.setCreatedAt(now);
        inv.setUpdatedAt(now);
        inventoryMapper.insert(inv);
        return inv;
    }

    /**
     * 给用户背包加物品。同种累加；新品种占新格，超过 capacity 抛异常。
     * 主要给 MapService 采集资源 / LoadoutService 退回旧装备时调用。
     */
    @Transactional
    public void addItem(Long userId, String itemCode, int quantity) {
        if (quantity <= 0) return;
        Item item = itemMapper.selectByCode(itemCode);
        if (item == null) throw new BizException("未知道具: " + itemCode);
        Inventory inv = getOrCreateInventory(userId);
        InventoryItem row = inventoryItemMapper.selectByInventoryAndItem(inv.getId(), item.getId());
        if (row != null) {
            row.setQuantity(row.getQuantity() + quantity);
            inventoryItemMapper.updateById(row);
        } else {
            long used = inventoryItemMapper.countByInventoryId(inv.getId());
            if (used >= inv.getCapacity()) {
                throw new BizException("背包已满（" + inv.getCapacity() + " 格），无法拾取更多种类");
            }
            InventoryItem ni = new InventoryItem();
            ni.setInventoryId(inv.getId());
            ni.setItemId(item.getId());
            ni.setQuantity(quantity);
            inventoryItemMapper.insert(ni);
        }
        inv.setUpdatedAt(LocalDateTime.now());
        inventoryMapper.updateById(inv);
    }

    /**
     * 从背包移除物品 quantity 个（用于装备扣 1 等）。
     * 数量不足抛 BizException；扣到 0 删行。
     */
    @Transactional
    public void removeItem(Long userId, String itemCode, int quantity) {
        if (quantity <= 0) return;
        Item item = itemMapper.selectByCode(itemCode);
        if (item == null) throw new BizException("未知道具: " + itemCode);
        Inventory inv = getOrCreateInventory(userId);
        InventoryItem row = inventoryItemMapper.selectByInventoryAndItem(inv.getId(), item.getId());
        if (row == null || row.getQuantity() < quantity) {
            throw new BizException("背包中「" + item.getName() + "」数量不足");
        }
        row.setQuantity(row.getQuantity() - quantity);
        if (row.getQuantity() <= 0) {
            inventoryItemMapper.deleteById(row.getId());
        } else {
            inventoryItemMapper.updateById(row);
        }
        inv.setUpdatedAt(LocalDateTime.now());
        inventoryMapper.updateById(inv);
    }

    /** 列出背包内全部行（含 quantity），背包不存在时返回空列表。 */
    public List<InventoryItem> listItems(Long userId) {
        Inventory inv = inventoryMapper.selectByUserId(userId);
        if (inv == null) return Collections.emptyList();
        return inventoryItemMapper.selectByInventoryId(inv.getId());
    }
}