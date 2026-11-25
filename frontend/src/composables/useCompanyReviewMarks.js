import { ref, onMounted } from 'vue'
import request from '../api/request'

/** 从服务端当前会话读取本行标记，避免本地缓存串到其他银行账号。 */
export function useCompanyReviewMarks() {
  const codes = ref(new Set())
  const ready = ref(false)
  const isMarked = row => Boolean(row?.companyCode && codes.value.has(row.companyCode))
  async function reloadMarks() {
    ready.value = false
    try {
      codes.value = new Set(await request.get('/company-review-marks'))
      ready.value = true
    } catch { codes.value = new Set() /* 统一请求模块已提示错误，不影响原始数据加载。 */ }
  }
  async function markCompany(row) {
    await request.post('/company-review-marks', { companyCode: row.companyCode })
    codes.value = new Set([...codes.value, row.companyCode])
  }
  onMounted(reloadMarks)
  return { isMarked, markCompany, ready }
}
