<template>
  <div class="panel">
    <h3>登录</h3>
    <div class="field">
      <label>道号（用户名）</label>
      <input v-model="username" placeholder="请输入用户名" />
    </div>
    <div class="field">
      <label>法咒（密码）</label>
      <input v-model="password" type="password" placeholder="请输入密码" @keyup.enter="submit" />
    </div>
    <button class="btn primary big" @click="submit">入道</button>
    <p class="tip" @click="$emit('switch', 'register')">尚无道号？前往注册</p>
    <p v-if="msg" class="msg">{{ msg }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

const emit = defineEmits(['switch', 'logged'])
const username = ref('')
const password = ref('')
const msg = ref('')

async function submit() {
  msg.value = ''
  try {
    const { data } = await http.post('/auth/login', {
      username: username.value,
      password: password.value
    })
    localStorage.setItem('token', data.data.token)
    emit('logged', data.data.user)
  } catch (e) {
    msg.value = e.response?.data?.message || '登录失败'
  }
}
</script>

<style scoped>
.msg { color: var(--seal); font-size: 13px; text-align: center; }
.tip {
  text-align: center; color: var(--seal); cursor: pointer; font-size: 13px; margin-top: 14px; letter-spacing: 1px;
}
</style>
