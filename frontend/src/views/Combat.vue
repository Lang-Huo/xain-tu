<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../api/http'

const emit = defineEmits(['exit'])

const loading = ref(true)
const error = ref('')             // load 错误：覆盖整个 UI
const actionError = ref('')       // 行动错误：只在战斗 UI 底部 flash，不覆盖
const combat = ref(null)        // CombatStateResponse
const combatId = ref(null)
const busy = ref(false)          // 行动中（防双击）

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (!combatId.value) throw new Error('combatId 缺失')
    const { data } = await http.get(`/combat/${combatId.value}`)
    console.info('[Combat] /combat/{id} 响应:', data.data)
    combat.value = data.data
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载战斗失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // combatId 通过 localStorage 跨视图传递（App.vue 的 enterCombat() 写入）
  const id = localStorage.getItem('combatId')
  if (id) combatId.value = Number(id)
  load()
})

const playerHpPct = computed(() => {
  if (!combat.value || !combat.value.playerHpSnapshot) return 0
  return Math.max(0, Math.min(100, (combat.value.playerHp / combat.value.playerHpSnapshot) * 100))
})
const monsterHpPct = computed(() => {
  if (!combat.value || !combat.value.monsterMaxHp) return 0
  return Math.max(0, Math.min(100, (combat.value.monsterHp / combat.value.monsterMaxHp) * 100))
})
const finished = computed(() => combat.value && combat.value.status === 'FINISHED')
const resultText = computed(() => {
  if (!finished.value) return ''
  switch (combat.value.result) {
    case 'VICTORY': return '你击败了「' + combat.value.monsterName + '」！'
    case 'DEFEAT':  return '你被「' + combat.value.monsterName + '」击败…'
    case 'FLED':    return '你成功脱身。'
    default: return ''
  }
})
const rewardText = computed(() => {
  if (!finished.value || combat.value.result !== 'VICTORY') return ''
  const parts = []
  if (combat.value.expGained) parts.push('修为 +' + combat.value.expGained)
  if (combat.value.droppedItemCode && combat.value.droppedQuantity) {
    parts.push('获得「' + combat.value.droppedItemCode + '」×' + combat.value.droppedQuantity)
  }
  return parts.length ? '（' + parts.join(' · ') + '）' : ''
})
// 背包里灵草数量（后端每帧附在响应里）
const lingCaoCount = computed(() => {
  const raw = combat.value?.lingCaoCount
  // 诊断日志：第一次拿到战斗状态时打印一次，方便 F12 排查
  if (combat.value && raw === undefined) {
    console.warn('[Combat] 战斗响应里没有 lingCaoCount 字段——八成是后端没重启。重启 Spring Boot 后再试。当前按 0 处理（按钮会一直显示"灵草已用完"）。')
  } else if (combat.value && raw !== undefined && lingCaoCount._logged !== raw) {
    console.info('[Combat] lingCaoCount =', raw, '(若背包实际有灵草但这里是 0，请检查后端是否重启 / 是否调通了 /api/combat/action)')
    lingCaoCount._logged = raw
  }
  return raw ?? 0
})
// "服用灵草" 按钮文字：0 时显示"灵草已用完"，>0 时显示"服用灵草 (×N)"
const lingCaoBtnLabel = computed(() => {
  const n = lingCaoCount.value
  return n > 0 ? ('服用灵草 ×' + n) : '灵草已用完'
})
// "服用灵草" 按钮是否禁用（玩家回合 + 不忙 + 有灵草）
const canUseLingCao = computed(() => lingCaoCount.value > 0)

async function act(action, itemCode) {
  if (busy.value || finished.value) return
  busy.value = true
  actionError.value = ''
  try {
    const { data } = await http.post('/combat/action', {
      combatId: combatId.value, action, itemCode
    })
    console.info('[Combat] 行动成功:', action, itemCode, 'lingCaoCount=', data.data?.lingCaoCount, data.data)
    combat.value = data.data
  } catch (e) {
    // 行动错误：只 flash，不覆盖整个 UI（之前用 error.value 覆盖会让战斗页面消失）
    console.error('[Combat] 行动失败:', e.response?.status, e.response?.data?.message || e.message)
    actionError.value = e.response?.data?.message || '行动失败'
    setTimeout(() => { actionError.value = '' }, 3000)
  } finally {
    busy.value = false
  }
}

function exit() {
  localStorage.removeItem('combatId')
  emit('exit')
}
</script>

<template>
  <div class="combat">
    <header class="ct-header">
      <button class="btn ghost" :disabled="!finished" @click="exit">← 返回秘境</button>
      <span class="ct-mark">战</span>
      <span class="title">战斗</span>
    </header>

    <p v-if="loading" class="tip">正在凝聚灵气…</p>
    <section v-else-if="error" class="panel error-panel">
      <div class="ep-mark">!</div>
      <h3 class="ep-title">战斗加载失败</h3>
      <p class="ep-msg">{{ error }}</p>
      <button class="btn ghost small" @click="load">重试</button>
    </section>

    <template v-else-if="combat">
      <!-- 双方状态 -->
      <section class="panel arena">
        <!-- 怪物 -->
        <div class="side monster">
          <div class="glyph big" :class="finished && combat.result === 'VICTORY' ? 'dead' : ''">
            {{ combat.monsterName.charAt(0) }}
          </div>
          <div class="info">
            <div class="name">
              <span class="nm">{{ combat.monsterName }}</span>
              <span class="lvl">Lv {{ combat.monsterLevel || 3 }}</span>
            </div>
            <div class="bar"><div class="fill hp-m" :style="{ width: monsterHpPct + '%' }"></div></div>
            <div class="hp-text">气血 {{ combat.monsterHp }} / {{ combat.monsterMaxHp }}</div>
          </div>
        </div>

        <div class="vs">VS</div>

        <!-- 玩家 -->
        <div class="side player">
          <div class="glyph big" :class="finished && combat.result !== 'VICTORY' ? 'dead' : ''">
            我
          </div>
          <div class="info">
            <div class="name">
              <span class="nm">你</span>
              <span class="lvl">攻 {{ combat.playerAttack }} · 防 {{ combat.playerDefense }}</span>
            </div>
            <div class="bar"><div class="fill hp-p" :style="{ width: playerHpPct + '%' }"></div></div>
            <div class="hp-text">气血 {{ combat.playerHp }} / {{ combat.playerHpSnapshot }}</div>
          </div>
        </div>
      </section>

      <!-- 回合日志 -->
      <section class="panel log-panel">
        <h3>回合记录 · 第 {{ combat.round }} 回合</h3>
        <div class="log">
          <p v-for="(line, i) in combat.log" :key="i" class="log-line">{{ line }}</p>
          <p v-if="!combat.log || combat.log.length === 0" class="tip">尚未开始</p>
        </div>
      </section>

      <!-- 行动按钮 / 战斗结果 -->
      <section v-if="!finished" class="panel actions">
        <h3>你的行动</h3>
        <p class="turn-hint">当前回合：<b>{{ combat.currentTurn === 'PLAYER' ? '你' : combat.monsterName }}</b></p>
        <div class="action-row">
          <button class="btn primary big" :disabled="busy || combat.currentTurn !== 'PLAYER'"
                  @click="act('ATTACK')">攻击</button>
          <button class="btn big" :disabled="busy || combat.currentTurn !== 'PLAYER' || !canUseLingCao"
                  @click="act('USE_ITEM', 'LING_CAO')">{{ lingCaoBtnLabel }}</button>
          <button class="btn big" :disabled="busy || combat.currentTurn !== 'PLAYER'"
                  @click="act('FLEE')">逃跑</button>
        </div>
        <p v-if="!canUseLingCao" class="herb-hint">灵草已耗尽，可在秘境中采集或战胜妖兽获得。</p>
        <transition name="fade">
          <p v-if="actionError" class="action-error">{{ actionError }}</p>
        </transition>
      </section>

      <section v-else class="panel result-panel" :class="'r-' + (combat.result || '').toLowerCase()">
        <div class="rp-mark">
          <span v-if="combat.result === 'VICTORY'">胜</span>
          <span v-else-if="combat.result === 'DEFEAT'">负</span>
          <span v-else>逃</span>
        </div>
        <h2 class="rp-title">{{ resultText }}</h2>
        <p v-if="rewardText" class="rp-reward">{{ rewardText }}</p>
        <p v-else-if="combat.result === 'DEFEAT'" class="rp-reward muted">气血已耗至 1，踉跄退回</p>
        <p v-else-if="combat.result === 'FLED'" class="rp-reward muted">脱离战斗，HP 保留</p>
        <button class="btn primary big" @click="exit">返回秘境</button>
      </section>
    </template>

    <!-- 兜底：理论上 combat 永远非空，但若 HMR/异常导致 combat 为 null，给出友好降级 -->
    <section v-else class="panel error-panel">
      <div class="ep-mark">!</div>
      <h3 class="ep-title">战斗数据缺失</h3>
      <p class="ep-msg">未读到战斗状态，可能是战斗已结束或被清理。</p>
      <button class="btn ghost small" @click="exit">返回秘境</button>
    </section>
  </div>
</template>

<style scoped>
.combat { animation: inkIn .5s ease-out both; }
.tip { text-align: center; color: var(--ink-light); font-size: 14px; padding: 18px 0; }
.error { color: var(--seal); text-align: center; margin-top: 16px; font-size: 14px; padding: 8px 14px; }

.ct-header { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.ct-mark {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border: 2px solid var(--seal); color: var(--seal);
  font-size: 16px; font-weight: 700; border-radius: 5px;
  background: rgba(158,59,52,.06); transform: rotate(-3deg);
}
.ct-header .title {
  font-size: 17px; font-weight: 700; color: var(--ink); letter-spacing: 4px;
}

.panel {
  position: relative; overflow: hidden; background: rgba(255,255,255,.55);
  border: 1px solid var(--line); border-radius: 4px; padding: 16px 18px;
  box-shadow: 0 2px 18px rgba(31,29,26,.06); backdrop-filter: blur(2px);
  margin-bottom: 14px;
}
.panel::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(ellipse 60%45% at 16% 0%, rgba(31,29,26,.05), transparent 62%);
}
.panel > * { position: relative; }
h3 { margin: 0 0 10px; font-size: 14px; color: var(--ink); letter-spacing: 2px;
     border-left: 3px solid var(--ink); padding-left: 8px; line-height: 1.2; }

.btn { border: 1px solid var(--ink); border-radius: 4px; padding: 7px 16px; font-size: 13px;
  background: transparent; color: var(--ink); font-family: inherit; transition: all .2s; cursor: pointer;
  letter-spacing: 1px;
}
.btn:hover:not(:disabled) { background: var(--ink); color: #fff; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn.primary { border-color: var(--seal); color: var(--seal); }
.btn.primary:hover:not(:disabled) { background: var(--seal); color: #fff; }
.btn.big { padding: 12px 22px; font-size: 15px; flex: 1; }
.btn.ghost { border: 1px solid var(--line); background: transparent; color: var(--ink-light); padding: 6px 14px; font-size: 12px; border-radius: 3px; }
.btn.ghost:hover:not(:disabled) { color: var(--ink); border-color: var(--ink); }
.btn.small { padding: 5px 12px; font-size: 12px; }

/* 战场 */
.arena {
  display: grid; grid-template-columns: 1fr auto 1fr; gap: 16px; align-items: center;
  padding: 22px 18px;
}
.side { display: flex; align-items: center; gap: 14px; }
.side.player { flex-direction: row-reverse; text-align: right; }
.glyph.big {
  width: 64px; height: 64px; flex: none;
  display: flex; align-items: center; justify-content: center;
  border: 2px solid var(--ink); border-radius: 6px;
  font-size: 30px; font-weight: 700; color: var(--ink);
  background: rgba(255,255,255,.5); transform: rotate(-3deg);
}
.side.monster .glyph.big { border-color: #6b3a18; color: #6b3a18; background: rgba(107,58,24,.06); transform: rotate(3deg); }
.glyph.big.dead { opacity: .35; filter: grayscale(.7); transform: rotate(90deg); transition: all .4s ease; }
.info { min-width: 0; flex: 1; }
.name { display: flex; align-items: baseline; gap: 8px; margin-bottom: 4px; flex-wrap: wrap; }
.side.player .name { justify-content: flex-end; }
.nm { font-size: 15px; font-weight: 700; color: var(--ink); letter-spacing: 1px; }
.lvl { font-size: 11px; color: var(--ink-light); letter-spacing: 1px; }
.bar { height: 8px; border-radius: 2px; background: #e3ded2; overflow: hidden; margin: 4px 0; }
.bar .fill { height: 100%; border-radius: 2px; transition: width .4s ease; }
.bar .fill.hp-m { background: linear-gradient(90deg, #c4897f, #6b3a18); }
.bar .fill.hp-p { background: linear-gradient(90deg, #c4897f, var(--seal)); }
.hp-text { font-size: 12px; color: var(--ink-light); letter-spacing: 1px; font-variant-numeric: tabular-nums; }
.vs {
  font-size: 22px; font-weight: 700; color: var(--seal);
  letter-spacing: 4px; font-family: serif;
}

/* 日志 */
.log {
  max-height: 200px; overflow-y: auto; padding-right: 6px;
}
.log-line {
  margin: 4px 0; padding: 5px 8px; border-bottom: 1px dashed var(--line);
  color: var(--ink); font-size: 13px; letter-spacing: 1px; line-height: 1.6;
}
.log-line:last-child { border-bottom: none; }

/* 行动按钮 */
.turn-hint { margin: 0 0 10px; color: var(--ink-soft); font-size: 12px; letter-spacing: 1px; }
.turn-hint b { color: var(--seal); font-weight: 700; }
.action-row { display: flex; gap: 10px; flex-wrap: wrap; }
.herb-hint {
  margin: 10px 0 0; color: var(--ink-light); font-size: 12px; letter-spacing: 1px;
  text-align: center; font-style: italic;
}
.action-error {
  margin: 12px 0 0; padding: 8px 12px; border-radius: 4px;
  background: rgba(158,59,52,.08); border: 1px dashed var(--seal);
  color: var(--seal); font-size: 13px; letter-spacing: 1px; text-align: center;
}
.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 结果 */
.result-panel { text-align: center; padding: 30px 22px; }
.rp-mark {
  display: inline-flex; align-items: center; justify-content: center;
  width: 60px; height: 60px; border: 3px solid var(--ink); border-radius: 50%;
  font-size: 28px; font-weight: 700; color: var(--ink); letter-spacing: 0;
  margin-bottom: 10px; background: rgba(255,255,255,.6);
}
.result-panel.r-victory .rp-mark { border-color: #2d6b3a; color: #2d6b3a; background: rgba(45,107,58,.1); }
.result-panel.r-defeat  .rp-mark { border-color: var(--seal); color: var(--seal); background: rgba(158,59,52,.1); }
.result-panel.r-fled    .rp-mark { border-color: #6b665d; color: #6b665d; background: rgba(107,102,93,.1); }
.rp-title { margin: 8px 0 6px; font-size: 20px; color: var(--ink); letter-spacing: 3px; }
.rp-reward { margin: 0 auto 18px; color: var(--seal); font-size: 14px; letter-spacing: 2px; font-weight: 600; }
.rp-reward.muted { color: var(--ink-light); font-weight: 400; }
.result-panel .btn { min-width: 160px; }

.error-panel { text-align: center; padding: 26px 22px; border-color: var(--seal); background: rgba(158,59,52,.06); }
.error-panel .ep-mark {
  display: inline-flex; align-items: center; justify-content: center;
  width: 44px; height: 44px; border: 2px solid var(--seal); color: var(--seal);
  font-size: 26px; font-weight: 700; border-radius: 6px;
  background: rgba(158,59,52,.08); transform: rotate(-3deg); margin-bottom: 10px;
}
.error-panel .ep-title { margin: 4px 0 8px; font-size: 16px; letter-spacing: 3px; color: var(--seal); }
.error-panel .ep-msg { margin: 0 auto 12px; color: var(--ink); font-size: 13px; }

@media (max-width: 520px) {
  .arena { grid-template-columns: 1fr; }
  .vs { text-align: center; }
  .side.player { flex-direction: row; text-align: left; }
  .side.player .name { justify-content: flex-start; }
}
</style>