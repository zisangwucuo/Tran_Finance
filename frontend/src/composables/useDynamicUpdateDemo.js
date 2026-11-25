import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

// 本模块仅管理展示页的内存状态。情景数值、版本和事件均不读取真实业务数据，
// 不调用训练、评分、发布接口，也不写入 localStorage；离开页面即销毁。
export const updateIndustries = ['电力行业', '钢铁行业', '有色金属行业', '航空机场行业', '基础化工行业', '建材行业', '煤炭行业', '石油石化行业']
export const updateScenarios = [
  { value: 'stable', label: '稳态监测' },
  { value: 'local', label: '局部变化' },
  { value: 'structural', label: '结构变化' }
]

// 前三类外部信号在本页保持稳定，只作背景监测。自动更新仅由模型表现和数据漂移触发。
// 两级阈值仅用于前端流程展示：达到更新阈值走增量分支，达到重训阈值优先走全量分支。
export const updateThresholds = {
  auc: { incremental: 0.05, full: 0.10 },
  psi: { incremental: 0.25, full: 0.40 }
}
const observations = {
  stable: { policy: 0, market: 1.2, distribution: 3.4, aucDrop: 0.006, psi: 0.04, samples: 18 },
  local: { policy: 0, market: 1.2, distribution: 3.4, aucDrop: 0.063, psi: 0.28, samples: 68 },
  structural: { policy: 0, market: 1.2, distribution: 3.4, aucDrop: 0.126, psi: 0.43, samples: 126 }
}

export const updatePaths = [
  {
    key: 'incremental', title: '增量更新', subtitle: '模型表现回落 · 数据漂移 · 局部调整',
    steps: [
      { key: 'incremental-data', label: '新增数据接入', note: '接入新增样本' },
      { key: 'incremental-weights', label: '局部参数更新', note: '调整参数与指标权重' },
      { key: 'incremental-validate', label: '快速验证', note: '检查稳定性与有效性' },
      { key: 'incremental-version', label: '生成增量版本', note: '保留基础版本' }
    ]
  },
  {
    key: 'full', title: '全量更新', subtitle: '模型表现明显下降 · 数据漂移加剧',
    steps: [
      { key: 'full-data', label: '整理历史数据', note: '重整全量历史样本' },
      { key: 'full-train', label: '重新训练模型', note: '重新拟合评估模型' },
      { key: 'full-weights', label: '重算指标权重', note: '重建指标权重体系' },
      { key: 'full-validate', label: '完整验证', note: '完成全量验证' },
      { key: 'full-version', label: '生成全量版本', note: '建立新基础版本' }
    ]
  }
]

export const publishSteps = [
  { key: 'publish-version', label: '模型版本管理与发布', note: '版本登记与发布' },
  { key: 'publish-service', label: '评分服务更新', note: '切换评估模型版本' },
  { key: 'publish-scores', label: '企业评分结果更新', note: '同步转型综合评分' }
]
export const feedbackSteps = [
  { key: 'feedback-weights', label: '指标权重变化', note: '指标权重快照已生成' },
  { key: 'feedback-scores', label: '企业评分变化', note: '评分更新结果已汇总' },
  { key: 'feedback-performance', label: '模型效果变化', note: '模型验证结果已归档' }
]

export function useDynamicUpdateDemo() {
  const industry = ref(updateIndustries[0])
  const scenario = ref('local')
  const speed = ref(1)
  const status = ref('idle')
  const stepIndex = ref(-1)
  const stepFraction = ref(0)
  const events = ref([])
  const version = ref({ major: 1, minor: 0 })
  const runVersion = ref('v1.0.0')
  let timer = null
  let lastTick = 0
  let eventSequence = 0

  const observation = computed(() => observations[scenario.value])
  const conditions = computed(() => {
    const value = observation.value
    return [
      { key: 'policy', label: '政策发生调整', rule: '背景监测 · 政策口径保持稳定', current: ['无调整', '条款修订', '体系调整'][value.policy], hit: false },
      { key: 'market', label: '市场波动超过阈值', rule: '背景监测 · 市场波动保持平稳', current: `${value.market.toFixed(1)}%`, hit: false },
      { key: 'distribution', label: '行业指标分布变化', rule: '背景监测 · 行业指标保持稳定', current: `${value.distribution.toFixed(1)}%`, hit: false },
      { key: 'auc', label: '模型表现下降', rule: `AUC 下降 ≥ ${updateThresholds.auc.incremental.toFixed(2)}`, current: value.aucDrop.toFixed(3), hit: value.aucDrop >= updateThresholds.auc.incremental, full: value.aucDrop >= updateThresholds.auc.full },
      { key: 'psi', label: '数据漂移达到阈值', rule: `漂移指数 PSI ≥ ${updateThresholds.psi.incremental.toFixed(2)}`, current: value.psi.toFixed(2), hit: value.psi >= updateThresholds.psi.incremental, full: value.psi >= updateThresholds.psi.full }
    ]
  })
  const selectedPath = computed(() => conditions.value.some(item => item.hit && item.full)
    ? 'full' : conditions.value.some(item => item.hit) ? 'incremental' : 'stable')
  const pathLabel = computed(() => ({ full: '全量更新', incremental: '增量更新', stable: '继续监测' })[selectedPath.value])
  const versionLabel = computed(() => `v${version.value.major}.${version.value.minor}.0`)
  const candidateVersion = computed(() => selectedPath.value === 'stable' ? runVersion.value
    : `v${version.value.major + (selectedPath.value === 'full' ? 1 : 0)}.${selectedPath.value === 'full' ? 0 : version.value.minor + 1}.0`)
  const decisionText = computed(() => ({
    full: '模型表现或数据漂移达到重训阈值，重新训练并完成全量验证。',
    incremental: '模型表现或数据漂移触发更新，调整局部参数与指标权重。',
    stable: '模型表现与数据漂移均未达到更新阈值，保留当前版本并继续监测。'
  })[selectedPath.value])
  const sources = computed(() => [
    { key: 'monitor-policy', label: '政策规则变化', value: conditions.value[0].current, detail: '政策口径与披露要求', tone: 'teal' },
    { key: 'monitor-market', label: '市场波动', value: `${observation.value.market.toFixed(1)}%`, detail: '市场波动幅度', tone: 'blue' },
    { key: 'monitor-industry', label: '行业指标变化', value: `${observation.value.distribution.toFixed(1)}%`, detail: '行业指标分布偏移', tone: 'amber' },
    { key: 'monitor-model', label: '模型表现与数据漂移', value: `PSI ${observation.value.psi.toFixed(2)}`, detail: `AUC 回落 ${observation.value.aucDrop.toFixed(3)}`, tone: 'violet' }
  ])

  // 时间线是唯一的流程驱动源。图中的连线、节点、进度和日志都从它派生，
  // 不单独开启多个定时器，确保暂停、切换情景后不会残留上一条分支的动画。
  const timeline = computed(() => {
    const initial = sources.value.map(item => ({ ...item, group: 'monitor' }))
    const decision = { key: 'decision', label: '更新条件判断', note: decisionText.value, group: 'decision' }
    const loop = { key: 'loop', label: '回到持续监测', note: '结果反馈已回流，进入下一轮监测', group: 'loop' }
    if (selectedPath.value === 'stable') {
      return [...initial, decision, { ...loop, note: '未触发更新，保留当前模型版本' }]
    }
    const branch = updatePaths.find(item => item.key === selectedPath.value)
    return [...initial, decision, ...branch.steps.map(item => ({ ...item, group: 'update' })),
      ...publishSteps.map(item => ({ ...item, group: 'publish' })),
      ...feedbackSteps.map(item => ({ ...item, group: 'feedback' })), loop]
  })
  const currentStep = computed(() => timeline.value[stepIndex.value])
  const decisionReady = computed(() => stepIndex.value > 4 || status.value === 'complete')
  const progress = computed(() => status.value === 'complete' ? 100 : stepIndex.value < 0 ? 0
    : Math.floor((stepIndex.value + stepFraction.value) / timeline.value.length * 100))
  const completedCount = computed(() => status.value === 'complete' ? timeline.value.length : Math.max(0, stepIndex.value))
  const statusLabel = computed(() => ({ idle: '等待启动', running: '监测运行中', paused: '已暂停', complete: '本轮已完成' })[status.value])

  function nodeState(key) {
    const index = timeline.value.findIndex(item => item.key === key)
    if (index < 0) return decisionReady.value ? 'skipped' : 'waiting'
    if (status.value === 'complete' || index < stepIndex.value) return 'done'
    if (index === stepIndex.value) return 'active'
    return 'waiting'
  }
  function recordEvent(title, detail, type = 'done') {
    events.value.unshift({ id: ++eventSequence, time: new Date().toLocaleTimeString('zh-CN', { hour12: false }), title, detail, type })
    // 限制列表长度，重复执行不会让当前视图无限积累日志。
    events.value = events.value.slice(0, 30)
  }
  function stopClock() {
    if (timer !== null) window.clearInterval(timer)
    timer = null
  }
  function finish() {
    stopClock()
    status.value = 'complete'
    if (selectedPath.value === 'full') version.value = { major: version.value.major + 1, minor: 0 }
    else if (selectedPath.value === 'incremental') version.value = { ...version.value, minor: version.value.minor + 1 }
    recordEvent(selectedPath.value === 'stable' ? '本轮无需更新' : `${pathLabel.value}完成`,
      selectedPath.value === 'stable' ? `${versionLabel.value} 保持不变` : `${runVersion.value} → ${versionLabel.value} · 反馈已回流`)
  }
  function startClock() {
    stopClock()
    lastTick = performance.now()
    timer = window.setInterval(() => {
      const now = performance.now()
      // 限制单次经过时间，避免后台标签页恢复时一下跳过多个节点。
      stepFraction.value += Math.min(now - lastTick, 250) * speed.value / 1400
      lastTick = now
      if (stepFraction.value < 1) return
      const step = currentStep.value
      recordEvent(step.label, step.note || `${industry.value} · ${step.value}`)
      stepFraction.value = 0
      if (stepIndex.value === timeline.value.length - 1) finish()
      else stepIndex.value += 1
    }, 80)
  }
  function play() {
    if (status.value === 'running') return
    if (status.value === 'paused') {
      status.value = 'running'
      startClock()
      return
    }
    runVersion.value = versionLabel.value
    stepIndex.value = 0
    stepFraction.value = 0
    events.value = []
    status.value = 'running'
    recordEvent('监测任务启动', `${industry.value} · ${updateScenarios.find(item => item.value === scenario.value).label}`, 'info')
    startClock()
  }
  function pause() {
    if (status.value !== 'running') return
    stopClock()
    status.value = 'paused'
  }
  function reset() {
    stopClock()
    status.value = 'idle'
    stepIndex.value = -1
    stepFraction.value = 0
    events.value = []
    version.value = { major: 1, minor: 0 }
    runVersion.value = 'v1.0.0'
  }

  // 行业、情景切换时清空本页状态；离开页面和隐藏浏览器标签页时停止推进。
  // 切回标签页后由用户继续，确保演示不会在不可见状态悄悄完成。
  function handleVisibility() {
    if (document.hidden) pause()
  }
  watch([industry, scenario], reset, { flush: 'sync' })
  onMounted(() => document.addEventListener('visibilitychange', handleVisibility))
  onBeforeUnmount(() => {
    stopClock()
    document.removeEventListener('visibilitychange', handleVisibility)
  })

  return { industry, scenario, speed, status, events, observation, conditions, sources, selectedPath, pathLabel,
    versionLabel, runVersion, candidateVersion, decisionText, decisionReady, currentStep, progress, completedCount,
    timeline, statusLabel, nodeState, play, pause, reset }
}
