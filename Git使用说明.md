# Git 和 GitHub 使用说明

本文档用于本项目的日常版本管理。建议 GitHub 仓库先创建为私有仓库。

## 1. 第一次上传到 GitHub

### 1.1 配置 Git 用户信息

只需要在这台电脑配置一次：

```powershell
git config --global user.name "你的 GitHub 用户名"
git config --global user.email "你的 GitHub 邮箱"
```

### 1.2 在 GitHub 创建仓库

1. 打开 GitHub，点击 New repository。
2. Repository name 建议填写 `trans-finance-platform`。
3. 选择 Private。
4. 不勾选 README、.gitignore、License，因为本地项目已经有文件。
5. 创建完成后复制仓库地址，例如：

```text
https://github.com/你的用户名/trans-finance-platform.git
```

### 1.3 本地首次提交并推送

在项目根目录执行：

```powershell
git status
git add .
git commit -m "Initial project version"
git branch -M main
git remote add origin https://github.com/你的用户名/trans-finance-platform.git
git push -u origin main
```

## 2. 日常开发流程

每次开始修改前，先确认当前状态：

```powershell
git status
```

完成一小块稳定功能后提交：

```powershell
git add .
git commit -m "说明这次改了什么"
git push
```

提交信息建议写清楚业务含义，例如：

```text
Add company missing data notice filters
Fix model training force confirm dialog
Update financial registration form
```

## 3. 推荐分支方式

项目早期可以保持简单：

- `main`：稳定版本，能正常启动和演示。
- `feature/xxx`：新功能开发分支，例如 `feature/company-system`。
- `fix/xxx`：修复问题分支，例如 `fix/model-clear-status`。

新建功能分支：

```powershell
git switch -c feature/功能名称
```

开发完成后合并回 main：

```powershell
git switch main
git merge feature/功能名称
git push
```

## 4. 回退常用命令

查看提交历史：

```powershell
git log --oneline
```

查看某个文件的修改：

```powershell
git diff 路径/文件名
```

撤销还没有提交的某个文件修改：

```powershell
git restore 路径/文件名
```

回到某个历史版本查看代码：

```powershell
git switch --detach 提交ID
```

如果确认要把 main 回退到旧版本，先单独确认后再操作，不要随便使用强制回退命令。

## 5. 不要提交的内容

以下内容已经在 `.gitignore` 中忽略：

- 后端编译产物：`backend/target/`
- 前端依赖和构建产物：`frontend/node_modules/`、`frontend/dist/`
- Python 虚拟环境和模型文件：`ml-service/.venv/`、`ml-service/models/`
- 本地环境变量：`.env`、`.env.*`
- IDEA/VS Code 本地配置：`.idea/`、`.vscode/`
- Codex 临时表格工作目录：`.codex-spreadsheet-work/`

如果以后新增本地配置文件、密钥、证书、真实密码，不要提交到 GitHub。
