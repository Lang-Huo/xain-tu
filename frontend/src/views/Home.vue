<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../api/http'

const emit = defineEmits(['logout', 'goTest'])
const user = ref(null)
const loading = ref(true)
const error = ref('')

// 灵根（无主副之分；灵根只决定可修炼的功法，无加成/稀有度/颜色等属性）
const rootList = computed(() => user.value?.spiritRoots || [])
const rootNames = computed(() => rootList.value.map(r => r.name).join(' · '))
const rootDesc = computed(() => {
  const list = rootList.value
  if (list.length === 0) return '尚未测灵根，前往测灵根以觉醒属性。'
  if (list.length === 1) return '已觉醒「' + list[0].name + '」灵根。'
  return '已觉醒 ' + list.length + ' 灵根之体：' + rootNames.value + '。'
})
// 灵根一行展示（昵称下方）：
//   1 个：name + 「灵根」            →「金灵根」
//   2 个：name1+name2 + 「双灵根」   →「金木双灵根」
//   3+ 个：count + 「灵根」           →「三灵根」「五灵根」
const CHINESE_NUMS = ['', '一', '二', '三', '四', '五', '六', '七', '八']
const rootsSummary = computed(() => {
  const list = rootList.value
  const n = list.length
  if (n === 0) return ''
  if (n === 1) return list[0].name + '灵根'
  if (n === 2) return list.map(r => r.name).join('') + '双灵根'
  return (CHINESE_NUMS[n] || String(n)) + '灵根'
})
// 昵称首字
const initial = computed(() => (user.value?.nickname || user.value?.username || '修').charAt(0))
const hpPct = computed(() =>
  user.value ? Math.max(0, Math.min(100, (user.value.hp / user.value.maxHp) * 100)) : 0
)
const manaPct = computed(() =>
  user.value ? Math.max(0, Math.min(100, (user.value.mana / user.value.maxMana) * 100)) : 0
)

onMounted(async () => {
  try {
    const { data } = await http.get('/user/me')
    user.value = data.data
  } catch (e) {
    error.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function logout() {
  localStorage.removeItem('token')
  emit('logout')
}
</script>

<template>
  <div class="profile">
    <p v-if="loading" class="tip">正在整理道友的行囊…</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else-if="user">
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

      <!-- 灵根（已觉醒时不再展示——名帖下方一行已带简述；未觉醒时给出提示） -->
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

      <button class="btn refresh" @click="logout">退出登录</button>
    </template>
  </div>
</template>

<style scoped>
.profile { animation: inkIn .5s ease-out both; }
.tip { text-align: center; color: var(--ink-light); font-size: 14px; padding: 18px 0; }
.error { color: var(--seal); text-align: center; margin-top: 16px; font-size: 14px; }

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