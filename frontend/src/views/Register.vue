<template>
  <div class="panel">
    <h3>注册</h3>
    <div class="field">
      <label>道号（用户名）</label>
      <input v-model="username" placeholder="3-50 位" />
    </div>
    <div class="field">
      <label>道号昵称（展示名）</label>
      <input v-model="nickname" placeholder="1-50 位，可后续修改" />
    </div>
    <div class="field">
      <label>法咒（密码）</label>
      <input v-model="password" type="password" placeholder="6-64 位" @keyup.enter="submit" />
    </div>
    <button class="btn primary big" @click="submit">开立道号</button>
    <p class="tip" @click="$emit('switch', 'login')">已有道号？前往登录</p>
    <p v-if="msg" class="msg">{{ msg }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

const emit = defineEmits(['switch', 'logged'])
const nickname = ref('')
const username = ref('')
const password = ref('')
const msg = ref('')

async function submit() {
  msg.value = ''
  if (!nickname.value.trim()) {
    msg.value = '请填写昵称'
    return
  }
  try {
    const { data } = await http.post('/auth/register', {
      nickname: nickname.value.trim(),
      username: username.value,
      password: password.value
    })
    localStorage.setItem('token', data.data.token)
    emit('logged', data.data.user)
  } catch (e) {
    msg.value = e.response?.data?.message || '注册失败'
  }
}
</script>

<style scoped>
.msg { color: var(--seal); font-size: 13px; text-align: center; }
.tip {
  text-align: center; color: var(--seal); cursor: pointer; font-size: 13px; margin-top: 14px; letter-spacing: 1px;
}
</style>
