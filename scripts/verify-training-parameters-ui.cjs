// 模拟训练接口验证参数弹窗及强行训练重试，不创建真实训练任务。
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
;(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_EXECUTABLE || undefined })
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
    const errors = [], requests = []
    let savedParameters = null
    page.on('pageerror', e => errors.push(e.message))
    await page.addInitScript(() => localStorage.setItem('financialUser', JSON.stringify({ institutionId: 'test', institutionName: '测试银行' })))
    await page.route('http://localhost:8080/api/**', async route => {
      const path = new URL(route.request().url()).pathname
      let body = { code: 200, data: { latestJob: null, models: savedParameters ? [{
        industryKey: 'power', targetName: 'transition_loan', trainedAt: '2026-11-09T15:00:00',
        metricsJson: JSON.stringify({ trainingParameters: savedParameters })
      }] : [] } }
      if (path.endsWith('/update') || path.endsWith('/train-all')) {
        const data = route.request().postDataJSON()
        requests.push({ path, ...data })
        if (path.endsWith('/update') && data.force) savedParameters = data.parameters
        if (path.endsWith('/update') && !data.force) body = { code: 400, message: '训练集存在缺失字段' }
      }
      await route.fulfill({ json: body, headers: { 'Access-Control-Allow-Origin': 'http://localhost:5174', 'Access-Control-Allow-Credentials': 'true' } })
    })
    await page.goto('http://localhost:5174/dashboard/model-training')
    await page.getByRole('button', { name: '更新模型', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: '更新模型参数', exact: true })
    await dialog.waitFor()
    await dialog.getByRole('spinbutton').first().fill('100')
    await dialog.getByRole('button', { name: '重新训练八个行业' }).click()
    await page.getByRole('button', { name: '强行训练', exact: true }).click()
    await dialog.waitFor({ state: 'hidden' })
    assert.equal(requests.length, 2)
    assert.equal(requests[0].parameters.nEstimators, 100)
    assert.deepEqual(requests[0].parameters, requests[1].parameters)
    assert.equal(requests[1].force, true)
    // 模拟离开到联邦页面再返回，以及完整刷新，验证回显来自已保存模型而非组件内存。
    await page.goto('http://localhost:5174/dashboard/federated-learning')
    await page.goto('http://localhost:5174/dashboard/model-training')
    await page.reload()
    await page.getByRole('button', { name: '更新模型', exact: true }).click()
    await dialog.waitFor()
    assert.equal(await dialog.getByRole('spinbutton').first().inputValue(), '100')
    await dialog.getByRole('button', { name: '取消', exact: true }).click()
    await page.getByRole('button', { name: '一键训练模型', exact: true }).click()
    await page.waitForFunction(() => document.querySelectorAll('.el-message--success').length > 0)
    assert.equal(requests[2].parameters, undefined)
    await page.getByRole('button', { name: '更新模型', exact: true }).click()
    await dialog.getByRole('button', { name: '恢复默认' }).click()
    assert.equal(await dialog.getByRole('spinbutton').first().inputValue(), '300')
    await page.setViewportSize({ width: 390, height: 844 })
    const box = await dialog.boundingBox()
    assert.ok(box.x >= 0 && box.x + box.width <= 391)
    await dialog.getByRole('button', { name: '取消', exact: true }).click()
    assert.equal(requests.length, 3)
    assert.deepEqual(errors, [])
    console.log('PASS: custom parameters, force retry snapshot, route/reload restoration, baseline request, reset, cancel, mobile width')
  } finally { await browser.close() }
})().catch(e => { console.error(e); process.exitCode = 1 })
