<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../api/http'

const emit = defineEmits(['back'])

const loading = ref(true)
const error = ref('')
const inv = ref(null)             // 背包视图
const filter = ref('ALL')         // ALL / MATERIAL / EQUIPMENT / PILL / TECHNIQUE
const notice = ref('')            // 操作反馈
const selectedCode = ref(null)    // 选中的物品 code
const busy = ref('')              // 当前正在进行的操作（防双击）

const TYPE_LABELS = {
  ALL: '全部', MATERIAL: '材料', EQUIPMENT: '装备', PILL: '丹药', TECHNIQUE: '功法'
}
const RARITY_LABELS = {
  COMMON: '凡品', RARE: '灵品', EPIC: '宝品', LEGENDARY: '仙品'
}

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.get('/inventory')
    inv.value = data.data
    console.info('[Inventory] 加载成功:', inv.value)
    // 选中项若已不存在则清掉
    if (selectedCode.value && !inv.value.items.some(i => i.code === selectedCode.value)) {
      selectedCode.value = null
    }
  } catch (e) {
    console.error('[Inventory] 加载失败:', {
      status: e.response?.status,
      message: e.response?.data?.message || e.message,
      path: e.config?.url
    })
    error.value = (e.response?.data?.message || e.message || '加载背包失败')
      + (e.response?.status ? `（HTTP ${e.response.status}）` : '')
  } finally {
    loading.value = false
  }
}

// ---- 容量 ----
const totalCapacity = computed(() => inv.value ? inv.value.capacity + (inv.value.capacityBonus || 0) : 0)
const usedPct = computed(() => {
  if (!inv.value || totalCapacity.value === 0) return '0%'
  return Math.min(100, (inv.value.used / totalCapacity.value) * 100) + '%'
})

// ---- 已装 code 集合（用来在格子上打「佩」标记 + 在详情里切换「装备/卸下」按钮）----
const equippedCodes = computed(() => {
  if (!inv.value) return new Set()
  return new Set(inv.value.loadout.filter(s => !s.empty).map(s => s.code))
})

// ---- 行囊 items（应用筛选）----
const filteredItems = computed(() => {
  if (!inv.value) return []
  if (filter.value === 'ALL') return inv.value.items
  return inv.value.items.filter(it => it.type === filter.value)
})

// 行囊格子：已装 item + 空格子补足到总容量；总容量 = 基础 + 装备加成（祖布囊 +5 等）
const cells = computed(() => {
  if (!inv.value) return []
  const filled = filteredItems.value.map(i => ({ item: i, empty: false }))
  const totalSlots = inv.value.capacity + (inv.value.capacityBonus || 0)
  const pad = Math.max(0, totalSlots - inv.value.used)
  return filled.concat(Array.from({ length: pad }, () => ({ item: null, empty: true })))
})

// ---- 选中详情 ----
const selected = computed(() => {
  if (!selectedCode.value || !inv.value) return null
  return inv.value.items.find(i => i.code === selectedCode.value) || null
})

const isPill = (i) => i && i.type === 'PILL'
const isEquip = (i) => i && (i.type === 'EQUIPMENT' || i.type === 'TECHNIQUE')

// 效果文本（仿 xiuxian BagView.effectText）
function effectText(it) {
  const parts = []
  if (it.capacityBonus > 0) parts.push(`储物格数 +${it.capacityBonus}`)
  if (it.hpRestore > 0) parts.push(`服用回血 +${it.hpRestore}`)
  return parts.length ? parts.join(' · ') : '暂无附加之效'
}

function pick(cell) {
  if (cell.empty) return
  selectedCode.value = selectedCode.value === cell.item.code ? null : cell.item.code
  notice.value = ''
  error.value = ''
}

async function act(fn, successMsg) {
  if (busy.value) return
  busy.value = '1'
  error.value = ''
  notice.value = ''
  try {
    await fn()
    notice.value = successMsg || '操作成功'
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || '操作失败'
  } finally {
    busy.value = ''
  }
}

async function useItem(it) {
  await act(() => http.post('/inventory/use', { itemCode: it.code }),
            `服用「${it.name}」+气血 ${it.hpRestore}`)
}

async function equipItem(it) {
  await act(() => http.post('/inventory/equip', { itemCode: it.code }),
            `已装备「${it.name}」`)
}

async function unequipBySlot(slot) {
  await act(() => http.post('/inventory/unequip', { slot }),
            `已卸下，回到行囊`)
}

async function unequipFromDetail(it) {
  const slot = inv.value.loadout.find(s => !s.empty && s.code === it.code)
  if (slot) await unequipBySlot(slot.slot)
}

function rarityClass(r) { return 'r-' + (r || 'COMMON').toLowerCase() }
</script>

<template>
  <div class="bag">
    <header class="bag-header">
      <button class="btn ghost" @click="emit('back')">← 返回主页</button>
      <span class="title">我的行囊</span>
    </header>

    <p v-if="loading" class="tip">正在打开储物法宝…</p>
    <section v-else-if="error" class="panel error-panel">
      <div class="ep-mark">!</div>
      <h3 class="ep-title">背包加载失败</h3>
      <p class="ep-msg">{{ error }}</p>
      <p class="ep-hint">
        可能原因：后端未启动 / 改完代码未重启 / token 过期。
        请到浏览器开发者工具（F12 → Console）查看详细日志，然后 <button class="btn ghost small" @click="load">重试</button>。
      </p>
    </section>

    <template v-else-if="inv">
      <!-- ============ 储物空间 ============ -->
      <section class="panel">
        <div class="cap-head">
          <h3>储物空间</h3>
          <span class="cap-num">{{ inv.used }} / {{ totalCapacity }}</span>
        </div>
        <div class="bar"><div class="fill" :style="{ width: usedPct }"></div></div>
        <div class="cap-detail">
          <span>基础 {{ inv.capacity }} 格</span>
          <span v-if="inv.capacityBonus > 0" class="bonus">装备加成 +{{ inv.capacityBonus }} 格</span>
          <span v-else class="muted">尚无储物法宝加成</span>
        </div>
      </section>

      <!-- ============ 已佩戴 ============ -->
      <section class="panel">
        <h3>已佩戴</h3>
        <div class="slots">
          <div v-for="s in inv.loadout" :key="s.slot" class="slot" :class="{ filled: !s.empty }">
            <div class="slot-label">{{ s.slotName }}</div>
            <div class="slot-body">
              <template v-if="!s.empty">
                <span class="slot-name" :class="rarityClass(s.rarity)">{{ s.name }}</span>
                <button class="mini" :disabled="busy" @click="unequipBySlot(s.slot)">卸下</button>
              </template>
              <span v-else class="slot-empty">空</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ 行囊 ============ -->
      <section class="panel">
        <div class="bag-head">
          <h3>行囊</h3>
          <nav class="filters">
            <button v-for="t in ['ALL','MATERIAL','EQUIPMENT','PILL','TECHNIQUE']" :key="t"
                    class="chip" :class="{ active: filter === t }" @click="filter = t">
              {{ TYPE_LABELS[t] }}
            </button>
          </nav>
        </div>
        <p v-if="inv.items.length === 0" class="tip">行囊空空，去秘境采些灵草吧。</p>
        <div v-else class="grid">
          <div
            v-for="(cell, i) in cells"
            :key="i"
            class="cell"
            :class="{
              empty: cell.empty,
              active: !cell.empty && cell.item.code === selectedCode,
              worn: !cell.empty && equippedCodes.has(cell.item.code)
            }"
            @click="pick(cell)"
          >
            <template v-if="!cell.empty">
              <span class="glyph" :class="rarityClass(cell.item.rarity)">{{ cell.item.name.charAt(0) }}</span>
              <span v-if="cell.item.quantity > 1" class="badge">{{ cell.item.quantity }}</span>
              <span v-if="equippedCodes.has(cell.item.code)" class="worn-dot">佩</span>
            </template>
          </div>
        </div>
      </section>

      <!-- ============ 选中详情 ============ -->
      <section v-if="selected" class="panel detail">
        <div class="d-head">
          <span class="glyph big" :class="rarityClass(selected.rarity)">{{ selected.name.charAt(0) }}</span>
          <div>
            <div class="d-name">
              {{ selected.name }}
              <span class="rarity" :class="rarityClass(selected.rarity)">{{ RARITY_LABELS[selected.rarity] || selected.rarity }}</span>
            </div>
            <div class="d-sub">
              {{ TYPE_LABELS[selected.type] || selected.type }}
              <template v-if="selected.quantity > 1"> · 持有 {{ selected.quantity }}</template>
            </div>
          </div>
        </div>
        <p class="d-desc">{{ selected.description }}</p>
        <div class="d-effect">{{ effectText(selected) }}</div>
        <div class="d-actions">
          <button v-if="isPill(selected) && selected.consumable"
                  class="btn primary" :disabled="busy" @click="useItem(selected)">服用</button>
          <button v-if="isEquip(selected) && !equippedCodes.has(selected.code)"
                  class="btn" :disabled="busy" @click="equipItem(selected)">装备</button>
          <button v-if="isEquip(selected) && equippedCodes.has(selected.code)"
                  class="btn" :disabled="busy" @click="unequipFromDetail(selected)">卸下</button>
        </div>
      </section>

      <p v-if="notice" class="notice">{{ notice }}</p>
      <button class="btn refresh" @click="load">整理行囊</button>
    </template>
  </div>
</template>

<style scoped>
.bag { animation: inkIn .5s ease-out both; }
.tip { text-align: center; color: var(--ink-light); font-size: 14px; padding: 18px 0; }
.error { color: var(--seal); text-align: center; margin-top: 16px; font-size: 14px; padding: 8px 14px; }
.error-panel {
  text-align: center; padding: 26px 22px;
  border-color: var(--seal); background: rgba(158,59,52,.06);
}
.error-panel .ep-mark {
  display: inline-flex; align-items: center; justify-content: center;
  width: 44px; height: 44px; border: 2px solid var(--seal); color: var(--seal);
  font-size: 26px; font-weight: 700; border-radius: 6px;
  background: rgba(158,59,52,.08); transform: rotate(-3deg);
  margin-bottom: 10px;
}
.error-panel .ep-title { margin: 4px 0 8px; font-size: 16px; letter-spacing: 3px; color: var(--seal); }
.error-panel .ep-msg { margin: 0 auto 12px; color: var(--ink); font-size: 13px; max-width: 460px; }
.error-panel .ep-hint { margin: 0 auto; color: var(--ink-soft); font-size: 12px; line-height: 1.8; max-width: 480px; }
.error-panel .ep-hint .btn { margin-left: 4px; }
.notice {
  text-align: center; color: var(--ink); font-size: 14px; letter-spacing: 1px;
  background: rgba(158,59,52,.08); border: 1px dashed var(--seal);
  border-radius: 4px; padding: 10px; margin-bottom: 14px;
}

/* 顶栏 */
.bag-header {
  display: flex; align-items: center; gap: 12px; margin-bottom: 14px;
}
.bag-header .title {
  font-size: 17px; font-weight: 700; color: var(--ink); letter-spacing: 4px;
}

/* panel 通用（仿 xiuxian BagView） */
.panel {
  position: relative; overflow: hidden; background: rgba(255,255,255,.55);
  border: 1px solid var(--line); border-radius: 4px; padding: 20px 22px;
  box-shadow: 0 2px 18px rgba(31,29,26,.06); backdrop-filter: blur(2px);
  margin-bottom: 16px;
}
.panel::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(ellipse 60% 45% at 16% 0%, rgba(31,29,26,.05), transparent 62%);
}
.panel > * { position: relative; }
h3 {
  margin: 0 0 14px; font-size: 15px; color: var(--ink); letter-spacing: 2px;
  border-left: 3px solid var(--ink); padding-left: 10px; line-height: 1.2;
}

/* 容量 */
.cap-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 10px; }
.cap-head h3 { margin-bottom: 0; }
.cap-num { font-size: 18px; font-weight: 700; color: var(--ink); font-variant-numeric: tabular-nums; }
.bar { height: 8px; border-radius: 2px; background: #e3ded2; overflow: hidden; }
.bar .fill { height: 100%; background: linear-gradient(90deg, #6b665d, var(--ink)); transition: width .4s ease; }
.cap-detail {
  display: flex; gap: 14px; flex-wrap: wrap;
  color: var(--ink-soft); font-size: 12px; margin-top: 10px; letter-spacing: 1px;
}
.cap-detail .bonus { color: var(--seal); font-weight: 600; }
.cap-detail .muted { color: var(--ink-light); }

/* 装备槽（2 列） */
.slots { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.slot {
  border: 1px dashed var(--line); border-radius: 4px; padding: 10px 12px;
  background: rgba(255,255,255,.4); transition: border-color .2s;
}
.slot.filled { border-style: solid; border-color: var(--ink-soft); }
.slot-label { font-size: 12px; color: var(--ink-light); letter-spacing: 2px; }
.slot-body {
  display: flex; align-items: center; justify-content: space-between;
  gap: 8px; margin-top: 4px;
}
.slot-name { font-size: 14px; font-weight: 600; letter-spacing: 1px; }
.slot-empty { font-size: 13px; color: var(--ink-light); }
.mini {
  border: 1px solid var(--line); background: transparent; color: var(--ink-light);
  border-radius: 3px; font-size: 12px; padding: 2px 8px; font-family: inherit;
  transition: all .2s; letter-spacing: 1px; cursor: pointer;
}
.mini:hover:not(:disabled) { border-color: var(--ink); color: var(--ink); }
.mini:disabled { opacity: .5; cursor: not-allowed; }

/* 行囊头：标题 + 筛选 */
.bag-head {
  display: flex; align-items: center; justify-content: space-between;
  flex-wrap: wrap; gap: 10px; margin-bottom: 12px;
}
.bag-head h3 { margin: 0; }
.filters { display: flex; gap: 6px; flex-wrap: wrap; }
.chip {
  background: rgba(255,255,255,.5); border: 1px solid var(--line);
  padding: 3px 10px; font-size: 12px; border-radius: 14px;
  color: var(--ink-light); font-family: inherit; letter-spacing: 1px;
  cursor: pointer; transition: all .2s;
}
.chip:hover { color: var(--ink); border-color: var(--ink); }
.chip.active { background: var(--seal); border-color: var(--seal); color: #fff; }

/* 行囊格子 */
.grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 8px; }
.cell {
  position: relative; aspect-ratio: 1;
  border: 1px solid var(--line); border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; background: rgba(255,255,255,.5); transition: all .18s;
}
.cell.empty { background: rgba(31,29,26,.03); border-style: dashed; cursor: default; }
.cell:hover:not(.empty) { border-color: var(--ink-soft); transform: translateY(-2px); }
.cell.active { border-color: var(--seal); box-shadow: 0 0 0 2px rgba(158,59,52,.12); }
.cell.worn { background: rgba(158,59,52,.06); }
.glyph { font-size: 20px; font-weight: 700; }
.badge {
  position: absolute; right: 2px; bottom: 1px; font-size: 11px; font-weight: 700;
  color: var(--ink); font-variant-numeric: tabular-nums;
}
.worn-dot {
  position: absolute; left: 3px; top: 2px; font-size: 10px; color: #fff;
  background: var(--seal); border-radius: 2px; padding: 0 3px;
  letter-spacing: 0; font-weight: 700;
}

/* 详情 */
.detail { margin-top: 4px; }
.d-head { display: flex; align-items: center; gap: 14px; }
.glyph.big {
  width: 46px; height: 46px; flex: none; display: flex; align-items: center; justify-content: center;
  border: 2px solid var(--ink-soft); border-radius: 4px; font-size: 22px; transform: rotate(-3deg);
}
.d-name { font-size: 17px; font-weight: 700; letter-spacing: 2px; }
.d-sub { font-size: 12px; color: var(--ink-light); margin-top: 4px; letter-spacing: 1px; }
.d-desc { color: var(--ink-soft); font-size: 13px; line-height: 1.7; margin: 14px 0 8px; }
.d-effect { color: var(--seal); font-size: 13px; letter-spacing: 1px; font-weight: 600; }
.d-actions { display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap; }

/* 按钮 */
.btn {
  border: 1px solid var(--ink); border-radius: 4px; padding: 9px 18px; font-size: 14px;
  background: transparent; color: var(--ink); font-family: inherit; transition: all .2s;
  cursor: pointer; letter-spacing: 1px;
}
.btn:hover:not(:disabled) { background: var(--ink); color: #fff; }
.btn.primary { background: var(--seal); border-color: var(--seal); color: #fff; }
.btn.primary:hover:not(:disabled) { background: #8a312b; border-color: #8a312b; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn.ghost { border: 1px solid var(--line); background: transparent; color: var(--ink-light); padding: 6px 14px; font-size: 12px; border-radius: 3px; }
.btn.ghost:hover:not(:disabled) { color: var(--ink); border-color: var(--ink); background: transparent; }
.refresh { display: block; margin: 0 auto; letter-spacing: 2px; }

/* 品阶配色（仿 xiuxian：墨色 + 青色 + 蓝色 + 紫色 + 朱砂） */
.rarity { font-size: 12px; margin-left: 6px; padding: 1px 6px; border-radius: 2px; border: 1px solid currentColor; }
.r-common { color: #6b665d; }
.r-rare { color: #4a7c6f; }
.r-epic { color: #3f6a9e; }
.r-legendary { color: #7a5aa0; }

@media (max-width: 520px) {
  .slots { grid-template-columns: 1fr; }
  .grid { grid-template-columns: repeat(5, 1fr); }
}
</style>