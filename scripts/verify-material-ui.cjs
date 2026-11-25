// 独立浏览器与模拟接口验收，不使用真实企业会话、不写业务数据、不调用在线AI。
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const assert = require('node:assert/strict')
const fs = require('node:fs')
;(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_EXECUTABLE || undefined })
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
    const errors = []; page.on('pageerror', e => errors.push(e.message))
    await page.addInitScript(() => localStorage.setItem('companyUser', JSON.stringify({ companyCode: 'demo', companyName: '示例钢铁企业' })))
    const candidate = { field: 'steelCarbonEmissionIntensity', label: '吨钢碳排放强度', rawValue: '2.00', value: 2,
      unit: '吨二氧化碳/吨粗钢', year: 2024, denominator: '粗钢产量', boundary: '四基地', reportCompany: '示例钢铁企业',
      page: 40, quote: '2024年四基地碳排放强度为2.00吨二氧化碳/吨粗钢。', status: 'REVIEW', reasons: ['请核对核算边界'], evidenceVerified: true }
    const task = { id: 'demo', fileName: '钢铁企业可持续发展报告.pdf', dataYear: 2024, status: 'COMPLETED', progress: 100,
      message: '提取完成，请核对原文后采用', createdAt: '2026-11-09 10:00:00', result: { pageCount: 90, ocrPages: [1], searchedPages: [30, 40], templateVersion: 'material-facts-v2', diagnostic: '已保存AI结构化响应', extractedFacts: [{name:'吨钢碳排放强度',rawValue:'2.00',page:40}], aiResponses: [], candidates: [candidate, {...candidate,field:'',label:'员工人数',status:'OUTSIDE'}, {...candidate,field:'rAndDIntensity',label:'研发投入强度',status:'MISSING',rawValue:''}] } }
    let writes = 0
    await page.route('**/api/company-materials**', async route => {
      const path = new URL(route.request().url()).pathname
      if (path.endsWith('/file')) return route.fulfill({ status: 503, body: 'fixture does not provide PDF' })
      let data
      if (path.endsWith('/template')) data = { companyName: '示例钢铁企业', aiConfigured: true, indicators: [{ ...candidate, numerator: '碳排放量', source: '演示口径v1' }] }
      else if (path.endsWith('/targets')) data = { rows: [{ id: 1, institution_name: '银行A', data_year: 2024, steel_carbon_emission_intensity: 3 }], adoptedRows: writes ? [1] : [] }
      else if (path.endsWith('/adoptions')) { const body = route.request().postDataJSON(); assert.equal(body.items[0].expectedOldValue, 3); assert.equal(body.items[0].value, 2); writes++; data = null }
      else data = path.endsWith('/demo') ? task : [task]
      await route.fulfill({ json: { code: 200, data }, headers: { 'Access-Control-Allow-Origin': 'http://localhost:5174', 'Access-Control-Allow-Credentials': 'true' } })
    })
    await page.goto('http://localhost:5174/company/dashboard/materials')
    await page.getByRole('button', { name: '查看结果' }).click()
    await page.getByText('员工人数', {exact:true}).waitFor()
    await page.getByText('模板待补项', {exact:true}).click()
    await page.getByText('研发投入强度', {exact:true}).waitFor()
    assert.equal(await page.getByText('员工人数', {exact:true}).count(),0)
    await page.getByText('已提取指标', {exact:true}).click()
    await page.getByRole('button', { name: '提取诊断' }).click()
    await page.getByText('已保存AI结构化响应', {exact:true}).waitFor()
    await page.keyboard.press('Escape')
    await page.getByRole('button', { name: '查看依据' }).first().click()
    await page.getByRole('dialog').waitFor()
    assert.ok(await page.getByText('请核对核算边界', { exact: true }).isVisible())
    await page.keyboard.press('Escape')
    await page.locator('.materials .el-table').nth(1).locator('.el-checkbox').nth(1).click()
    await page.getByRole('button', { name: '写入待评估数据（可选）' }).click()
    await page.locator('.adoption-bar .el-select').click()
    await page.getByText('银行A · 2024年 · ID 1', { exact: true }).click()
    await page.getByRole('button', { name: '确认采用（1项）' }).click()
    assert.ok(await page.getByRole('button', { name: '确认写入', exact: true }).isDisabled())
    await page.getByPlaceholder('填写复核说明：主体、核算范围、年份和分母口径如何确认').fill('已核对报告主体、2024年及粗钢产量分母')
    await page.getByText('已核对报告主体、期间及核算范围属于本企业本条记录', { exact: true }).click()
    await page.getByRole('button', { name: '确认写入', exact: true }).click()
    await page.getByText('已写入待评估记录，旧评分已清空', { exact: true }).waitFor()
    assert.equal(writes, 1)
    await page.getByRole('dialog').waitFor({ state: 'hidden' })
    await page.waitForTimeout(400)
    fs.mkdirSync('output/material-ui', { recursive: true })
    await page.screenshot({ path: 'output/material-ui/desktop.png', fullPage: true })
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: 'output/material-ui/mobile.png', fullPage: true })
    assert.deepEqual(errors, [])
    console.log('PASS: results, evidence dialog, explicit adoption, old value, one write, no browser errors')
  } finally { await browser.close() }
})().catch(e => { console.error(e.message); process.exitCode = 1 })
