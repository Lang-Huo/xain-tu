<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import http from '../api/http'

const props = defineProps({
  /** 入图时传的模板 code（如 'BACK_BAMBOO'）；为空时尝试恢复上次的实例（按 localStorage） */
  templateCode: { type: String, default: '' }
})
const emit = defineEmits(['exit', 'back'])

const canvasRef = ref(null)
const state = ref(null)
const loading = ref(true)
const error = ref('')
const event = ref(null)        // { type, message, ts }
const moving = ref(false)
const instanceIdRef = ref(null)

const cellPx = computed(() => {
  if (!state.value) return 56
  // 取最大 480，按 size 等比缩放
  return Math.floor(Math.min(480, state.value.size * 56) / state.value.size)
})
const canvasSize = computed(() => cellPx.value * (state.value?.size || 0))
const canMove = computed(() => state.value && state.value.status === 'ACTIVE' && !moving.value)
const stepPct = computed(() => state.value ? Math.min(100, (state.value.stepCount / state.value.maxSteps) * 100) : 0)

onMounted(async () => {
  window.addEventListener('keydown', onKey)
  if (props.templateCode) {
    await enterMap(props.templateCode)
  } else {
    const stored = localStorage.getItem('mapInstanceId')
    if (stored) {
      try {
        await loadState(Number(stored))
      } catch (e) {
        // 实例失效或已结束 → 清掉
        localStorage.removeItem('mapInstanceId')
        error.value = '上一次的秘境已结束，请回到主页重新进入'
      }
    } else {
      error.value = '请从主页选择秘境进入'
    }
  }
  loading.value = false
  await nextTick()
  draw()
})

onUnmounted(() => window.removeEventListener('keydown', onKey))

watch(() => state.value, () => nextTick(draw), { deep: true })

async function enterMap(code) {
  error.value = ''
  const { data } = await http.post('/map/enter', { templateCode: code })
  state.value = data.data
  instanceIdRef.value = state.value.instanceId
  localStorage.setItem('mapInstanceId', String(state.value.instanceId))
  flash('info', '踏入' + state.value.templateName + '，拨开迷雾，谨慎前行')
}

async function loadState(id) {
  const { data } = await http.get('/map/state', { params: { instanceId: id } })
  state.value = data.data
  instanceIdRef.value = state.value.instanceId
}

async function move(dir) {
  if (!canMove.value || !state.value) return
  moving.value = true
  error.value = ''
  try {
    const { data } = await http.post('/map/move', {
      instanceId: state.value.instanceId,
      direction: dir
    })
    const resp = data.data
    state.value = resp.state
    if (resp.event === 'BLOCKED') {
      flash('warn', resp.message || '无法移动')
    } else if (resp.event === 'RESOURCE_COLLECTED') {
      flash('good', resp.message || '采集到资源')
    } else if (resp.event === 'MONSTER_ENCOUNTER') {
      flash('danger', resp.message || '遭遇野怪')
    } else if (resp.event === 'EXIT_REACHED') {
      flash('good', resp.message || '通关！')
      setTimeout(() => {
        localStorage.removeItem('mapInstanceId')
      }, 800)
    } else if (resp.event === 'STEPS_EXHAUSTED') {
      flash('warn', '步数耗尽，本次探索结束')
      localStorage.removeItem('mapInstanceId')
    }
  } catch (e) {
    error.value = e.response?.data?.message || '移动失败'
    if (e.response?.status === 400) {
      // 步数耗尽 / 实例已结束等
      localStorage.removeItem('mapInstanceId')
    }
  } finally {
    moving.value = false
  }
}

async function abandon() {
  if (!state.value) { emit('exit'); return }
  if (!confirm('确定要离开当前秘境吗？进度将丢失。')) return
  try {
    await http.post('/map/abandon', null, { params: { instanceId: state.value.instanceId } })
  } catch (e) { /* ignore */ }
  localStorage.removeItem('mapInstanceId')
  emit('exit')
}

function flash(type, msg) {
  event.value = { type, message: msg, ts: Date.now() }
  setTimeout(() => { if (event.value && Date.now() - event.value.ts >= 1900) event.value = null }, 2000)
}

function onKey(e) {
  if (!canMove.value) return
  const map = { ArrowUp: 'UP', ArrowDown: 'DOWN', ArrowLeft: 'LEFT', ArrowRight: 'RIGHT',
                KeyW: 'UP', KeyS: 'DOWN', KeyA: 'LEFT', KeyD: 'RIGHT' }
  const dir = map[e.code]
  if (dir) {
    e.preventDefault()
    move(dir)
  }
}

// ---------- Canvas ----------
function draw() {
  const canvas = canvasRef.value
  if (!canvas || !state.value) return
  const ctx = canvas.getContext('2d')
  const size = state.value.size
  const c = cellPx.value
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.font = `${Math.floor(c * 0.55)}px "KaiTi","STKaiti",serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  for (const cell of state.value.cells) {
    const x = cell.x * c
    const y = cell.y * c

    // 底色按类型
    drawCellBase(ctx, x, y, c, cell.type)

    // 迷雾覆盖
    if (cell.fog === 'HIDDEN') {
      ctx.fillStyle = 'rgba(20,18,15,0.92)'
      ctx.fillRect(x, y, c, c)
    } else if (cell.fog === 'MEMORY') {
      ctx.fillStyle = 'rgba(20,18,15,0.55)'
      ctx.fillRect(x, y, c, c)
      // MEMORY 时再画一个暗淡的字
      drawCellGlyph(ctx, x, y, c, cell.type, 0.45)
    } else {
      drawCellGlyph(ctx, x, y, c, cell.type, 1.0)
    }

    // 网格细线
    ctx.strokeStyle = 'rgba(31,29,26,0.18)'
    ctx.lineWidth = 1
    ctx.strokeRect(x + 0.5, y + 0.5, c - 1, c - 1)
  }

  // 玩家
  const px = state.value.posX * c
  const py = state.value.posY * c
  drawPlayer(ctx, px, py, c)
}

function drawCellBase(ctx, x, y, c, type) {
  ctx.fillStyle = '#f6f1e3'  // 宣纸底
  ctx.fillRect(x, y, c, c)
  if (type === 'OBSTACLE') ctx.fillStyle = '#a89a85'
  else if (type === 'RESOURCE') ctx.fillStyle = '#c5d4a5'
  else if (type === 'MONSTER') ctx.fillStyle = '#d8a89a'
  else if (type === 'EXIT') ctx.fillStyle = '#e8d28a'
  else ctx.fillStyle = '#f6f1e3'
  ctx.fillRect(x, y, c, c)
}

function drawCellGlyph(ctx, x, y, c, type, alpha) {
  ctx.save()
  ctx.globalAlpha = alpha
  if (type === 'OBSTACLE') {
    ctx.fillStyle = '#3a352d'
    ctx.fillText('岩', x + c / 2, y + c / 2 + 1)
  } else if (type === 'RESOURCE') {
    ctx.fillStyle = '#3f5a2a'
    ctx.fillText('草', x + c / 2, y + c / 2 + 1)
  } else if (type === 'MONSTER') {
    ctx.fillStyle = '#7a2a1c'
    ctx.fillText('妖', x + c / 2, y + c / 2 + 1)
  } else if (type === 'EXIT') {
    ctx.fillStyle = '#6b5418'
    ctx.fillText('出', x + c / 2, y + c / 2 + 1)
  }
  ctx.restore()
}

function drawPlayer(ctx, px, py, c) {
  // 圆点 + 你字
  ctx.save()
  // 蓝绿色圆
  ctx.fillStyle = 'rgba(31,90,140,0.18)'
  ctx.beginPath()
  ctx.arc(px + c / 2, py + c / 2, c * 0.42, 0, Math.PI * 2)
  ctx.fill()
  ctx.strokeStyle = '#1f5a8c'
  ctx.lineWidth = 2
  ctx.stroke()
  // 字
  ctx.fillStyle = '#1f5a8c'
  ctx.font = `bold ${Math.floor(c * 0.5)}px "KaiTi","STKaiti",serif`
  ctx.fillText('你', px + c / 2, py + c / 2 + 2)
  ctx.restore()
}
</script>

<template>
  <div class="map-wrap">
    <header class="map-header">
      <div class="title">
        <span class="seal">探</span>
        <div class="title-text">
          <h2>{{ state?.templateName || '秘境' }}</h2>
          <p v-if="state">
            步数 <b>{{ state.stepCount }}</b> / {{ state.maxSteps }}
            <span class="dot-sep">·</span>
            神识半径 <b>{{ state.radius }}</b>
            <span class="dot-sep">·</span>
            <span class="status-tag" :class="state.status">{{ statusText }}</span>
          </p>
        </div>
      </div>
      <button class="btn ghost" @click="abandon">离开秘境</button>
    </header>

    <!-- 进度条 -->
    <div v-if="state" class="step-bar"><div class="fill" :style="{ width: stepPct + '%' }"></div></div>

    <!-- 事件提示 -->
    <transition name="fade">
      <div v-if="event" class="event-toast" :class="event.type">{{ event.message }}</div>
    </transition>

    <p v-if="loading" class="tip">正在开启秘境…</p>
    <p v-else-if="error" class="error">{{ error }}<button class="btn inline" @click="emit('back')">返回主页</button></p>

    <template v-else-if="state">
      <div class="canvas-box">
        <canvas
          ref="canvasRef"
          :width="canvasSize"
          :height="canvasSize"
          class="map-canvas"
        ></canvas>
      </div>

      <div class="controls">
        <div></div>
        <button class="btn ctrl" :disabled="!canMove" @click="move('UP')">↑</button>
        <div></div>
        <button class="btn ctrl" :disabled="!canMove" @click="move('LEFT')">←</button>
        <div class="ctr-mid">你</div>
        <button class="btn ctrl" :disabled="!canMove" @click="move('RIGHT')">→</button>
        <div></div>
        <button class="btn ctrl" :disabled="!canMove" @click="move('DOWN')">↓</button>
        <div></div>
      </div>

      <p class="legend">
        <span><i class="dot rock"></i>岩</span>
        <span><i class="dot herb"></i>草</span>
        <span><i class="dot foe"></i>妖</span>
        <span><i class="dot out"></i>出</span>
        <span class="hint">WASD / 方向键移动</span>
      </p>

      <p v-if="state.status === 'COMPLETED'" class="banner ok">
        🎉 已走出秘境！本次探索结束。<button class="btn inline" @click="emit('back')">返回主页</button>
      </p>
      <p v-else-if="state.status === 'ABANDONED'" class="banner muted">
        已离开秘境。<button class="btn inline" @click="emit('back')">返回主页</button>
      </p>
    </template>
  </div>
</template>

<style scoped>
.map-wrap { animation: inkIn .5s ease-out both; }
.tip { text-align: center; color: var(--ink-light); padding: 18px 0; }
.error { color: var(--seal); text-align: center; margin: 14px 0; }

.map-header {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  margin-bottom: 14px;
}
.title { display: flex; align-items: center; gap: 12px; }
.seal {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border: 2px solid var(--ink); color: var(--ink);
  font-size: 18px; font-weight: 700; border-radius: 5px;
  background: rgba(31,29,26,.04); transform: rotate(-4deg);
  letter-spacing: 0;
}
.title-text h2 { margin: 0; font-size: 18px; letter-spacing: 4px; color: var(--ink); font-weight: 700; }
.title-text p { margin: 4px 0 0; font-size: 12px; color: var(--ink-light); letter-spacing: 1px; }
.dot-sep { color: var(--line); margin: 0 4px; }
.status-tag {
  display: inline-block; padding: 1px 8px; border-radius: 2px;
  font-size: 11px; letter-spacing: 1px; border: 1px solid var(--line);
}
.status-tag.ACTIVE { color: var(--seal); border-color: var(--seal); }
.status-tag.COMPLETED { color: #2d6b3a; border-color: #2d6b3a; }
.status-tag.ABANDONED { color: var(--ink-light); }

.step-bar { height: 4px; background: var(--line); border-radius: 2px; overflow: hidden; margin-bottom: 12px; }
.step-bar .fill { height: 100%; background: linear-gradient(90deg, #c5a36c, var(--seal)); transition: width .35s ease; }

.event-toast {
  text-align: center; padding: 10px 16px; margin-bottom: 12px; border-radius: 4px;
  font-size: 14px; letter-spacing: 2px; border: 1px solid var(--line);
}
.event-toast.info { color: var(--ink); background: rgba(31,29,26,.04); }
.event-toast.good { color: #2d6b3a; border-color: #2d6b3a; background: rgba(45,107,58,.06); }
.event-toast.warn { color: var(--seal); border-color: var(--seal); background: rgba(158,59,52,.06); }
.event-toast.danger { color: var(--seal); border-color: var(--seal); background: rgba(158,59,52,.10); font-weight: 700; }
.fade-enter-active, .fade-leave-active { transition: opacity .25s ease, transform .25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-4px); }

.canvas-box {
  display: flex; justify-content: center; margin-bottom: 14px;
}
.map-canvas {
  border: 1px solid var(--ink); border-radius: 4px;
  background: #f6f1e3;
  box-shadow: 0 2px 18px rgba(31,29,26,.10);
  max-width: 100%; height: auto;
}

.controls {
  display: grid; grid-template-columns: repeat(3, 60px);
  gap: 8px; justify-content: center; margin-bottom: 14px;
}
.btn.ctrl {
  width: 60px; height: 44px; padding: 0;
  font-size: 18px; line-height: 1; color: var(--ink);
  border: 1px solid var(--ink); border-radius: 4px; background: rgba(255,255,255,.5);
}
.btn.ctrl:hover:not(:disabled) { background: var(--ink); color: #fff; }
.btn.ctrl:disabled { opacity: .35; cursor: not-allowed; }
.ctr-mid {
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; color: var(--seal); font-weight: 700; letter-spacing: 2px;
  border: 1px dashed var(--line); border-radius: 4px;
}

.legend {
  display: flex; flex-wrap: wrap; gap: 14px; justify-content: center;
  margin: 0 0 14px; font-size: 12px; color: var(--ink-light); letter-spacing: 1px;
}
.legend span { display: inline-flex; align-items: center; gap: 5px; }
.legend .hint { color: var(--ink-soft); font-style: italic; }
.legend .dot {
  display: inline-block; width: 12px; height: 12px; border-radius: 2px;
  border: 1px solid var(--ink);
}
.legend .rock { background: #a89a85; }
.legend .herb { background: #c5d4a5; }
.legend .foe { background: #d8a89a; }
.legend .out { background: #e8d28a; }

.btn.ghost { border: 1px solid var(--line); background: transparent; color: var(--ink-light); padding: 6px 14px; font-size: 13px; border-radius: 3px; font-family: inherit; letter-spacing: 2px; }
.btn.ghost:hover { color: var(--ink); border-color: var(--ink); }
.btn.inline { display: inline-block; margin-left: 8px; padding: 2px 10px; font-size: 12px; border: 1px solid var(--ink); background: transparent; color: var(--ink); border-radius: 3px; font-family: inherit; cursor: pointer; letter-spacing: 1px; }
.btn.inline:hover { background: var(--ink); color: #fff; }

.banner {
  text-align: center; padding: 12px; border-radius: 4px;
  font-size: 14px; letter-spacing: 2px; margin: 0;
}
.banner.ok { color: #2d6b3a; background: rgba(45,107,58,.08); border: 1px solid #2d6b3a; }
.banner.muted { color: var(--ink-light); background: rgba(31,29,26,.04); border: 1px dashed var(--line); }
</style>