# exercises-dataset

**健身练习数据集** — 1,324 个练习，含多语言说明、动画 GIF 和缩略图。

## 技术栈

- **数据格式**: JSON (1,324 条记录) + JSON Schema (Draft 2020-12)
- **应用**: 纯 HTML5 + CSS3 + 原生 JavaScript (ES2020+)，无框架
- **媒体**: 1,324 × 180×180 JPEG 缩略图 + 1,324 × 180×180 动画 GIF
- **依赖**: 零。无 package.json、无构建步骤、无需服务器。

## 目录结构

```
exercises-dataset/
├── data/
│   ├── exercises.json          # 1,324 条练习记录（主数据集）
│   └── exercises.schema.json   # JSON Schema 定义
├── images/                     # 1,324 张缩略图 (180×180 JPG)
├── videos/                     # 1,324 个动画 GIF (180×180)
├── index.html                  # 练习浏览器 — 搜索、筛选、弹窗
├── setup.html                  # 开发者设置指南（SQL、API、LLM 提示）
├── README.md                   # 英文文档
├── README_zh.md                # 中文文档
├── LICENSE                     # MIT + 媒体例外条款
├── NOTICE.md                   # 媒体归属 (© Gym visual)
└── .gitignore
```

## 构建 / 测试 / CI

- **无构建系统、无测试、无 CI 管道。**
- 直接在浏览器中打开 `index.html` 或 `setup.html` 即可运行。
- 验证 `exercises.json`：用 JSON Schema 校验器（如 `ajv`）。
- JS 语法检查：提取 `<script>` 内容后执行 `node --check <file>`。

## 编码规范

### 数据 (JSON)
- **字段命名**: `snake_case`（`body_part`, `muscle_group`, `instruction_steps`）
- **ID**: 4 位零填充字符串（`"0001"`）
- **语言键**: ISO 639-1（`en`, `es`, `it`, `tr`, `ru`, `zh`, `hi`, `pl`, `ko`, `fr`）
- **日期**: ISO 8601 含 UTC 偏移
- `body_part` 是枚举类型：back, cardio, chest, lower arms, lower legs, neck, shoulders, upper arms, upper legs, waist
- 媒体路径前缀 `images/` / `videos/`（相对路径）
- `name_zh`：可选的中文练习名称字段

### HTML / CSS
- HTML5 `<!DOCTYPE html>`，`<html lang="en">`（通过 JS 切换至 `zh-CN`）
- 所有样式 **内联** 在 `<style>` 块中
- 类命名：kebab-case + BEM 风格（`search-wrapper`, `exercise-card`, `modal-overlay`）
- 设计令牌：`:root` 中的 CSS 自定义属性（`--accent`, `--bg-base`, `--radius-md` 等）
- 无预处理器、无外部 CSS 文件

### JavaScript
- **现代 ES2020+**: `const`/`let`、箭头函数、模板字面量、解构、`IntersectionObserver`
- **变量/函数**: `camelCase`（`buildInserts`, `copyToClipboard`）
- **全局常量**: `SCREAMING_SNAKE_CASE`（`EXERCISES`, `DB_SQL`, `META_ZH`, `I18N`）
- **DOM 查询**: `document.getElementById()` / `querySelectorAll()`，无框架
- **事件绑定**: `addEventListener` 在脚本末尾绑定（script 位于 `</body>` 前）
- **i18n 模式**:
  - `I18N` 字典含 `en`/`zh` 键；`t(key, ...args)` 查找函数
  - `data-i18n` / `data-i18n-placeholder` / `data-i18n-title` 属性声明式绑定
  - `META_ZH` 映射英文元数据值为中文；`tm(value)` 渲染时翻译
  - 语言选择持久化在 `localStorage.getItem('lang')`，默认 `'en'`
- **注释风格**: 分节注释用 `──` 装饰的框状注释

## 关键架构决策

- `index.html` 内联完整 1,324 条数据集为 `const EXERCISES = [...]` — 运行时无网络请求
- `setup.html` 动态加载 `data/exercises.json`（懒加载）
- 每条练习数据有 **14 个必需字段** + 可选 `name_zh`
- Schema 设置 `additionalProperties: false` — 新增字段需要显式更新 Schema
- 同一数据集存在于两处：`data/exercises.json` 和内联在 `index.html` 中 — 两者必须保持同步

## 编辑流程

1. 修改数据：编辑 `data/exercises.json`
2. 如果数据变更需要同步到内联副本，重建 `index.html` 中的 `const EXERCISES`
3. 修改 UI：编辑 `index.html` / `setup.html`
4. 验证 JSON：用 `json.load()` 或 Schema 校验器解析
5. 验证 JS 语法：提取 `<script>` 内容后执行 `node --check`
6. 在浏览器中打开确认行为正常

## i18n / 本地化（已添加功能）

- 侧边栏头部语言切换按钮（`中`/`EN`），选择存储在 `localStorage`
- UI 文本：`index.html` 和 `setup.html` 中的 `I18N` 字典
- 元数据翻译（category, body_part, equipment, target）：`META_ZH` + `tm()` 函数
- 练习名称：中文模式下显示 `name_zh` 字段
- 搜索索引通过 `tm()` + `name_zh` 同时支持中英文搜索

## 许可证

- 代码、数据结构、指令文本：MIT（© Hasan Emir Yildirim）
- 媒体（图片和 GIF）：© Gym visual — 仅限 180×180 分辨率，详见 `NOTICE.md`

## 回复偏好

- 用中文回复
- 代码注释用中文
