<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../api/http'

const emit = defineEmits(['done', 'cancel'])
const questions = ref([])
const current = ref(0)
const answers = ref([-1, -1, -1, -1, -1])
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const result = ref(null)

const total = 5
const progress = computed(() => (current.value + 1) / total * 100)
const allAnswered = computed(() => answers.value.every(a => a >= 0))
const rootNames = computed(() => result.value?.roots.map(r => r.name).join(' · ') || '')

onMounted(async () => {
  try {
    const { data } = await http.get('/spirit-root/questions')
    questions.value = data.data
  } catch (e) {
    error.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function pick(optIdx) {
  answers.value[current.value] = optIdx
}
function next() {
  if (answers.value[current.value] < 0) return
  if (current.value < total - 1) current.value++
}
function prev() {
  if (current.value > 0) current.value--
}
async function submit() {
  if (!allAnswered.value) return
  submitting.value = true
  error.value = ''
  try {
    const { data } = await http.post('/spirit-root/test', { answers: answers.value })
    result.value = data.data
  } catch (e) {
    error.value = e.response?.data?.message || '测灵根失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="test">
    <p v-if="loading" class="tip">正在开启灵台…</p>
    <p v-else-if="error && !result" class="error">{{ error }}</p>

    <!-- 揭晓结果 -->
    <section v-else-if="result" class="panel result-card">
      <div class="r-seal">灵</div>
      <div class="r-title">
        <span class="r-main">{{ rootNames }}</span>
        <span v-if="result.dual" class="r-tag">双灵根之体</span>
      </div>
      <p class="r-hint">
        {{ result.dual
            ? '你觉醒了 2 个灵根，可修炼更广的功法。'
            : '你觉醒了 1 个灵根。灵根决定日后可修炼的功法。' }}
      </p>
      <button class="btn primary big" @click="emit('done')">返回我的页面</button>
    </section>

    <!-- 答题 -->
    <template v-else-if="questions.length">
      <div class="progress"><div class="fill" :style="{ width: progress + '%' }"></div></div>
      <p class="q-num">第 {{ current + 1 }} 题 / 共 {{ total }} 题</p>

      <section class="panel q-card">
        <h3 class="q-text">{{ questions[current].text }}</h3>
        <div class="options">
          <button v-for="opt in questions[current].options" :key="opt.index"
            class="opt" :class="{ active: answers[current] === opt.index }"
            @click="pick(opt.index)">
            <span class="opt-mark">{{ ['甲', '乙', '丙', '丁'][opt.index] }}</span>
            <span class="opt-text">{{ opt.text }}</span>
          </button>
        </div>
      </section>

      <div class="nav">
        <button class="btn" :disabled="current === 0" @click="prev">上一题</button>
        <button v-if="current < total - 1" class="btn primary" :disabled="answers[current] < 0" @click="next">下一题</button>
        <button v-else class="btn primary" :disabled="!allAnswered || submitting" @click="submit">
          {{ submitting ? '揭晓中…' : '揭晓灵根' }}
        </button>
      </div>
      <button class="btn ghost" @click="emit('cancel')">稍后再测</button>
    </template>
  </div>
</template>

<style scoped>
.test { animation: inkIn .5s ease-out both; }
.tip { text-align: center; color: var(--ink-light); font-size: 14px; padding: 18px 0; }
.error { color: var(--seal); text-align: center; margin: 16px 0; font-size: 14px; }

/* 进度条 */
.progress { height: 4px; background: var(--line); border-radius: 2px; overflow: hidden; margin-bottom: 8px; }
.progress .fill { height: 100%; background: var(--ink); transition: width .35s ease; }
.q-num { text-align: center; color: var(--ink-light); font-size: 13px; margin: 0 0 12px; letter-spacing: 2px; }

/* 通用面板 */
.panel {
  position: relative; overflow: hidden; background: rgba(255,255,255,.55);
  border: 1px solid var(--line); border-radius: 4px; padding: 22px 24px;
  box-shadow: 0 2px 18px rgba(31,29,26,.06); backdrop-filter: blur(2px);
  margin-bottom: 16px;
}
.panel::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(ellipse 60% 45% at 16% 0%, rgba(31,29,26,.05), transparent 62%);
}
.panel > * { position: relative; }

/* 题卡 */
.q-card { padding: 28px 24px; }
.q-text { margin: 0 0 18px; font-size: 17px; color: var(--ink); font-weight: 600; letter-spacing: 2px; line-height: 1.7; }
.options { display: grid; gap: 12px; }
.opt {
  display: flex; align-items: center; gap: 12px; text-align: left;
  background: rgba(255,255,255,.4); border: 1px solid var(--line); border-radius: 4px;
  padding: 12px 16px; font-size: 14px; color: var(--ink-soft); font-family: inherit;
  transition: all .2s; cursor: pointer;
}
.opt:hover { border-color: var(--ink); color: var(--ink); background: rgba(255,255,255,.7); }
.opt.active {
  border-color: var(--seal); color: var(--ink); background: rgba(158,59,52,.06);
  box-shadow: 0 0 0 3px rgba(158,59,52,.08);
}
.opt-mark {
  flex: none; width: 26px; height: 26px; display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: var(--ink-light); border: 1px solid var(--line); border-radius: 50%;
}
.opt.active .opt-mark { color: var(--seal); border-color: var(--seal); }
.opt-text { letter-spacing: 1px; line-height: 1.6; }

/* 按钮 */
.nav { display: flex; justify-content: space-between; gap: 12px; margin-top: 14px; }
.btn {
  border: 1px solid var(--ink); border-radius: 4px; padding: 9px 22px; font-size: 14px;
  background: transparent; color: var(--ink); font-family: inherit;
  letter-spacing: 2px; transition: all .2s; cursor: pointer;
}
.btn:hover:not(:disabled) { background: var(--ink); color: #fff; }
.btn:disabled { opacity: .45; cursor: not-allowed; }
.btn.primary { border-color: var(--seal); color: var(--seal); }
.btn.primary:hover:not(:disabled) { background: var(--seal); color: #fff; }
.btn.big { padding: 11px 32px; font-size: 15px; }
.btn.ghost { width: 100%; margin-top: 12px; border: none; color: var(--ink-light); font-size: 13px; letter-spacing: 1px; }
.btn.ghost:hover { background: transparent; color: var(--ink); }

/* 结果卡 */
.result-card { text-align: center; padding: 32px 24px; border-top: 3px solid var(--seal); }
.r-seal {
  display: inline-flex; align-items: center; justify-content: center;
  width: 56px; height: 56px; font-size: 26px; font-weight: 700;
  border: 2px solid var(--seal); border-radius: 6px; transform: rotate(-4deg);
  color: var(--seal); background: rgba(255,255,255,.4); margin-bottom: 14px;
}
.r-title { font-size: 30px; font-weight: 700; color: var(--ink); letter-spacing: 8px; margin-bottom: 8px; }
.r-main { color: var(--seal); }
.r-tag {
  display: inline-block; font-size: 11px; letter-spacing: 2px; color: var(--seal);
  border: 1px solid var(--seal); padding: 2px 8px; border-radius: 3px;
  margin-left: 10px; vertical-align: middle; transform: rotate(-2deg);
}
.r-hint { margin: 10px 0 20px; color: var(--ink-soft); font-size: 14px; line-height: 1.8; letter-spacing: 1px; }
</style>