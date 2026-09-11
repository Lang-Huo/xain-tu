<script setup>
// Home.vue 版本戳 v2：装备槽可点击 + 详情面板 subtype + equippedSlotOf + 后端 InventoryItemView.subtype
// 如果 F12 Console 看不到下面的 BUILD_TAG，请 Ctrl+Shift+R 强制刷新（清 Vite HMR 缓存）
const BUILD_TAG = 'HOME-V2-2026-09-11T18:15'
console.info('[Home] build tag:', BUILD_TAG)
import { ref, computed, onMounted } from 'vue'
import http from '../api/http'

const emit = defineEmits(['logout', 'goTest', 'enterMap', 'goInventory'])
const user = ref(null)
const loading = ref(true)
const error = ref('')
const tab = ref('me')   // 'me' | 'map'

// -------- 我的（profile）相关 --------
const rootList = computed(() => user.value?.spiritRoots || [])
const rootNames = computed(() => rootList.value.map(r => r.name).join(' · '))
const rootDesc = computed(() => {
  const list = rootList.value
  if (list.length === 0) return '尚未测灵根，前往测灵根以觉醒属性。'
  if (list.length === 1) return '已觉醒「' + list[0].name + '」灵根。'
  return '已觉醒 ' + list.length + ' 灵根之体：' + rootNames.value + '。'
})
const CHINESE_NUMS = ['', '一', '二', '三', '四', '五', '六', '七', '八']
const rootsSummary = computed(() => {
  const list = rootList.value
  const n = list.length
  if (n === 0) return ''
  if (n === 1) return list[0].name + '灵根'
  if (n === 2) return list.map(r => r.name).join('') + '双灵根'
  return (CHINESE_NUMS[n] || String(n)) + '灵根'
})
const initial = computed(() => (user.value?.nickname || user.value?.username || '修').charAt(0))
const hpPct = computed(() =>
  user.value ? Math.max(0, Math.min(100, (user.value.hp / user.value.maxHp) * 100)) : 0
)
const manaPct = computed(() =>
  user.value ? Math.max(0, Math.min(100, (user.value.mana / user.value.maxMana) * 100)) : 0
)

// -------- 探索秘境相关 --------
const templates = ref([])
const activeInst = ref(null)
const mapLoading = ref(false)
const mapError = ref('')
const mapLoaded = ref(false)

// -------- 行囊（背包内联）相关 --------
const inv = ref(null)             // 背包视图
const invLoading = ref(true)
const invError = ref('')
const filter = ref('ALL')
const selectedCode = ref(null)
const busy = ref(false)
const notice = ref('')

const TYPE_LABELS = {
  ALL: '全部', MATERIAL: '材料', EQUIPMENT: '装备', PILL: '丹药', TECHNIQUE: '功法'
}
const RARITY_LABELS = {
  COMMON: '凡品', RARE: '灵品', EPIC: '宝品', LEGENDARY: '仙品'
}

async function loadInv() {
  invLoading.value = true
  invError.value = ''
  try {
    const { data } = await http.get('/inventory')
    console.info('[Home→行囊] /inventory 响应:', data)
    if (!data || !data.data) {
      throw new Error('响应缺少 data 字段：' + JSON.stringify(data))
    }
    if (!Array.isArray(data.data.items) || !Array.isArray(data.data.loadout)) {
      throw new Error('响应结构异常，items/loadout 不是数组：' + JSON.stringify(data.data))
    }
    inv.value = data.data
    if (selectedCode.value && !inv.value.items.some(i => i.code === selectedCode.value)) {
      selectedCode.value = null
    }
  } catch (e) {
    console.error('[Home→行囊] 加载失败:', e.response?.status, e.response?.data?.message || e.message)
    invError.value = (e.response?.data?.message || e.message || '加载背包失败')
      + (e.response?.status ? `（HTTP ${e.response.status}）` : '')
  } finally {
    invLoading.value = false
  }
}

const totalCapacity = computed(() => inv.value ? inv.value.capacity + (inv.value.capacityBonus || 0) : 0)
const usedPct = computed(() => {
  if (!inv.value || totalCapacity.value === 0) return '0%'
  return Math.min(100, (inv.value.used / totalCapacity.value) * 100) + '%'
})
const equippedCodes = computed(() => {
  if (!inv.value) return new Set()
  return new Set(inv.value.loadout.filter(s => !s.empty).map(s => s.code))
})
const filteredItems = computed(() => {
  if (!inv.value) return []
  if (filter.value === 'ALL') return inv.value.items
  return inv.value.items.filter(it => it.type === filter.value)
})
const cells = computed(() => {
  if (!inv.value) return []
  const filled = filteredItems.value.map(i => ({ item: i, empty: false }))
  const totalSlots = inv.value.capacity + (inv.value.capacityBonus || 0)
  const pad = Math.max(0, totalSlots - inv.value.used)
  return filled.concat(Array.from({ length: pad }, () => ({ item: null, empty: true })))
})
const selected = computed(() => {
  if (!selectedCode.value || !inv.value) return null
  // 先从背包找（行囊选中的物品）
  const inBag = inv.value.items.find(i => i.code === selectedCode.value)
  if (inBag) return inBag
  // 找不到时尝试从 loadout 找（点了装备槽里的物品，但行囊里没有这件）
  const slot = inv.value.loadout.find(s => !s.empty && s.code === selectedCode.value)
  if (slot) {
    // 构造一个伪物品对象给详情面板显示；标记 inLoadoutOnly 让面板提示用户
    return {
      itemId: slot.itemId, code: slot.code, name: slot.name, type: 'EQUIPMENT',
      subtype: slot.slot, rarity: slot.rarity, description: slot.description,
      attrsJson: '', consumable: false, hpRestore: 0, capacityBonus: 0,
      quantity: 1, _inLoadoutOnly: true
    }
  }
  return null
})

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
  invError.value = ''
}

/** 点装备槽里的物品 → 选中（让 selectedCode 与行囊内该物品联动，详情面板聚焦） */
function pickFromLoadout(slot) {
  if (slot.empty) return
  selectedCode.value = selectedCode.value === slot.code ? null : slot.code
  notice.value = ''
  invError.value = ''
}

async function invAct(fn, successMsg) {
  if (busy.value) {
    console.warn('[Home→行囊] 上一次操作还没结束（busy=', busy.value, '），忽略本次点击')
    return
  }
  busy.value = true
  invError.value = ''
  notice.value = ''
  try {
    // 加超时保护：10 秒还没响应就强制释放 busy（避免 HTTP 卡死时永远禁用按钮）
    const result = await Promise.race([
      fn(),
      new Promise((_, rej) => setTimeout(() => rej(new Error('请求超时（10s）')), 10000))
    ])
    notice.value = successMsg || '操作成功'
    await loadInv()
  } catch (e) {
    console.error('[Home→行囊] 操作失败:', e)
    invError.value = (e.response?.data?.message || e.message || '操作失败')
      + (e.response?.status ? `（HTTP ${e.response.status}）` : '')
  } finally {
    busy.value = false
  }
}

async function useItem(it) {
  console.info('[Home→行囊] useItem:', it.code, it.name)
  await invAct(() => http.post('/inventory/use', { itemCode: it.code }),
               `服用「${it.name}」+气血 ${it.hpRestore}`)
}
async function equipItem(it) {
  console.info('[Home→行囊] equipItem:', it.code, it.name, 'type=', it.type, 'subtype=', it.subtype)
  await invAct(() => http.post('/inventory/equip', { itemCode: it.code }),
               `已装备「${it.name}」`)
  console.info('[Home→行囊] equipItem 完成')
}
async function unequipBySlot(slot) {
  console.info('[Home→行囊] unequipBySlot:', slot)
  await invAct(() => http.post('/inventory/unequip', { slot }), `已卸下「${slot}」槽装备`)
}
async function unequipFromDetail(it) {
  console.info('[Home→行囊] unequipFromDetail:', it.code)
  const slot = inv.value.loadout.find(s => !s.empty && s.code === it.code)
  if (slot) await unequipBySlot(slot.slot)
  else console.warn('[Home→行囊] unequipFromDetail: 找不到对应槽位, it.code=', it.code)
}

const isPill = (i) => i && i.type === 'PILL'
const isEquip = (i) => i && (i.type === 'EQUIPMENT' || i.type === 'TECHNIQUE')
function rarityClass(r) { return 'r-' + (r || 'COMMON').toLowerCase() }
/** 装备槽位 code → 显示名（与后端 UserLoadout.slotName 一致） */
const SLOT_LABELS = { WEAPON: '法器', ARMOR: '护身', STORAGE: '储物', TECHNIQUE: '功法' }
function slotName(code) { return SLOT_LABELS[code] || code || '' }
/** 找物品当前装备在哪个槽位（不在槽里返回 null） */
const equippedSlotOf = (item) => {
  if (!item || !inv.value) return null
  return inv.value.loadout.find(s => !s.empty && s.code === item.code) || null
}
/** 装备按钮 disabled 的原因（null 表示不禁用） */
const equipDisabledReason = computed(() => {
  if (busy.value) return '上一次操作还没结束（busy=' + busy.value + '）'
  return null
})

onMounted(async () => {
  // 防止 Vite HMR 残留 busy 状态（之前用 ref('') 时 '1' 残留会让按钮永远 disabled）
  busy.value = false
  try {
    const { data } = await http.get('/user/me')
    user.value = data.data
  } catch (e) {
    error.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
  // 行囊：和 /user/me 并行拉，互不影响
  loadInv()
})

async function switchTab(name) {
  if (tab.value === name) return
  tab.value = name
  if (name === 'map' && !mapLoaded.value) {
    await loadMapTab()
  }
}

async function loadMapTab() {
  mapLoading.value = true
  mapError.value = ''
  try {
    const [tplRes, actRes] = await Promise.all([
      http.get('/map/templates'),
      // active 可能 404 / null，统一容错
      http.get('/map/active').catch(() => ({ data: { data: null } }))
    ])
    templates.value = tplRes.data.data || []
    activeInst.value = actRes.data.data || null
    mapLoaded.value = true
  } catch (e) {
    mapError.value = e.response?.data?.message || '加载秘境失败'
  } finally {
    mapLoading.value = false
  }
}

async function enterMap(code) {
  if (activeInst.value && activeInst.value.templateCode !== code) {
    if (!confirm(`你正在探索【${activeInst.value.templateName}】，开始新的探索将放弃当前进度，是否继续？`)) return
    try {
      await http.post('/map/abandon', null, { params: { instanceId: activeInst.value.instanceId } })
    } catch (e) { /* ignore */ }
    localStorage.removeItem('mapInstanceId')
  }
  emit('enterMap', code)
}

function resumeMap() {
  emit('enterMap', '')   // 空 code → Map.vue 按 localStorage 恢复
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('mapInstanceId')
  emit('logout')
}
</script>

<template>
  <div class="profile">
    <p v-if="loading" class="tip">正在整理道友的行囊…</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else-if="user">
      <!-- 顶部 Tab 栏 -->
      <nav class="tabs">
        <button class="tab" :class="{ active: tab === 'me' }" @click="switchTab('me')">
          <i class="tab-mark">我</i>我的
        </button>
        <button class="tab" :class="{ active: tab === 'map' }" @click="switchTab('map')">
          <i class="tab-mark">探</i>探索秘境
        </button>
      </nav>

      <!-- ============== 我的 ============== -->
      <template v-if="tab === 'me'">
        <!-- 「我的」页面：左侧名帖 + 属性 / 战力，右侧行囊（背包） -->
        <div class="me-layout">
          <div class="col-main">
            <!-- 名帖 -->
            <section class="panel namecard">
              <div class="avatar-lg">{{ initial }}</div>
              <div class="who">
                <div class="nick">
                  {{ user.nickname }}
                  <span class="badge">{{ user.realmDisplayName || user.realmCode }}</span>
                </div>
                <div v-if="rootsSummary" class="roots-line">{{ rootsSummary }}</div>
                <div class="title">神识 {{ user.spiritualSense }} · 修为 {{ user.exp }}</div>
                <div class="sub">道号 {{ user.username }} · 仙途编号 #{{ user.userNumber }}</div>
              </div>
              <button class="mini" @click="logout">退出</button>
            </section>

            <!-- 灵根（已觉醒时不再展示） -->
            <section v-if="rootList.length === 0" class="panel root-card" :class="{ untested: rootList.length === 0 }">
              <div class="r-name" :class="{ muted: rootList.length === 0 }">
                {{ rootNames || '灵根未定' }}
                <span v-if="rootList.length >= 2" class="r-tag">{{ rootList.length }} 灵根之体</span>
              </div>
              <p class="r-desc">{{ rootDesc }}</p>
            </section>

            <!-- 未测灵根 CTA -->
            <section v-if="rootList.length === 0" class="panel cta">
              <div class="cta-seal">测</div>
              <h3 class="cta-title">你尚未觉醒灵根</h3>
              <p class="cta-desc">前往灵台答 5 道题，觉醒 1 个或 2 个灵根，决定日后可修炼的功法。</p>
              <button class="btn primary big" @click="emit('goTest')">前往测灵根</button>
            </section>

            <!-- 道身属性 + 战力 -->
            <div class="cols">
              <section class="panel">
                <h3>道身属性</h3>
                <dl class="kv">
                  <div><dt>境界</dt><dd>{{ user.realmDisplayName || user.realmCode }}</dd></div>
                  <div><dt>灵根</dt><dd>{{ rootNames || '未定' }}</dd></div>
                  <div><dt>神识</dt><dd>{{ user.spiritualSense }}</dd></div>
                  <div><dt>修为</dt><dd class="uno">{{ user.exp }}</dd></div>
                </dl>
              </section>

              <section class="panel">
                <h3>战力</h3>
                <div class="row"><span class="label">气血</span><span class="val">{{ user.hp }} / {{ user.maxHp }}</span></div>
                <div class="bar"><div class="fill hp" :style="{ width: hpPct + '%' }"></div></div>
                <div class="row"><span class="label">灵力</span><span class="val">{{ user.mana }} / {{ user.maxMana }}</span></div>
                <div class="bar"><div class="fill mp" :style="{ width: manaPct + '%' }"></div></div>
                <div class="row"><span class="label">攻击</span><span class="val">{{ user.attack }}</span></div>
                <div class="row"><span class="label">防御</span><span class="val">{{ user.defense }}</span></div>
                <div class="row"><span class="label">身法</span><span class="val">{{ user.speed }}</span></div>
              </section>
            </div>
          </div>

          <!-- 右栏：行囊（直接内联 Inventory 内容，不依赖跨 SFC 引用） -->
          <div class="col-side">
            <p v-if="invLoading" class="tip">正在打开储物法宝…</p>
            <section v-else-if="invError" class="panel error-panel">
              <div class="ep-mark">!</div>
              <h3 class="ep-title">背包加载失败</h3>
              <p class="ep-msg">{{ invError }}</p>
              <p class="ep-hint">可能原因：后端未启动 / 改完代码未重启 / token 过期。请到 F12 → Console 查看详细日志。</p>
              <button class="btn ghost small" @click="loadInv">重试</button>
            </section>

            <template v-else-if="inv && Array.isArray(inv.items) && Array.isArray(inv.loadout)">
              <!-- 储物空间 -->
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

              <!-- 已佩戴 -->
              <section class="panel">
                <h3>已佩戴</h3>
                <div class="slots">
                  <div v-for="s in inv.loadout" :key="s.slot" class="slot" :class="{ filled: !s.empty, active: !s.empty && selectedCode === s.code }"
                       :style="!s.empty ? { cursor: 'pointer' } : {}"
                       @click="!s.empty && pickFromLoadout(s)">
                    <div class="slot-label">{{ s.slotName }}</div>
                    <div class="slot-body">
                      <template v-if="!s.empty">
                        <span class="slot-name" :class="rarityClass(s.rarity)">{{ s.name }}</span>
                        <button class="mini" :disabled="busy"
                                :title="busy ? '操作进行中（busy=true），请稍候或按下方"重置状态"按钮' : `从「${s.slotName}」槽卸下`"
                                @click.stop="unequipBySlot(s.slot)">卸下</button>
                      </template>
                      <span v-else class="slot-empty">空</span>
                    </div>
                  </div>
                </div>
                <p v-if="busy" class="busy-hint">操作进行中… <button class="mini inline" @click="busy = false">重置状态</button></p>
              </section>

              <!-- 行囊 -->
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

              <!-- 选中详情 -->
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
                      <template v-if="isEquip(selected) && selected.subtype"> · {{ slotName(selected.subtype) }}类</template>
                      <template v-if="selected.quantity > 1"> · 持有 {{ selected.quantity }}</template>
                      <template v-if="isEquip(selected)">
                        · <span :class="equippedCodes.has(selected.code) ? 'worn-tag' : 'unworn-tag'">
                          {{ equippedCodes.has(selected.code) ? '已装备于「' + slotName(equippedSlotOf(selected)?.slot) + '」槽' : '未装备' }}
                        </span>
                      </template>
                    </div>
                  </div>
                </div>
                <p class="d-desc">{{ selected.description }}</p>
                <div class="d-effect">{{ effectText(selected) }}</div>
                <p v-if="selected._inLoadoutOnly" class="hint-line">此物现装备于「{{ slotName(selected.subtype) }}」槽，先卸下再操作。</p>
                <div class="d-actions">
                  <button v-if="isPill(selected) && selected.consumable"
                          class="btn primary" :disabled="busy" @click="useItem(selected)">服用</button>
                  <button v-if="isEquip(selected) && !equippedCodes.has(selected.code)"
                          class="btn" :disabled="busy"
                          :title="equipDisabledReason || `装到「${slotName(selected.subtype)}」槽`"
                          @click="equipItem(selected)">装备</button>
                  <button v-if="isEquip(selected) && equippedCodes.has(selected.code)"
                          class="btn" :disabled="busy"
                          :title="equipDisabledReason || `从「${slotName(equippedSlotOf(selected)?.slot)}」槽卸下`"
                          @click="unequipFromDetail(selected)">卸下</button>
                </div>
              </section>

              <p v-if="notice" class="notice">{{ notice }}</p>
              <button class="btn refresh" @click="loadInv">整理行囊</button>
            </template>

            <!-- 兜底：以上条件都不满足时显示诊断信息 -->
            <section v-else class="panel error-panel">
              <div class="ep-mark">?</div>
              <h3 class="ep-title">背包数据未就绪</h3>
              <p class="ep-msg">
                加载已结束但未拿到数据。可能是后端没启动、返回结构异常或登录态失效。
              </p>
              <p class="ep-hint">
                请按 F12 → Console 查看 <code>[Home→行囊]</code> 相关日志。<br>
                当前状态：loading={{ invLoading }} / error={{ invError || '无' }} / inv={{ inv ? '有对象' : '空' }}
              </p>
              <button class="btn ghost small" @click="loadInv">重试</button>
            </section>
          </div>
        </div>

        <button class="btn refresh" @click="logout">退出登录</button>
      </template>

      <!-- ============== 探索秘境 ============== -->
      <template v-else-if="tab === 'map'">
        <p v-if="mapLoading" class="tip">正在寻找秘境入口…</p>
        <p v-else-if="mapError" class="error">{{ mapError }}</p>
        <template v-else>
          <!-- 继续上次探索 -->
          <section v-if="activeInst" class="panel resume-card">
            <div class="re-seal">续</div>
            <div class="re-text">
              <h3>继续上次探索</h3>
              <p>
                <b>{{ activeInst.templateName }}</b>
                · 步数 {{ activeInst.stepCount }} / {{ activeInst.maxSteps }}
                · 神识半径 {{ activeInst.radius }}
              </p>
            </div>
            <button class="btn primary big" @click="resumeMap">继续</button>
          </section>

          <!-- 模板列表 -->
          <h3 class="section-title">选择秘境</h3>
          <div v-if="templates.length === 0" class="empty">暂无可用秘境</div>
          <div v-else class="map-list">
            <article v-for="t in templates" :key="t.id" class="panel map-card">
              <div class="mc-head">
                <h4>{{ t.name }}</h4>
                <span class="realm-tag">{{ t.recommendedRealmName || t.recommendedRealm }}</span>
              </div>
              <p class="mc-desc">{{ t.description }}</p>
              <div class="mc-meta">
                <span>{{ t.size }}×{{ t.size }} 地图</span>
                <span class="dot-sep">·</span>
                <span>{{ t.maxSteps }} 步上限</span>
                <span class="dot-sep">·</span>
                <span>岩 {{ t.obstacleRate }}%</span>
                <span class="dot-sep">·</span>
                <span>妖 {{ t.monsterRate }}%</span>
                <span class="dot-sep">·</span>
                <span>草 {{ t.resourceRate }}%</span>
              </div>
              <button class="btn primary big" @click="enterMap(t.code)">进入秘境</button>
            </article>
          </div>
        </template>
      </template>
    </template>
  </div>
</template>

<style scoped>
.profile { animation: inkIn .5s ease-out both; }
.tip { text-align: center; color: var(--ink-light); font-size: 14px; padding: 18px 0; }
.error { color: var(--seal); text-align: center; margin-top: 16px; font-size: 14px; }
.empty { text-align: center; color: var(--ink-light); padding: 30px 0; font-size: 13px; letter-spacing: 2px; }

.panel {
  position: relative; overflow: hidden; background: rgba(255,255,255,.55);
  border: 1px solid var(--line); border-radius: 4px; padding: 20px 22px;
  box-shadow: 0 2px 18px rgba(31,29,26,.06); backdrop-filter: blur(2px);
  margin-bottom: 16px;
}
.panel::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(ellipse 60%45% at 16% 0%, rgba(31,29,26,.05), transparent 62%);
}
.panel > * { position: relative; }
h3 {
  margin: 0 0 14px; font-size: 15px; color: var(--ink); letter-spacing: 2px;
  border-left: 3px solid var(--ink); padding-left: 10px; line-height: 1.2;
}

/* ---------- Tab 栏 ---------- */
.tabs {
  display: flex;
  border-bottom: 1px solid var(--line);
  margin-bottom: 18px;
  position: relative;
}
.tab {
  flex: 1;
  background: transparent;
  border: none;
  padding: 12px 0 10px;
  font-size: 14px;
  color: var(--ink-light);
  letter-spacing: 4px;
  font-family: inherit;
  cursor: pointer;
  position: relative;
  transition: color .2s;
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
}
.tab:hover { color: var(--ink); }
.tab.active { color: var(--seal); font-weight: 700; }
.tab.active::after {
  content: "";
  position: absolute;
  left: 50%; bottom: -1px;
  transform: translateX(-50%);
  width: 36px; height: 2px;
  background: var(--seal);
  border-radius: 1px;
}
.tab-mark {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; font-size: 11px; font-weight: 700;
  border: 1px solid currentColor; border-radius: 3px;
  letter-spacing: 0;
}

/* 双栏（仿 xiuxian App.vue .profile-layout） */
.me-layout {
  display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr);
  gap: 16px; align-items: start;
}
.col-side {
  /* 右栏内联行囊面板 */
  display: flex; flex-direction: column;
}
@media (max-width: 900px) {
  .me-layout { grid-template-columns: 1fr; }
}

/* ============== 行囊面板（内联在 Home 右栏） ============== */
.cap-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 10px; }
.cap-head h3 { margin-bottom: 0; }
.cap-num { font-size: 18px; font-weight: 700; color: var(--ink); font-variant-numeric: tabular-nums; }
.bar { height: 8px; border-radius: 2px; background: #e3ded2; overflow: hidden; }
.bar .fill { height: 100%; background: linear-gradient(90deg, #6b665d, var(--ink)); transition: width .4s ease; }
.cap-detail { display: flex; gap: 14px; flex-wrap: wrap; color: var(--ink-soft); font-size: 12px; margin-top: 10px; letter-spacing: 1px; }
.cap-detail .bonus { color: var(--seal); font-weight: 600; }
.cap-detail .muted { color: var(--ink-light); }

.slots { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.slot { border: 1px dashed var(--line); border-radius: 4px; padding: 10px 12px; background: rgba(255,255,255,.4); transition: border-color .2s; }
.slot.filled { border-style: solid; border-color: var(--ink-soft); }
.slot.active { border-color: var(--seal); background: rgba(158,59,52,.06); box-shadow: 0 0 0 2px rgba(158,59,52,.12); }
.slot-label { font-size: 12px; color: var(--ink-light); letter-spacing: 2px; }
.slot-body { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 4px; }
.slot-name { font-size: 14px; font-weight: 600; letter-spacing: 1px; }
.slot-empty { font-size: 13px; color: var(--ink-light); }
.worn-tag { color: #2d6b3a; font-weight: 600; }
.unworn-tag { color: var(--ink-light); }
.hint-line {
  margin: 6px 0 0; padding: 6px 10px; font-size: 12px; letter-spacing: 1px;
  color: var(--ink-light); background: rgba(31,29,26,.04);
  border: 1px dashed var(--line); border-radius: 3px; text-align: center;
}
.busy-hint {
  margin: 8px 0 0; padding: 6px 10px; font-size: 12px;
  color: var(--seal); background: rgba(158,59,52,.06);
  border: 1px dashed var(--seal); border-radius: 3px; text-align: center;
}
.busy-hint .mini.inline {
  margin-left: 8px; padding: 2px 10px; font-size: 11px;
  border: 1px solid var(--seal); background: transparent; color: var(--seal);
  border-radius: 3px; cursor: pointer;
}
.mini {
  border: 1px solid var(--line); background: transparent; color: var(--ink-light);
  border-radius: 3px; font-size: 12px; padding: 2px 8px; font-family: inherit;
  transition: all .2s; letter-spacing: 1px; cursor: pointer;
}
.mini:hover:not(:disabled) { border-color: var(--ink); color: var(--ink); }
.mini:disabled { opacity: .5; cursor: not-allowed; }

.bag-head { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; }
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

.notice {
  text-align: center; color: var(--ink); font-size: 14px; letter-spacing: 1px;
  background: rgba(158,59,52,.08); border: 1px dashed var(--seal);
  border-radius: 4px; padding: 10px; margin-bottom: 14px;
}
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
.error-panel .ep-hint { margin: 0 auto 14px; color: var(--ink-soft); font-size: 12px; line-height: 1.8; max-width: 480px; }

.r-common { color: #6b665d; }
.r-rare { color: #4a7c6f; }
.r-epic { color: #3f6a9e; }
.r-legendary { color: #7a5aa0; }
.rarity { font-size: 12px; margin-left: 6px; padding: 1px 6px; border-radius: 2px; border: 1px solid currentColor; }

/* 名帖 */
.namecard { display: flex; align-items: center; gap: 18px; padding: 22px; }
.avatar-lg {
  width: 76px; height: 76px; flex: none; display: flex; align-items: center; justify-content: center;
  font-size: 34px; font-weight: 700; border-radius: 6px;
  border: 2px solid var(--seal); transform: rotate(-3deg); color: var(--seal);
  background: rgba(158,59,52,.06);
  box-shadow: 0 0 0 3px rgba(158,59,52,.05), 0 2px 10px rgba(31,29,26,.14);
  transition: transform .2s;
}
.avatar-lg:hover { transform: rotate(0deg) scale(1.03); }
.who { min-width: 0; flex: 1; }
.nick {
  font-size: 22px; font-weight: 700; letter-spacing: 2px;
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
}
.badge {
  font-size: 12px; font-weight: 700; letter-spacing: 1px; color: #fff; background: var(--ink);
  padding: 3px 10px; border-radius: 3px; transform: rotate(-1.5deg);
}
.title { color: var(--seal); font-size: 13px; letter-spacing: 1px; margin-top: 6px; }
.roots-line {
  color: var(--seal); font-size: 14px; font-weight: 600;
  letter-spacing: 3px; margin-top: 4px;
}
.sub { color: var(--ink-light); font-size: 12px; margin-top: 4px; letter-spacing: 1px; }
.mini {
  border: 1px solid var(--line); background: transparent; color: var(--ink-light);
  border-radius: 3px; font-size: 12px; padding: 4px 10px; font-family: inherit;
  letter-spacing: 1px; transition: all .2s; flex: none; align-self: flex-start;
}
.mini:hover { border-color: var(--ink); color: var(--ink); }

/* 灵根卡 */
.root-card { text-align: center; }
.root-card .r-name {
  font-size: 28px; font-weight: 700; color: var(--seal);
  letter-spacing: 8px; padding: 4px 0 8px;
}
.root-card.untested .r-name { font-size: 18px; letter-spacing: 4px; color: var(--ink-light); font-weight: 500; }
.r-desc { margin: 0; color: var(--ink-soft); font-size: 13px; line-height: 1.8; letter-spacing: 1px; }
.r-tag {
  display: inline-block; font-size: 11px; letter-spacing: 2px; color: var(--seal);
  border: 1px solid var(--seal); padding: 2px 8px; border-radius: 3px;
  margin-left: 10px; vertical-align: middle; transform: rotate(-2deg);
}

/* 未测灵根 CTA */
.cta { text-align: center; padding: 26px 22px; }
.cta-seal {
  display: inline-flex; align-items: center; justify-content: center;
  width: 48px; height: 48px; font-size: 22px; font-weight: 700;
  color: var(--seal); border: 2px solid var(--seal); border-radius: 6px;
  background: rgba(158,59,52,.06); transform: rotate(-3deg); margin-bottom: 12px;
}
.cta-title { margin: 6px 0 8px; font-size: 18px; letter-spacing: 3px; color: var(--ink); font-weight: 700; }
.cta-desc { margin: 0 auto 18px; color: var(--ink-soft); font-size: 13px; line-height: 1.8; letter-spacing: 1px; max-width: 420px; }
.cta .btn { padding: 11px 32px; }

/* 双栏 */
.cols { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.cols .panel { margin-bottom: 16px; }

/* 明细表 */
.kv { margin: 0; display: grid; grid-template-columns: 1fr; gap: 10px; }
.kv > div { display: flex; justify-content: space-between; gap: 8px; border-bottom: 1px dashed var(--line); padding-bottom: 6px; }
dt { color: var(--ink-light); font-size: 13px; letter-spacing: 1px; }
dd { margin: 0; font-size: 13px; color: var(--ink); font-weight: 600; text-align: right; }
dd.uno { color: var(--seal); letter-spacing: 2px; font-variant-numeric: tabular-nums; }

/* 进度条 */
.row { display: flex; justify-content: space-between; font-size: 13px; margin: 8px 0 5px; }
.label { color: var(--ink-soft); letter-spacing: 1px; }
.val { font-weight: 600; }
.bar { height: 8px; border-radius: 2px; background: #e3ded2; overflow: hidden; margin-bottom: 12px; }
.bar .fill { height: 100%; border-radius: 2px; transition: width .4s ease; }
.bar .fill.hp { background: linear-gradient(90deg, #c4897f, var(--seal)); }
.bar .fill.mp { background: linear-gradient(90deg, #7fa8c4, #2f6f9e); }

/* ---------- 探索秘境 tab ---------- */
.section-title {
  font-size: 14px; color: var(--ink); letter-spacing: 4px;
  margin: 4px 0 12px; padding-left: 10px;
  border-left: 3px solid var(--ink); line-height: 1.2;
}
.resume-card {
  display: flex; align-items: center; gap: 14px; padding: 18px 22px;
  background: rgba(45,107,58,.06); border-color: #2d6b3a;
}
.re-seal {
  flex: none; width: 44px; height: 44px;
  display: inline-flex; align-items: center; justify-content: center;
  border: 2px solid #2d6b3a; color: #2d6b3a;
  font-size: 20px; font-weight: 700; border-radius: 5px;
  background: rgba(45,107,58,.10); transform: rotate(-3deg);
}
.re-text { flex: 1; min-width: 0; }
.re-text h3 {
  margin: 0; font-size: 16px; color: #2d6b3a; letter-spacing: 2px;
  border: none; padding: 0; font-weight: 700;
}
.re-text p { margin: 4px 0 0; font-size: 12px; color: var(--ink-soft); letter-spacing: 1px; }
.resume-card .btn { flex: none; }

.map-list { display: grid; gap: 14px; }
.map-card { padding: 18px 22px; }
.mc-head {
  display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap;
  border-bottom: 1px dashed var(--line);
  padding-bottom: 8px; margin-bottom: 10px;
}
.mc-head h4 {
  margin: 0; font-size: 18px; color: var(--ink);
  letter-spacing: 3px; font-weight: 700;
}
.realm-tag {
  font-size: 11px; padding: 2px 8px; border-radius: 2px;
  background: var(--seal); color: #fff;
  letter-spacing: 2px; font-weight: 600;
}
.mc-desc { margin: 0 0 10px; color: var(--ink-soft); font-size: 13px; line-height: 1.7; letter-spacing: 1px; }
.mc-meta { font-size: 12px; color: var(--ink-light); letter-spacing: 1px; margin-bottom: 14px; }
.mc-meta .dot-sep { color: var(--line); margin: 0 4px; }
.map-card .btn { display: block; margin: 0 auto; min-width: 140px; }

.btn { border: 1px solid var(--ink); border-radius: 4px; padding: 9px 20px; font-size: 14px;
  background: transparent; color: var(--ink); font-family: inherit; transition: all .2s; }
.btn:hover { background: var(--ink); color: #fff; }
.refresh { display: block; margin: 0 auto; letter-spacing: 2px; }

@media (max-width: 620px) {
  .cols { grid-template-columns: 1fr; gap: 0; }
  .namecard { flex-wrap: wrap; }
  .mini { margin-left: auto; }
}
</style>