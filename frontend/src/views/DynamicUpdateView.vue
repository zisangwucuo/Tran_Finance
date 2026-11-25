<template>
  <div class="dynamic-update" :class="{ 'motion-paused': status !== 'running' }">
    <header class="update-heading">
      <div><h1>动态更新</h1><p>条件驱动的模型更新与反馈闭环</p></div>
      <div class="update-heading-actions">
        <span class="run-status" :class="status"><i />{{ statusLabel }}</span>
        <el-button :icon="Document" @click="rulesVisible = true">更新规则</el-button>
      </div>
    </header>

    <div class="update-toolbar">
      <div class="update-select"><label for="update-industry">所属行业</label>
        <el-select id="update-industry" v-model="industry" aria-label="所属行业">
          <el-option v-for="name in updateIndustries" :key="name" :label="name" :value="name" />
        </el-select>
      </div>
      <div class="update-scenarios"><span id="scenario-label">监测情景</span>
        <el-radio-group v-model="scenario" aria-labelledby="scenario-label">
          <el-radio-button v-for="item in updateScenarios" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
        </el-radio-group>
      </div>
      <div class="update-playback">
        <el-select v-model="speed" class="speed-select" aria-label="流程速度">
          <el-option label="1× 速度" :value="1" /><el-option label="2× 速度" :value="2" />
        </el-select>
        <el-tooltip content="重置流程"><el-button :icon="RefreshLeft" aria-label="重置流程" @click="reset" /></el-tooltip>
        <el-button v-if="status === 'running'" type="primary" :icon="VideoPause" @click="pause">暂停流程</el-button>
        <el-button v-else type="primary" :icon="VideoPlay" @click="play">{{ status === 'paused' ? '继续流程' : status === 'complete' ? '再次监测' : '开始监测' }}</el-button>
      </div>
    </div>

    <dl class="update-summary">
      <div><dt>监测来源 <el-icon><Monitor /></el-icon></dt><dd>4<small>类信号</small></dd><span>政策 · 市场 · 行业 · 模型</span></div>
      <div><dt>更新条件 <el-icon><SetUp /></el-icon></dt><dd>{{ decisionReady ? conditions.filter(item => item.hit).length : '—' }}<small>/ 5 项命中</small></dd><span>{{ decisionReady ? '已完成条件判断' : '等待监测与条件判断' }}</span></div>
      <div><dt>更新策略 <el-icon><Connection /></el-icon></dt><dd class="summary-strategy">{{ decisionReady ? pathLabel : '等待判定' }}</dd><span>{{ decisionReady ? selectedPath === 'stable' ? '维持现有模型' : selectedPath === 'full' ? '重建模型与权重体系' : '保留基础模型，局部更新' : '按变化程度选择更新路径' }}</span></div>
      <div><dt>当前模型版本 <el-icon><Files /></el-icon></dt><dd class="summary-version">{{ versionLabel }}</dd><span>{{ industry }} · 机构模型</span></div>
    </dl>

    <section class="update-flow" aria-label="动态更新流程">
      <div class="flow-heading"><h2>监测与决策</h2><div class="flow-legend"><span><i class="active" />进行中</span><span><i class="done" />已完成</span><span><i />待执行</span></div></div>
      <div class="flow-columns">
        <section class="source-column">
          <div class="step-heading"><b>01</b><h3>多源数据与环境监测</h3></div>
          <div class="source-list">
            <div v-for="(source, index) in sources" :key="source.key" class="source-item" :class="[source.tone, nodeState(source.key)]">
              <span class="source-icon"><el-icon><component :is="sourceIcons[index]" /></el-icon></span>
              <div class="source-copy"><h4>{{ source.label }}</h4><p>{{ source.detail }}</p></div>
              <div class="source-reading"><strong>{{ source.value }}</strong><span>{{ stateLabel(nodeState(source.key)) }}</span></div>
              <div v-if="nodeState(source.key) === 'active'" class="source-scan" />
            </div>
          </div>
          <div class="monitor-footer"><el-icon><CircleCheck /></el-icon><span>动态监测与变化识别</span><span class="monitor-pips"><i /><i /><i /></span></div>
        </section>

        <section class="decision-column">
          <div class="step-heading"><b>02</b><h3>更新条件判断</h3></div>
          <ul class="condition-list">
            <li v-for="condition in conditions" :key="condition.key" :class="{ hit: decisionReady && condition.hit }">
              <el-icon><CircleCheck v-if="decisionReady && condition.hit" /><Remove v-else /></el-icon>
              <div><strong>{{ condition.label }}</strong><span>{{ condition.rule }}</span></div>
              <b>{{ condition.current }}</b>
            </li>
          </ul>
          <div class="decision-result" :class="[nodeState('decision'), { 'no-update': decisionReady && selectedPath === 'stable' }]" aria-live="polite">
            <el-icon><component :is="decisionReady ? selectedPath === 'stable' ? Refresh : Connection : SetUp" /></el-icon>
            <div><strong>{{ decisionReady ? pathLabel : nodeState('decision') === 'active' ? '正在判断更新条件' : '是否满足更新条件？' }}</strong><span>{{ decisionReady ? decisionText : '根据监测信号匹配更新路径' }}</span></div>
          </div>
        </section>

        <section class="branch-column">
          <div class="step-heading"><b>03</b><h3>模型更新</h3><span class="heading-note">按条件选择路径</span></div>
          <div v-for="path in updatePaths" :key="path.key" class="update-branch" :class="[path.key, { chosen: decisionReady && selectedPath === path.key, bypassed: decisionReady && selectedPath !== path.key }]" :data-path="path.key">
            <div class="branch-heading"><div><el-icon><component :is="path.key === 'full' ? Refresh : Plus" /></el-icon><h4>{{ path.title }}</h4></div><span class="branch-badge">{{ branchStatus(path) }}</span></div>
            <p>{{ path.subtitle }}</p>
            <ol class="branch-steps" :style="{ '--step-count': path.steps.length }">
              <li v-for="(step, index) in path.steps" :key="step.key" :class="nodeState(step.key)" :data-node="step.key">
                <span class="node-circle"><el-icon v-if="nodeState(step.key) === 'done'"><Check /></el-icon><span v-else>{{ String(index + 1).padStart(2, '0') }}</span></span>
                <strong>{{ step.label }}</strong>
              </li>
            </ol>
          </div>
          <div class="stable-path" :class="{ selected: decisionReady && selectedPath === 'stable' }"><el-icon><Refresh /></el-icon><span>未满足更新条件</span><el-icon><Right /></el-icon><strong>继续监测</strong></div>
        </section>
      </div>

      <div class="delivery-row">
        <section class="publish-column">
          <div class="step-heading"><b>04</b><h3>版本发布与评分同步</h3><span class="heading-note">{{ decisionReady && selectedPath !== 'stable' ? status === 'complete' ? versionLabel : `${runVersion} → ${candidateVersion}` : '等待更新结果' }}</span></div>
          <ol class="publish-steps">
            <li v-for="(step, index) in publishSteps" :key="step.key" :class="nodeState(step.key)" :data-node="step.key">
              <el-icon><component :is="nodeState(step.key) === 'done' ? CircleCheckFilled : publishIcons[index]" /></el-icon>
              <div><strong>{{ step.label }}</strong><span>{{ nodeState(step.key) === 'skipped' ? '本轮无需更新' : stateLabel(nodeState(step.key)) }}</span></div>
            </li>
          </ol>
        </section>
        <section class="feedback-column">
          <div class="step-heading"><b>05</b><h3>结果反馈</h3></div>
          <div class="feedback-items">
            <div v-for="(step, index) in feedbackSteps" :key="step.key" :class="nodeState(step.key)" :data-node="step.key"><el-icon><component :is="feedbackIcons[index]" /></el-icon><strong>{{ step.label }}</strong><span>{{ stateLabel(nodeState(step.key)) }}</span></div>
          </div>
        </section>
      </div>
      <div class="loop-track" :class="nodeState('loop')"><el-icon><Refresh /></el-icon><span>持续迭代与闭环更新</span><div class="loop-line"><i /></div><small>{{ status === 'complete' ? '结果已回流至监测环节' : '监测 · 判断 · 更新 · 反馈' }}</small><el-icon><Back /></el-icon></div>
    </section>

    <section class="update-activity">
      <div class="task-panel">
        <div class="activity-heading"><h2>当前任务</h2><span>{{ completedCount }} / {{ timeline.length }} 个环节</span><el-button v-if="completedResult" link type="primary" @click="resultVisible = true">查看结果</el-button></div>
        <div class="task-progress"><strong data-testid="progress-value">{{ progress }}<small>%</small></strong><span>{{ status === 'complete' ? selectedPath === 'stable' ? '无需更新，保持当前版本' : '更新完成，进入持续监测' : currentStep?.label || '等待启动监测' }}</span></div>
        <div class="task-progress-track" role="progressbar" :aria-valuenow="progress" aria-valuemin="0" aria-valuemax="100" aria-label="当前更新任务进度"><i :style="{ transform: `scaleX(${progress / 100})` }" /></div>
        <div class="task-foot"><span>本轮新增样本 <b>{{ observation.samples }}</b> 条</span><span>{{ status === 'paused' ? '流程已暂停' : `${speed}× 速度` }}</span></div>
      </div>
      <div class="event-panel">
        <div class="activity-heading"><h2>流程事件</h2><span>最近 {{ events.length }} 条</span></div>
        <div v-if="!events.length" class="events-empty"><el-icon><Clock /></el-icon><span>等待监测任务启动</span></div>
        <TransitionGroup v-else name="update-event" tag="ol" class="event-list" aria-label="流程事件记录">
          <li v-for="event in events.slice(0, 4)" :key="event.id"><time>{{ event.time }}</time><i :class="event.type" /><strong>{{ event.title }}</strong><span>{{ event.detail }}</span></li>
        </TransitionGroup>
      </div>
    </section>

    <el-dialog v-model="resultVisible" title="检测与更新结果" width="min(620px, calc(100vw - 32px))" align-center :close-on-click-modal="false" class="update-result-dialog">
      <div v-if="completedResult" class="completion-content">
        <div class="completion-heading">
          <el-icon><CircleCheckFilled /></el-icon>
          <div><h3>{{ completedResult.title }}</h3><p>{{ completedResult.industry }} · {{ completedResult.hits.length }} / 5 项条件命中</p></div>
        </div>
        <p class="completion-summary">{{ completedResult.summary }}</p>
        <template v-if="completedResult.hits.length">
          <h4>本轮检测到的变化</h4>
          <dl class="completion-conditions"><div v-for="item in completedResult.hits" :key="item.key"><dt>{{ item.label }}</dt><dd>{{ item.current }}</dd></div></dl>
        </template>
        <div v-else class="completion-stable"><el-icon><CircleCheck /></el-icon><span>政策、市场、行业指标、模型表现及数据漂移均未触发更新条件。</span></div>
        <div class="completion-version"><span>模型版本</span><strong>{{ completedResult.path === 'stable' ? `${completedResult.afterVersion}（保持不变）` : `${completedResult.beforeVersion} → ${completedResult.afterVersion}` }}</strong></div>
        <p class="completion-feedback">{{ completedResult.path === 'stable' ? '保留当前模型，进入下一轮持续监测。' : '版本发布、评分服务和企业评分同步已完成，结果已回流至监测环节。' }}</p>
      </div>
      <template #footer><el-button type="primary" @click="resultVisible = false">知道了</el-button></template>
    </el-dialog>

    <el-dialog v-model="rulesVisible" title="动态更新规则" width="min(740px, calc(100vw - 32px))" class="update-rules-dialog">
      <div class="rules-content">
        <h3>触发条件</h3>
        <dl><template v-for="condition in conditions" :key="condition.key"><dt>{{ condition.label }}</dt><dd>{{ condition.rule }}</dd></template></dl>
        <h3>更新路径</h3>
        <p><b>背景监测：</b>政策、市场和行业指标保持稳定，保留监测展示，不参与本页自动更新触发。</p>
        <p><b>增量更新：</b>AUC 下降达到 {{ updateThresholds.auc.incremental.toFixed(2) }} 或 PSI 达到 {{ updateThresholds.psi.incremental.toFixed(2) }}，且均未达到重训阈值时，接入新增数据，调整局部参数与指标权重，经快速验证后生成增量版本。</p>
        <p><b>全量更新：</b>AUC 下降达到 {{ updateThresholds.auc.full.toFixed(2) }} 或 PSI 达到 {{ updateThresholds.psi.full.toFixed(2) }} 时，重新整理历史数据、训练模型、计算指标权重，经完整验证后生成全量版本。</p>
        <p><b>判断优先级：</b>全量更新条件优先于增量更新；均未命中时保持原版本，继续监测。</p>
        <p><b>闭环反馈：</b>更新完成后依次进入版本发布、评分服务更新和企业评分同步，将指标权重、企业评分及模型效果的变化反馈至监测环节。</p>
      </div>
      <template #footer><el-button type="primary" @click="rulesVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Back, Check, CircleCheck, CircleCheckFilled, Clock, Connection, Cpu, DataAnalysis, Document,
  Files, Histogram, Monitor, OfficeBuilding, Plus, Refresh, RefreshLeft, Remove, Right, SetUp,
  TrendCharts, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import { feedbackSteps, publishSteps, updateIndustries, updatePaths, updateScenarios, updateThresholds, useDynamicUpdateDemo } from '../composables/useDynamicUpdateDemo'

// 页面只消费本地流程状态，不引入业务 API。现有机构模型、企业评分及数据库均不受影响。
// 图形由可访问的 DOM 节点构成，支持键盘操作和窄屏重排，不依赖静态截图。
const { industry, scenario, speed, status, events, observation, conditions, sources, selectedPath, pathLabel,
  versionLabel, runVersion, candidateVersion, decisionText, decisionReady, currentStep, progress, completedCount,
  timeline, statusLabel, nodeState, play, pause, reset } = useDynamicUpdateDemo()
const rulesVisible = ref(false)
const resultVisible = ref(false)
const completedResult = ref(null)

// 仅在完整流程进入完成状态时弹出一次。保存本轮结果快照，确保关闭再查看时
// 展示的是本轮检测和最终版本；开始新一轮或切换情景后清空，避免旧结果串入。
// 使用默认批量侦听，在 composable 完成版本更新后再读取结果，不触发业务接口。
watch(status, value => {
  if (value !== 'complete') {
    resultVisible.value = false
    completedResult.value = null
    return
  }
  const path = selectedPath.value
  completedResult.value = {
    industry: industry.value,
    path,
    title: path === 'stable' ? '检测完成，无需更新' : `${pathLabel.value}已完成`,
    summary: {
      stable: '本轮模型表现与数据漂移均未达到更新阈值，前三项背景信号保持稳定，当前版本无需更新。',
      incremental: '模型表现回落与数据漂移达到更新阈值，尚未达到重训阈值。已完成局部参数与指标权重更新，并通过快速验证。',
      full: '模型表现回落与数据漂移达到重训阈值。已重新整理历史数据、训练模型并重算指标权重，完成全量验证。'
    }[path],
    hits: conditions.value.filter(item => item.hit).map(item => ({ key: item.key, label: item.label, current: item.current })),
    beforeVersion: runVersion.value,
    afterVersion: versionLabel.value
  }
  rulesVisible.value = false
  resultVisible.value = true
})
const sourceIcons = [Document, TrendCharts, Files, DataAnalysis]
const publishIcons = [Files, Cpu, OfficeBuilding]
const feedbackIcons = [SetUp, OfficeBuilding, Histogram]

function stateLabel(state) {
  return { waiting: '待执行', active: status.value === 'paused' ? '已暂停' : '进行中', done: '已完成', skipped: '未触发' }[state]
}
function branchStatus(path) {
  if (!decisionReady.value) return '待判断'
  if (selectedPath.value !== path.key) return '未触发'
  if (nodeState(path.steps.at(-1).key) === 'done') return '已完成'
  return status.value === 'paused' ? '已暂停' : '执行中'
}
</script>

<style scoped>
/* 样式限定在本页：沿用机构端的青绿主题，蓝色区分全量分支，琥珀色标记条件命中。 */
.dynamic-update { padding: 24px 28px 32px; color: #29453f; min-width: 0; letter-spacing: 0; font-size: 14px; }
.dynamic-update h1, .dynamic-update h2, .dynamic-update h3, .dynamic-update h4, .dynamic-update p { margin: 0; }
.update-heading { display: flex; justify-content: space-between; gap: 16px; align-items: center; margin-bottom: 20px; }
.update-heading h1 { font-size: 24px; }
.update-heading p { color: #637d72; font-size: 14px; margin-top: 7px; }
.update-heading-actions { display: flex; align-items: center; gap: 18px; }
.run-status { font-size: 14px; display: inline-flex; align-items: center; gap: 7px; white-space: nowrap; color: #637d72; }
.run-status i, .flow-legend i { width: 7px; height: 7px; display: inline-block; border-radius: 50%; background: #b8c5c1; }
.run-status.running { color: #177b78; }
.run-status.running i { background: #177b78; animation: monitor-pulse 1.8s ease-in-out infinite; }
.run-status.paused { color: #a27730; }
.run-status.paused i { background: #bb934f; }
.run-status.complete i { background: #177b78; }
.update-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 18px; padding: 16px 0; border-top: 1px solid #dfe7e5; }
.update-select, .update-scenarios, .update-playback { display: flex; align-items: center; gap: 10px; }
.update-select label, .update-scenarios > span { font-size: 14px; color: #526e60; white-space: nowrap; }
.update-select .el-select { width: 158px; }
.update-playback { margin-left: auto; gap: 8px; }
.update-playback .el-button + .el-button { margin: 0; }
.update-playback .speed-select { width: 102px; }
.update-playback > .el-button:last-child { min-width: 110px; }
.update-scenarios :deep(.el-radio-button__inner) { padding: 10px 13px; font-size: 14px; }
.update-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); background: #fff; margin: 0; border-bottom: 1px solid #e3ebe8; padding: 17px 0; }
.update-summary > div { padding: 0 22px; border-right: 1px solid #e4ece9; }
.update-summary > div:last-child { border: 0; }
.update-summary dt { display: flex; align-items: center; justify-content: space-between; color: #637d72; font-size: 14px; }
.update-summary dt .el-icon { color: #97ada6; font-size: 15px; }
.update-summary dd { margin: 10px 0 7px; font-size: 26px; font-weight: 600; line-height: 1.2; color: #177b78; font-variant-numeric: tabular-nums; }
.update-summary dd small { font-weight: 400; margin-left: 7px; font-size: 13px; color: #637d72; }
.update-summary .summary-strategy, .update-summary .summary-version { font-size: 21px; line-height: 1.5; }
.update-summary > div:nth-child(2) dd { color: #a67934; }
.update-summary > div:nth-child(4) dd { color: #477da1; }
.update-summary > div > span { font-size: 13px; line-height: 1.65; color: #6c8377; }
.update-flow { background: white; margin-top: 16px; padding: 18px 22px 0; }
.flow-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 12px; }
.flow-heading h2, .activity-heading h2 { font-size: 18px; font-weight: 600; }
.flow-legend { display: flex; gap: 16px; font-size: 13px; color: #637d72; }
.flow-legend span { display: flex; align-items: center; gap: 6px; }
.flow-legend i.active { background: #177b78; }
.flow-legend i.done { background: #a4cfc0; }
.flow-columns { display: grid; grid-template-columns: minmax(200px, .95fr) minmax(225px, 1fr) minmax(330px, 1.65fr); gap: 32px; }
.flow-columns > section { min-width: 0; position: relative; }
.step-heading { display: flex; align-items: center; gap: 9px; margin-bottom: 15px; min-height: 24px; flex-wrap: wrap; }
.step-heading > b { font-size: 12px; color: #567c69; background: #eef5f2; border-radius: 4px; height: 25px; width: 28px; display: grid; place-items: center; font-variant-numeric: tabular-nums; }
.step-heading h3 { font-size: 16px; font-weight: 600; }
.heading-note { margin-left: auto; font-size: 12px; color: #6c8377; font-variant-numeric: tabular-nums; }
.source-list { display: grid; gap: 10px; }
.source-item { position: relative; display: flex; align-items: center; gap: 10px; min-height: 82px; border: 1px solid #e2eae7; border-radius: 6px; padding: 12px 10px; overflow: hidden; }
.source-icon { display: grid; place-items: center; height: 31px; width: 31px; flex-shrink: 0; border-radius: 6px; background: #edf6f2; color: #25836e; font-size: 19px; }
.source-item.blue .source-icon { background: #edf3f9; color: #5284ad; }
.source-item.amber .source-icon { background: #faf3e6; color: #b58d45; }
.source-item.violet .source-icon { background: #f1eff7; color: #8a7ba8; }
.source-copy { flex: 1; min-width: 0; }
.source-copy h4 { font-size: 14px; line-height: 1.6; font-weight: 500; }
.source-copy p { font-size: 13px; line-height: 1.6; color: #637d72; margin-top: 3px; }
.source-reading { text-align: right; flex-shrink: 0; }
.source-reading strong { display: block; font-size: 15px; font-weight: 600; font-variant-numeric: tabular-nums; }
.source-reading span { display: block; font-size: 12px; color: #6c8377; margin-top: 5px; }
.source-item.active { border-color: #60aaa0; background: #f5fbf8; }
.source-item.active .source-reading span { color: #177b78; }
.source-item.done { border-color: #cae2d8; }
.source-scan { height: 2px; position: absolute; bottom: 0; left: 0; width: 100%; background: #49a693; animation: scan-pass 1.4s linear infinite; transform-origin: left; }
.monitor-footer { margin-top: 13px; display: flex; align-items: center; gap: 6px; font-size: 13px; color: #637d72; }
.monitor-pips { display: flex; gap: 4px; margin-left: auto; }
.monitor-pips i { width: 4px; height: 8px; background: #78b6a3; animation: monitor-pulse 1.8s ease-in-out infinite; }
.monitor-pips i:nth-child(2) { animation-delay: .3s; }
.monitor-pips i:nth-child(3) { animation-delay: .6s; }
/* 栏间箭头是布局的一部分；窄屏时改为向下衔接，避免横向滚动。 */
.source-column::after, .decision-column::after { content: ''; position: absolute; top: 50%; right: -25px; width: 17px; border-top: 1px solid #96b9ac; }
.source-column::before, .decision-column::before { content: ''; position: absolute; top: calc(50% - 3px); right: -26px; width: 6px; height: 6px; border-right: 1px solid #96b9ac; border-top: 1px solid #96b9ac; transform: rotate(45deg); }
.condition-list { list-style: none; padding: 0; margin: 0; }
.condition-list li { display: flex; align-items: center; gap: 7px; padding: 10px 0; border-bottom: 1px solid #edf1ef; min-height: 61px; }
.condition-list .el-icon { color: #bdc9c4; font-size: 14px; flex-shrink: 0; }
.condition-list strong { display: block; font-size: 14px; font-weight: 500; }
.condition-list span { display: block; font-size: 13px; color: #637d72; margin-top: 5px; line-height: 1.5; }
.condition-list b { margin-left: auto; font-size: 14px; font-variant-numeric: tabular-nums; color: #637d72; font-weight: 500; white-space: nowrap; }
.condition-list .hit .el-icon { color: #b08a41; }
.condition-list .hit b { color: #a67b32; }
.decision-result { display: flex; align-items: center; gap: 10px; min-height: 67px; padding: 10px; margin-top: 13px; background: #f5f8f6; border-left: 3px solid #c9d8d0; }
.decision-result > .el-icon { font-size: 22px; flex-shrink: 0; color: #7fa798; }
.decision-result strong { display: block; font-size: 15px; }
.decision-result span { display: block; font-size: 13px; line-height: 1.75; color: #5d7c69; margin-top: 5px; }
.decision-result.done, .decision-result.active { background: #eaf6ef; border-left-color: #348d72; }
.decision-result.no-update { background: #f1f5f7; border-left-color: #6c96ad; }
.update-branch { --branch-accent: #288a75; --branch-soft: #eaf6f1; border: 1px solid #d6e7df; padding: 16px 14px; border-radius: 6px; min-height: 176px; }
.update-branch.full { --branch-accent: #5183a6; --branch-soft: #edf3f8; border-color: #dbe5ec; margin-top: 12px; }
.branch-heading, .branch-heading > div { display: flex; align-items: center; gap: 7px; }
.branch-heading { justify-content: space-between; color: var(--branch-accent); }
.branch-heading h4 { font-size: 16px; }
.branch-badge { font-size: 12px; background: var(--branch-soft); padding: 3px 7px; border-radius: 4px; }
.update-branch > p { font-size: 13px; line-height: 1.6; color: #637d72; margin-top: 9px; }
.branch-steps { display: grid; grid-template-columns: repeat(var(--step-count), minmax(0, 1fr)); margin: 14px 0 0; padding: 0; list-style: none; }
.branch-steps li { min-width: 0; text-align: center; position: relative; }
.node-circle { position: relative; display: grid; place-items: center; width: 30px; height: 30px; margin: 0 auto 9px; border: 1px solid #cadbd1; background: #fff; border-radius: 50%; color: #6c8377; font-size: 12px; z-index: 1; }
.branch-steps strong { display: block; max-width: 100px; padding: 0 4px; margin: auto; font-size: 14px; font-weight: 500; line-height: 1.7; color: #526e60; text-wrap: balance; }
.branch-steps li:not(:last-child)::after { content: ''; position: absolute; top: 15px; left: calc(50% + 15px); width: calc(100% - 30px); height: 1px; background: #dfe7e2; }
.branch-steps .done:not(:last-child)::after { background: var(--branch-accent); }
.branch-steps .active .node-circle { color: white; background: var(--branch-accent); border-color: var(--branch-accent); }
.branch-steps .active .node-circle::after { content: ''; position: absolute; inset: -4px; border: 1px solid var(--branch-accent); border-radius: 50%; animation: node-pulse 1.8s ease-out infinite; }
.branch-steps .done .node-circle { background: var(--branch-soft); color: var(--branch-accent); border-color: var(--branch-accent); }
.branch-steps .active strong, .branch-steps .done strong { color: var(--branch-accent); }
.update-branch.chosen { background: #f7fcf9; border-color: var(--branch-accent); }
.update-branch.full.chosen { background: #f7fafc; }
.update-branch.bypassed { background: #f8faf9; border-color: #e1e8e4; }
.stable-path { display: flex; align-items: center; justify-content: center; gap: 7px; font-size: 13px; padding: 12px 3px 0; color: #6c8377; flex-wrap: wrap; }
.stable-path strong { font-weight: 500; }
.stable-path.selected { color: #467f9d; }
.delivery-row { display: grid; grid-template-columns: 1.55fr 1fr; gap: 28px; border-top: 1px solid #e6ede9; margin-top: 22px; padding: 18px 0; }
.publish-steps { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); margin: 0; padding: 0; list-style: none; gap: 20px; }
.publish-steps li { position: relative; display: flex; align-items: center; gap: 9px; min-height: 51px; }
.publish-steps li > .el-icon { font-size: 23px; color: #a5b9af; flex-shrink: 0; }
.publish-steps strong { font-size: 14px; font-weight: 500; display: block; line-height: 1.6; color: #526e60; }
.publish-steps span { font-size: 12px; color: #6c8377; display: block; margin-top: 5px; }
.publish-steps li:not(:last-child)::after { content: ''; position: absolute; right: -12px; width: 5px; height: 5px; border-top: 1px solid #bdcfc4; border-right: 1px solid #bdcfc4; transform: rotate(45deg); }
.publish-steps .active > .el-icon { color: #177b78; animation: monitor-pulse 1.8s ease-in-out infinite; }
.publish-steps .done > .el-icon, .publish-steps .done strong { color: #298775; }
.feedback-column { border-left: 1px solid #e6ede9; padding-left: 24px; }
.feedback-items { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.feedback-items > div { display: grid; grid-template-columns: 20px 1fr; gap: 5px; align-items: center; padding-top: 7px; }
.feedback-items .el-icon { font-size: 18px; color: #9db3a7; }
.feedback-items strong { font-size: 14px; line-height: 1.6; font-weight: 500; color: #526e60; }
.feedback-items span { grid-column: 2; font-size: 12px; color: #6c8377; }
.feedback-items .active .el-icon { color: #177b78; animation: monitor-pulse 1.8s ease-in-out infinite; }
.feedback-items .done .el-icon, .feedback-items .done strong { color: #298775; }
.loop-track { display: flex; align-items: center; gap: 10px; padding: 14px 0; color: #637d72; border-top: 1px solid #e6ede9; font-size: 14px; }
.loop-track > .el-icon:first-child { font-size: 18px; }
.loop-track > span { white-space: nowrap; }
.loop-track small { font-size: 12px; }
.loop-line { position: relative; height: 1px; background: #d9e8df; flex: 1; min-width: 20px; overflow: hidden; }
.loop-line i { display: block; width: 100%; height: 1px; background: #499b7e; transform: translateX(100%); }
.loop-track.active .loop-line i { animation: loop-return 1.4s linear infinite; }
.loop-track.done { color: #28856e; }
.loop-track.done .loop-line { background: #81b7a0; }
.update-activity { display: grid; grid-template-columns: 1fr 1.6fr; background: #fff; margin-top: 16px; }
.task-panel, .event-panel { padding: 18px 22px; min-width: 0; }
.event-panel { border-left: 1px solid #e6ede9; }
.activity-heading { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 10px; align-items: center; }
.activity-heading > span { color: #6c8377; font-size: 13px; }
.task-progress { display: flex; gap: 15px; align-items: center; margin: 19px 0 13px; }
.task-progress > strong { font-size: 28px; line-height: 1; color: #267f6d; font-variant-numeric: tabular-nums; min-width: 72px; }
.task-progress small { font-size: 13px; margin-left: 3px; font-weight: 500; }
.task-progress > span { font-size: 14px; line-height: 1.6; color: #526e60; }
.task-progress-track { height: 5px; background: #ebf3ee; border-radius: 3px; overflow: hidden; }
.task-progress-track i { display: block; height: 100%; background: #35977e; transform-origin: left; transition: transform .1s linear; }
.task-foot { display: flex; justify-content: space-between; gap: 8px; font-size: 13px; color: #6c8377; margin-top: 13px; }
.task-foot b { color: #587c69; font-weight: 500; }
.event-list { padding: 0; margin: 15px 0 0; list-style: none; min-height: 104px; }
.event-list li { display: grid; grid-template-columns: 68px 5px minmax(110px, .7fr) minmax(0, 1.3fr); gap: 10px; align-items: baseline; font-size: 13px; line-height: 1.7; margin-bottom: 10px; }
.event-list time { color: #6c8377; font-variant-numeric: tabular-nums; }
.event-list i { width: 5px; height: 5px; border-radius: 50%; background: #61a88a; align-self: center; }
.event-list i.info { background: #7d9db5; }
.event-list strong { font-weight: 500; color: #557b65; }
.event-list li > span { color: #6c8377; overflow-wrap: anywhere; }
.events-empty { min-height: 117px; display: flex; align-items: center; justify-content: center; gap: 9px; color: #6c8377; font-size: 14px; }
.events-empty .el-icon { font-size: 22px; }
.update-event-enter-active { transition: transform .25s ease, opacity .25s ease; }
.update-event-enter-from { transform: translateY(-6px); opacity: 0; }
.rules-content h3 { font-size: 16px; color: #31564b; margin: 0 0 12px; }
.rules-content dl { display: grid; grid-template-columns: 1fr 1fr; margin: 0 0 24px; font-size: 14px; }
.rules-content dt, .rules-content dd { padding: 9px 0; margin: 0; border-bottom: 1px solid #edf1ee; }
.rules-content dd { color: #637d72; }
.rules-content p { font-size: 14px; line-height: 1.9; color: #526e60; margin-bottom: 12px; }
/* 结果弹窗独立滚动，窄屏和较矮窗口中仍能保留标题及关闭按钮。 */
.completion-content { max-height: calc(100dvh - 190px); overflow-y: auto; padding-right: 4px; font-size: 14px; color: #526e60; }
.completion-heading { display: flex; gap: 12px; align-items: center; margin-bottom: 18px; }
.completion-heading > .el-icon { font-size: 36px; color: #268674; flex-shrink: 0; }
.completion-heading h3 { font-size: 19px; color: #284d40; margin: 0; line-height: 1.6; }
.completion-content .completion-heading p { margin: 5px 0 0; color: #6c8377; font-size: 14px; }
.completion-content .completion-summary { margin: 0 0 20px; line-height: 1.85; }
.completion-content h4 { font-size: 14px; color: #31564b; margin: 0 0 8px; }
.completion-conditions { margin: 0 0 20px; }
.completion-conditions > div { display: flex; gap: 12px; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #e7eeea; }
.completion-conditions dd { margin: 0; font-weight: 600; color: #906c2c; font-variant-numeric: tabular-nums; white-space: nowrap; }
.completion-stable { display: flex; gap: 8px; line-height: 1.85; margin-bottom: 18px; }
.completion-stable .el-icon { color: #268674; flex-shrink: 0; margin-top: 6px; }
.completion-version { display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap; padding-top: 4px; }
.completion-version strong { color: #287e6c; font-variant-numeric: tabular-nums; }
.completion-content .completion-feedback { font-size: 13px; color: #6c8377; line-height: 1.85; margin: 14px 0 0; }
/* 动效仅使用 transform/opacity；暂停及系统减少动画设置均能停止非必要动效。 */
@keyframes monitor-pulse { 0%, 100% { opacity: 1; } 50% { opacity: .35; } }
@keyframes scan-pass { from { transform: scaleX(0); } to { transform: scaleX(1); } }
@keyframes node-pulse { from { transform: scale(.85); opacity: .8; } to { transform: scale(1.25); opacity: 0; } }
@keyframes loop-return { from { transform: translateX(100%); } to { transform: translateX(-100%); } }
.motion-paused *, .motion-paused *::after { animation-play-state: paused !important; }
@media (prefers-reduced-motion: reduce) {
  .dynamic-update *, .dynamic-update *::after { animation: none !important; transition: none !important; }
}
@media (min-width: 1700px) {
  .flow-columns { grid-template-columns: 1fr 1fr 1.7fr; gap: 44px; }
  .source-item { padding: 12px; }
  .update-branch { padding: 18px; }
  .branch-steps strong { max-width: 95px; }
  .branch-steps { margin-top: 18px; }
  .source-column::after, .decision-column::after { right: -34px; width: 24px; }
  .source-column::before, .decision-column::before { right: -35px; }
}
@media (max-width: 1250px) {
  .dynamic-update { padding: 20px; }
  .update-flow { padding: 18px 18px 0; }
  .flow-columns { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; }
  .branch-column { grid-column: 1 / -1; display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
  .branch-column .step-heading { grid-column: 1 / -1; margin: 0; }
  .branch-column .update-branch { margin: 0; }
  .branch-column .stable-path { grid-column: 1 / -1; padding: 0; }
  .source-column::before { right: -20px; }
  .source-column::after { right: -19px; width: 13px; }
  .decision-column::after, .decision-column::before { display: none; }
  .delivery-row { grid-template-columns: 1fr; gap: 18px; }
  .feedback-column { padding: 16px 0 0; border-left: 0; border-top: 1px solid #e6ede9; }
  .feedback-items { max-width: 620px; }
  .update-summary > div { padding: 0 16px; }
  .update-toolbar { column-gap: 20px; row-gap: 12px; }
}
@media (max-width: 700px) {
  .dynamic-update { padding: 16px 12px; }
  .update-heading { align-items: flex-start; }
  .update-heading-actions { flex-direction: column; gap: 8px; align-items: flex-end; }
  .update-toolbar { align-items: stretch; }
  .update-select, .update-scenarios { width: 100%; justify-content: space-between; flex-wrap: wrap; }
  .update-select .el-select { width: 222px; max-width: 100%; }
  .update-scenarios :deep(.el-radio-button__inner) { padding: 10px 11px; }
  .update-playback { width: 100%; justify-content: flex-end; }
  .update-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); row-gap: 20px; }
  .update-summary > div:nth-child(2) { border: 0; }
  .flow-columns { grid-template-columns: minmax(0, 1fr); gap: 28px; }
  .flow-columns > section::before, .flow-columns > section::after { display: none; }
  .branch-column { grid-template-columns: minmax(0, 1fr); }
  .flow-heading { flex-wrap: wrap; margin-bottom: 20px; }
  .flow-legend { gap: 10px; }
  .publish-steps { grid-template-columns: 1fr; gap: 12px; }
  .publish-steps li { min-height: 42px; }
  .publish-steps li:not(:last-child)::after { right: auto; left: 9px; bottom: -7px; transform: rotate(135deg); }
  .loop-track { flex-wrap: wrap; }
  .loop-track small { flex-basis: 80%; margin-left: 28px; }
  .update-activity { grid-template-columns: minmax(0, 1fr); }
  .event-panel { border-left: 0; border-top: 1px solid #e6ede9; }
  .event-list li { grid-template-columns: 68px 5px 1fr; gap: 8px; margin-bottom: 12px; }
  .event-list li > span { grid-column: 3; margin-top: -5px; }
  .rules-content dl { gap: 0 12px; }
}
</style>
