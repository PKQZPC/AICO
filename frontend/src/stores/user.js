// src/stores/user.js
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null
  }),
  actions: {
    setUserInfo(info) {
      this.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    },
    loadUserInfo() {
      const info = localStorage.getItem('userInfo')
      if (info) this.userInfo = JSON.parse(info)
    }
  }
})