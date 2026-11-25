const { spawn } = require('child_process')
const fs = require('fs')
const path = require('path')

const rootDir = path.resolve(__dirname, '..')
const frontendDir = path.join(rootDir, 'frontend')
const logPath = path.join(frontendDir, 'frontend-dev.log')
const errPath = path.join(frontendDir, 'frontend-dev.err')

const out = fs.openSync(logPath, 'a')
const err = fs.openSync(errPath, 'a')

const child = spawn(
  process.execPath,
  ['node_modules/vite/bin/vite.js', '--host', '0.0.0.0', '--strictPort'],
  {
    cwd: frontendDir,
    detached: true,
    stdio: ['ignore', out, err],
    windowsHide: true
  }
)

child.unref()
console.log(`Frontend dev server started, pid=${child.pid}`)
