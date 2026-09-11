<script setup>
import { ref } from 'vue'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Home from './views/Home.vue'
import SpiritRootTest from './views/SpiritRootTest.vue'
import Map from './views/Map.vue'
import Inventory from './views/Inventory.vue'
import Combat from './views/Combat.vue'
import InkBackground from './components/InkBackground.vue'

const token = ref(localStorage.getItem('token'))
const view = ref(token.value ? 'home' : 'login')
// 进入秘境时携带的模板 code（map 视图按这个决定是新建实例还是恢复旧实例）
const pendingTemplateCode = ref('')

function onLogged() {
  token.value = localStorage.getItem('token')
  view.value = 'home'
}

function onLogout() {
  token.value = null
  view.value = 'login'
  // 清掉地图记忆
  localStorage.removeItem('mapInstanceId')
}

function goMap(templateCode) {
  pendingTemplateCode.value = templateCode || ''
  view.value = 'map'
}

function goInventory() {
  view.value = 'inventory'
}

function enterCombat(combatId) {
  localStorage.setItem('combatId', String(combatId))
  view.value = 'combat'
}

function exitCombat() {
  localStorage.removeItem('combatId')
  // 清掉 pendingTemplateCode，让回到 Map.vue 时走 localStorage 恢复路径
  // 而不是 /api/map/enter 开新实例
  pendingTemplateCode.value = ''
  view.value = 'map'
}
</script>

<template>
  <InkBackground />

  <div class="app" :class="{ wide: token }">
    <header>
      <div class="brand">
        <span class="seal">仙</span>
        <h1>修仙录</h1>
      </div>
      <p class="sub">测灵根 · 探秘境 · 战妖兽 · 炼丹修仙</p>
    </header>

    <template v-if="token">
      <Home v-if="view === 'home'" @logout="onLogout" @go-test="view = 'test'"
            @enter-map="goMap" @go-inventory="goInventory" />
      <SpiritRootTest v-else-if="view === 'test'" @done="view = 'home'" @cancel="view = 'home'" />
      <Map v-else-if="view === 'map'" :template-code="pendingTemplateCode"
           @exit="view = 'home'" @back="view = 'home'"
           @combat="enterCombat" />
      <Inventory v-else-if="view === 'inventory'" @back="view = 'home'" />
      <Combat v-else-if="view === 'combat'" @exit="exitCombat" />
    </template>

    <template v-else>
      <Login v-if="view === 'login'" @switch="view = 'register'" @logged="onLogged" />
      <Register v-else @switch="view = 'login'" @logged="onLogged" />
    </template>

    <footer>修仙录 MVP · 后端 Spring Boot + MyBatis-Plus + H2 · 前端 Vue3</footer>
  </div>
</template>

<style scoped>
.app {
  max-width: 720px; margin: 0 auto; padding: 32px 20px 60px;
  position: relative; z-index: 1;          /* 浮于水墨背景之上 */
  animation: inkIn .7s ease-out both;      /* 墨落宣纸的入场 */
  transition: max-width .25s ease;
}
/* 「我的」页含行囊两栏布局时放宽容器（仿 xiuxian .app.wide） */
.app.wide { max-width: 1080px; }
header { text-align: center; margin-bottom: 22px; position: relative; }
.brand { display: inline-flex; align-items: center; gap: 12px; justify-content: center; }
.seal {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border: 2px solid var(--seal); color: var(--seal);
  font-size: 20px; font-weight: 700; border-radius: 5px; background: rgba(158,59,52,.06);
  line-height: 1; transform: rotate(-4deg);
  box-shadow: 0 0 0 3px rgba(158,59,52,.05), 0 1px 6px rgba(158,59,52,.18);
  animation: sealStamp .8s .15s cubic-bezier(.2,.8,.3,1) both;
}
h1 {
  margin: 0; font-size: 30px; letter-spacing: 6px; color: var(--ink); font-weight: 700;
  position: relative; padding-bottom: 12px;
}
/* 标题笔触：一道由左扫出的墨线 */
h1::after {
  content: ""; position: absolute; left: 6%; right: 6%; bottom: 2px; height: 3px;
  border-radius: 50%; transform-origin: left center;
  background: linear-gradient(90deg,
    transparent, rgba(31,29,26,.5) 8%, rgba(31,29,26,.18) 45%, rgba(31,29,26,.55) 80%, transparent);
  filter: blur(.4px);
  animation: brushSweep .9s .25s ease-out both;
}
.sub { color: var(--ink-light); font-size: 14px; margin-top: 8px; letter-spacing: 1px; }

/* 登录/注册通过各页面内的链接互跳，无 tab 样式 */

footer {
  text-align: center; color: var(--ink-light); font-size: 12px;
  margin-top: 36px; padding-top: 18px; letter-spacing: 1px; position: relative;
}
footer::before {
  content: ""; position: absolute; top: 0; left: 22%; right: 22%; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(31,29,26,.2), transparent);
}
</style>
