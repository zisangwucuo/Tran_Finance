// 模拟API验收页面与导出，图中数据仅为UI夹具，不作为实验结论。
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
;(async () => {
  const browser = await chromium.launch({ executablePath: process.env.PLAYWRIGHT_EXECUTABLE, headless: true })
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
    const errors = []; page.on('pageerror', e => errors.push(e.message))
    await page.addInitScript(() => localStorage.setItem('financialUser', JSON.stringify({ institutionId: 'test', institutionName: '测试银行' })))
    const results = ['逻辑回归', '单棵决策树', '梯度提升树', '随机森林'].map((model, i) => ({
      industryKey: 'power', industryName: '电力行业', model, trainYears: [2019, 2020, 2021], testYear: 2022,
      trainSamples: 192, testSamples: 64, auc: .7 + i * .03, f1: .65, brier: .2,
      parameters: { random_state: 42 }, dataFingerprint: 'test-fixture',
      fixedThreshold: { threshold: .5, predictedPositive: 0, precision: null, recall: 0, f1: 0,
        accuracy: .59375, confusionMatrix: [[38, 0], [26, 0]], precisionReason: '没有预测正例，精确率分母为0' },
      selectedThreshold: { threshold: .35, predictedPositive: 30, precision: .6, recall: .7, f1: .65,
        accuracy: .7, confusionMatrix: [[26, 12], [8, 18]] },
      probabilityRange: { min: .3, max: .4, mean: .35 },
      thresholdSelection: { available: true, validationYears: [2020, 2021], validationSamples: 128, validationF1: .7, rule: '内部验证F1' } }))
    let runs = [], submitted = 0
    await page.route('http://localhost:8080/api/model-comparisons', async route => {
      let data = runs
      if (route.request().method() === 'POST') {
        assert.deepEqual(route.request().postDataJSON(), { industryKey: 'all', mode: 'optimized' })
        submitted++
        runs = [{ id: 'demo', createdAt: new Date().toISOString(), status: 'SUCCESS', progress: 100,
          results, mode: 'tuned', split: '最新年份测试', tuning: '自动调参', seed: 42, threshold: .5, message: '实验完成' }]
        data = { id: 'demo' }
      }
      await route.fulfill({ json: { code: 200, data }, headers: { 'Access-Control-Allow-Origin': 'http://localhost:5174', 'Access-Control-Allow-Credentials': 'true' } })
    })
    await page.goto('http://localhost:5174/dashboard/model-comparison')
    await page.getByText('暂无对比实验', { exact: true }).waitFor()
    await page.getByText('原始指标优化', { exact: true }).click()
    await page.getByRole('button', { name: '开始对比' }).click()
    await page.getByText('逻辑回归', { exact: true }).waitFor()
    await page.locator('.comparison-chart canvas').waitFor()
    await page.getByRole('button', { name: '查看', exact: true }).first().click()
    await page.getByRole('dialog').waitFor()
    await page.getByText('没有预测正例，精确率分母为0', { exact: true }).waitFor()
    assert.equal(await page.locator('.confusion-matrix').count(), 2)
    assert.ok(await page.getByText('38（TN）', { exact: true }).isVisible())
    await page.keyboard.press('Escape')
    const download = page.waitForEvent('download')
    await page.getByRole('button', { name: '导出 CSV' }).click()
    assert.ok((await download).suggestedFilename().endsWith('.csv'))
    fs.mkdirSync('output/comparison-ui', { recursive: true })
    await page.screenshot({ path: 'output/comparison-ui/desktop.png', fullPage: true })
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: 'output/comparison-ui/mobile.png', fullPage: true })
    assert.equal(submitted, 1)
    assert.deepEqual(errors, [])
    console.log('PASS: start, results, chart, details, CSV download, desktop/mobile render')
  } finally { await browser.close() }
})().catch(e => { console.error(e); process.exitCode = 1 })
