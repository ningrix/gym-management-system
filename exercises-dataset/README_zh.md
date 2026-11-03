<div align="center">

# 💪 练习数据集 (Exercises Dataset)

<p>
  <img src="videos/0025-EIeI8Vf.gif" width="120" alt="杠铃卧推" />
  <img src="videos/0043-qXTaZnJ.gif" width="120" alt="杠铃全蹲" />
  <img src="videos/0032-ila4NZS.gif" width="120" alt="杠铃硬拉" />
  <img src="videos/0652-lBDjFxJ.gif" width="120" alt="引体向上" />
  <img src="videos/0294-NbVPDMW.gif" width="120" alt="哑铃二头弯举" />
  <img src="videos/0334-DsgkuIt.gif" width="120" alt="哑铃侧平举" />
</p>

**一个全面且可直接使用的健身练习数据集，包含 1,324 个动作——每个动作配有动画 GIF、180×180 缩略图、分类、身体部位、器械、目标和肌群数据，以及 10 种语言（英语、西班牙语、意大利语、土耳其语、俄语、中文、印地语、波兰语、韩语、法语）的分步说明。**

[![练习数](https://img.shields.io/badge/练习-1324-blue?style=flat-square)](data/exercises.json)
[![动画 GIF](https://img.shields.io/badge/动画%20GIF-1324-brightgreen?style=flat-square)](videos/)
[![缩略图](https://img.shields.io/badge/缩略图-1324-orange?style=flat-square)](images/)
[![语言数](https://img.shields.io/badge/语言-10-green?style=flat-square)](#-概述)
[![移动应用](https://img.shields.io/badge/应用-LogPress-111111?style=flat-square&logo=react)](https://github.com/hasaneyldrm/logpress-public)
[![许可证](https://img.shields.io/badge/许可证-MIT%20%2B%20media%20terms-blue?style=flat-square)](LICENSE)

</div>

> **📱 为 [LogPress](https://github.com/hasaneyldrm/logpress-public) 应用提供数据支持**——一款 AI 辅助的健身追踪器；本数据集是其练习数据层。正在开发自己的健身应用？可直接将其接入你的后端。

> **[English README](README.md) · 中文说明**

---

## 📦 数据来源

**本仓库提供：**

- 1,324 个练习，包含分类、身体部位、器械、目标和肌群数据
- 每个练习配有动画 GIF + 180×180 缩略图（媒体版权 © [Gym visual](https://gymvisual.com/)——参见 [许可](#-许可与使用)）
- 10 种语言的分步说明（🇬🇧 英语、🇪🇸 西班牙语、🇮🇹 意大利语、🇹🇷 土耳其语、🇷🇺 俄语、🇨🇳 中文、🇮🇳 印地语、🇵🇱 波兰语、🇰🇷 韩语、🇫🇷 法语）
- 交互式浏览器（`index.html`）和开发者设置指南（`setup.html`）
- 中文练习名称（`name_zh` 字段）和中英文切换界面

---

## 📋 目录

- [数据来源](#-数据来源)
- [概述](#-概述)
- [交互式浏览器与开发者设置](#-交互式浏览器与开发者设置)
- [文件结构](#-文件结构)
- [统计数据](#-统计数据)
- [数据结构](#-数据结构)
- [示例练习](#-示例练习)
- [使用示例](#-使用示例)
- [许可与使用](#-许可与使用)

---

## 🔍 概述

本数据集是一个精心整理的 **1,324 个健身练习**合集，适用于教育和研究目的。涵盖了广泛的肌群、器械类型和练习分类——适用于：

- 构建健身或训练计划应用
- 涉及练习识别或推荐的机器学习项目
- 健康与保健研究
- 教育演示和原型开发

每条练习记录包含：

| 字段 | 说明 |
|---|---|
| 唯一 ID | 数字标识符（如 `"0001"`） |
| 名称 | 完整的描述性练习名称（含中英文） |
| 分类 | 主要目标肌群 |
| 目标肌肉 | 具体目标肌肉 |
| 肌群 | 辅助/协同肌群 |
| 器械 | 所需器械（或 `body weight` 表示自重） |
| 说明 | 每个练习的分步说明 |
| 可用语言 | 🇬🇧 英语 · 🇪🇸 西班牙语 · 🇮🇹 意大利语 · 🇹🇷 土耳其语 · 🇷🇺 俄语 · 🇨🇳 中文 · 🇮🇳 印地语 · 🇵🇱 波兰语 · 🇰🇷 韩语 · 🇫🇷 法语 |
| 媒体 | 每个练习配 180×180 缩略图（`image`）+ 动画 GIF（`gif_url`）——媒体 © Gym visual，参见[许可](#-许可与使用) |

---

## 🖥️ 交互式浏览器与开发者设置

本仓库包含两个可直接使用的 HTML 工具——无需服务器，直接在浏览器中打开即可。

> **注意：** 浏览器会显示每个练习的 180×180 缩略图和动画 GIF，以及其元数据和说明。

### `index.html` — 练习浏览器

一个完全客户端侧的动作浏览器，具有：
- 实时搜索全部 1,324 个动作
- 按分类、器械和目标肌肉筛选
- 无限滚动网格
- 支持中文/英文界面切换
- 点击卡片查看完整详情及中英文等多语言说明

### `setup.html` — 开发者设置指南

将数据集集成到你自己的应用中的分步指南：

1. **数据库设置** — SQL Server、PostgreSQL、MySQL 和 SQLite 的 `CREATE TABLE` SQL。生成可直接运行的包含全部 1,324 条 INSERT 语句的 `.sql` 文件，完全在浏览器内构建。
2. **API 集成** — **JavaScript、Python、C#、Java、PHP、Go 和 cURL** 的复制粘贴客户端代码，展示如何调用你的后端 API。输入基础 URL，所有示例实时更新。
3. **向你的 LLM 提问** — 一个结构化提示（选择你的框架 + 数据库），可粘贴到 ChatGPT、Claude 或 Gemini 中，一键生成完整的、生产就绪的 REST API。支持 Express.js、FastAPI、ASP.NET Core、Spring Boot、Laravel 和 Gin。

---

## 📂 文件结构

```
exercises-dataset/
├── data/
│   ├── exercises.json        # 完整数据集 — 1,324 条练习记录（JSON 数组）
│   └── exercises.schema.json # JSON Schema (2020-12) 描述每条记录
├── images/                  # 1,324 个 180×180 缩略图（© Gym visual）
├── videos/                  # 1,324 个 180×180 动画 GIF（© Gym visual）
├── index.html               # 交互式练习浏览器（客户端，无需服务器）
├── setup.html               # 开发者设置指南（数据库导入 + API 集成）
├── NOTICE.md                # 媒体归属与许可条款
├── README.md                # 英文说明文档
└── README_zh.md             # 中文说明文档（本文件）
```

### 关键文件

- **`data/exercises.json`** — 主要数据文件。包含 1,324 个练习对象的 JSON 数组，含有所有元数据。`image` / `gif_url` 指向本地 180×180 资源，每条记录带有 `attribution` 字段；`media_id` 保存原始媒体引用 ID。新增 `name_zh` 字段存储中文练习名称。
- **`data/exercises.schema.json`** — 一份 [JSON Schema](https://json-schema.org/) (Draft 2020-12)，正式描述每个字段、类型和约束。可用于验证数据集或你自己的新增内容。
- **`images/`, `videos/`** — 180×180 缩略图和动画 GIF（© [Gym visual](https://gymvisual.com/)，经许可使用）。
- **`index.html`** — 独立练习浏览器。支持中文/英文界面切换。可直接在任何现代浏览器中打开。
- **`setup.html`** — 开发者指南，包含数据库设置、API 集成和 LLM 辅助后端生成。支持中文/英文界面切换。
- **`LICENSE`, `NOTICE.md`** — MIT（代码/数据）+ Gym visual 媒体条款。

---

## 📊 统计数据

| 指标 | 数量 |
|---|---|
| 练习总数 | **1,324** |
| 说明语言 | **10** |

### 按身体部位统计

| 身体部位 | 练习数量 |
|---|---|
| 上臂 | 292 |
| 大腿 | 227 |
| 背部 | 203 |
| 腰部 | 169 |
| 胸部 | 163 |
| 肩部 | 143 |
| 小腿 | 59 |
| 前臂 | 37 |
| 有氧 | 29 |
| 颈部 | 2 |

### 按器械统计

| 器械 | 练习数量 |
|---|---|
| 自重 | 325 |
| 哑铃 | 294 |
| 绳索 | 157 |
| 杠铃 | 154 |
| 固定器械 | 81 |
| 弹力带 | 54 |
| 史密斯机 | 48 |
| 壶铃 | 41 |
| 负重 | 36 |
| 健身球 | 28 |
| EZ 杠 | 23 |
| 其他 | 83 |

> **注意：** 约 25% 的练习不需要任何器械——非常适合居家锻炼应用。

---

## 🗂️ 数据结构

`data/exercises.json` 中的每条记录遵循以下结构。还提供了一份机器可读的 [JSON Schema](data/exercises.schema.json) 用于验证。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `string` | 唯一数字标识符（如 `"0001"`） |
| `name` | `string` | 完整练习名称（如 `"3/4 Sit-up"`） |
| `name_zh` | `string` | 中文练习名称（如 `"四分之三仰卧起坐"`） |
| `category` | `string` | 身体部位分类（如 `"upper arms"`、`"chest"`、`"back"`） |
| `body_part` | `string` | 与 `category` 相同——目标身体部位 |
| `equipment` | `string` | 所需器械（如 `"dumbbell"`、`"body weight"`） |
| `instructions.en` | `string` | 英语完整分步说明 |
| `instructions.es` | `string` | 西班牙语完整分步说明 |
| `instructions.it` | `string` | 意大利语完整分步说明 |
| `instructions.tr` | `string` | 土耳其语完整分步说明 |
| `instructions.ru` | `string` | 俄语完整分步说明 |
| `instructions.zh` | `string` | 中文完整分步说明 |
| `instructions.hi` | `string` | 印地语完整分步说明 |
| `instructions.pl` | `string` | 波兰语完整分步说明 |
| `instructions.ko` | `string` | 韩语完整分步说明 |
| `instructions.fr` | `string` | 法语完整分步说明 |
| `instruction_steps.<lang>` | `array[string]` | 按语言分步的说明数组（`en`、`es`、`it`、`tr`、`ru`、`zh`、`hi`、`pl`、`ko`、`fr`） |
| `muscle_group` | `string` | 主要协同肌群 |
| `secondary_muscles` | `array[string]` | 参与的其他肌肉 |
| `target` | `string` | 主要目标肌肉（如 `"biceps"`、`"pectoralis major"`） |
| `media_id` | `string` | 原始媒体引用 ID（如 `"2gPfomN"`） |
| `image` | `string` | 180×180 缩略图路径（如 `"images/0001-2gPfomN.jpg"`） |
| `gif_url` | `string` | 180×180 动画 GIF 路径（如 `"videos/0001-2gPfomN.gif"`） |
| `attribution` | `string` | 媒体版权声明 —— `"© Gym visual — https://gymvisual.com/"` |
| `created_at` | `string` | ISO 8601 记录创建时间戳 |

---

## 🎬 示例练习

> 每个示例附带 180×180 缩略图（`image`）和动画 GIF（`gif_url`），© [Gym visual](https://gymvisual.com/)。

### 1 — 杠铃卧推 · 胸部

<img src="videos/0025-EIeI8Vf.gif" width="150" align="right" alt="杠铃卧推" />

> **器械：** 杠铃 · **目标：** 胸肌 · **辅助：** 肱三头肌、肩部 · **媒体 ID：** `EIeI8Vf`

杠铃卧推是胸部训练的基石，也是"三大力量举"动作之一。平躺于卧推凳上，将装载好杠铃片的下放至胸部，然后爆发性地推回。它同时调动胸肌、肱三头肌和三角肌前束，是上半身推力和胸肌围度发展的最有效动作。

**关键提示：** 出杆前收紧并下沉肩胛骨。双脚平踩地面，下背部自然拱起，保持与肩同宽的握距。控制杠铃下放至中胸位置，通过脚跟发力推起。

### 2 — 杠铃硬拉 · 大腿 / 背部

<img src="videos/0032-ila4NZS.gif" width="150" align="right" alt="杠铃硬拉" />

> **器械：** 杠铃 · **目标：** 臀肌 · **辅助：** 腘绳肌、下背部 · **媒体 ID：** `ila4NZS`

杠铃硬拉被广泛认为是终极全身力量训练动作。它几乎调动后侧链路的每一块主要肌肉——臀肌、腘绳肌和下背部——同时需要上背部、斜方肌和握力的大力参与。正确的脊柱排列和呼吸技巧对于表现和安全性都至关重要。

**关键提示：** 设置时让杠铃位于脚掌中段上方。髋部铰链，握杆位置在腿外侧，收紧核心，在整个拉起过程中保持杠铃贴近小腿。将地面推离，顶部锁定位置收紧臀肌并完全伸展髋部。

### 3 — 杠铃全蹲 · 大腿

<img src="videos/0043-qXTaZnJ.gif" width="150" align="right" alt="杠铃全蹲" />

> **器械：** 杠铃 · **目标：** 臀肌 · **辅助：** 股四头肌、腘绳肌、小腿、核心 · **媒体 ID：** `qXTaZnJ`

常被称为"动作之王"，杠铃全蹲需要整个下肢和核心的协调力量。相比半蹲，蹲至髋部低于膝盖（打破平行）能最大化臀肌和腘绳肌的激活。它是几乎所有力量和增肌训练计划的基础。

**关键提示：** 杠铃置于上斜方肌（高杠）或后三角肌（低杠）。下蹲前收紧核心，膝盖与脚尖方向一致向外推，髋部向后坐，下蹲至大腿超过与地面平行。通过整个脚掌发力站起。

### 4 — 哑铃二头弯举 · 上臂

<img src="videos/0294-NbVPDMW.gif" width="150" align="right" alt="哑铃二头弯举" />

> **器械：** 哑铃 · **目标：** 二头肌 · **辅助：** 前臂 · **媒体 ID：** `NbVPDMW`

哑铃二头弯举是最广为人知的手臂孤立训练动作。独立训练每侧有助于识别和纠正肢体间的力量不平衡。掌心向上的握姿能在动作顶点最大化二头肌的收缩。

**关键提示：** 站直，肘部固定在身体两侧。弯举时手腕外旋，顶点处挤压，然后控制下放，避免摆动。不要用肩部或下背部借力。

### 5 — 引体向上 · 背部

<img src="videos/0652-lBDjFxJ.gif" width="150" align="right" alt="引体向上" />

> **器械：** 自重 · **目标：** 背阔肌 · **辅助：** 二头肌、前臂 · **媒体 ID：** `lBDjFxJ`

引体向上是上半身拉力的黄金标准自重训练动作。它主要发展背阔肌——打造标志性的 V 形身材——同时大量调动二头肌、后三角肌和核心稳定肌群。适用于从初学者（弹力带辅助）到高级（负重）的各个阶段。

**关键提示：** 正手悬垂，握距与肩同宽或稍宽。通过下沉肩胛骨启动背阔肌，然后将胸部拉向杠铃。每组之间完全下放以保持动作幅度。

### 6 — 哑铃侧平举 · 肩部

<img src="videos/0334-DsgkuIt.gif" width="150" align="right" alt="哑铃侧平举" />

> **器械：** 哑铃 · **目标：** 三角肌 · **辅助：** 斜方肌 · **媒体 ID：** `DsgkuIt`

哑铃侧平举是打造肩宽的经典孤立训练动作。它直接针对三角肌中束，这是塑造宽肩外观的关键。节奏控制和动作规范远比重量重要。

**关键提示：** 站立，肘部在整个过程中保持微屈。将哑铃向两侧举起，直到手臂与地面平行——不要过高。肘部领先，而非手腕。控制下放以最大化张力时间。

---

## 🚀 使用示例

### Python —— 加载与筛选

```python
import json

with open("data/exercises.json", "r", encoding="utf-8") as f:
    exercises = json.load(f)

print(f"Total exercises loaded: {len(exercises)}")

# Filter by category
chest_exercises = [ex for ex in exercises if ex["category"] == "chest"]
print(f"Chest exercises: {len(chest_exercises)}")
# -> Chest exercises: 163

# Filter by equipment
bodyweight = [ex for ex in exercises if ex["equipment"] == "body weight"]
print(f"Bodyweight exercises: {len(bodyweight)}")
# -> Bodyweight exercises: 325

# Get all unique categories
categories = sorted({ex["category"] for ex in exercises})
print("Categories:", categories)

# Access multilingual instructions
ex = exercises[0]
print(ex["instructions"]["en"])  # English
print(ex["instructions"]["es"])  # Spanish
print(ex["instructions"]["it"])  # Italian
print(ex["instructions"]["tr"])  # Turkish
print(ex["instructions"]["ru"])  # Russian
print(ex["instructions"]["zh"])  # Chinese
print(ex["instructions"]["hi"])  # Hindi
print(ex["instructions"]["pl"])  # Polish
print(ex["instructions"]["ko"])  # Korean
print(ex["instructions"]["fr"])  # French
```

### Python —— 使用 Pandas

```python
import json
import pandas as pd

with open("data/exercises.json", "r", encoding="utf-8") as f:
    data = json.load(f)

df = pd.DataFrame(data)

# Top categories by exercise count
print(df["category"].value_counts().head(10))

# All barbell exercises targeting upper legs
barbell_quads = df[(df["equipment"] == "barbell") & (df["category"] == "upper legs")]
print(barbell_quads[["name", "target", "equipment"]])
```

### JavaScript / Node.js

```js
const exercises = require("./data/exercises.json");

console.log(`Total exercises: ${exercises.length}`);

// Bodyweight exercises only
const bodyweight = exercises.filter(ex => ex.equipment === "body weight");
console.log(`Bodyweight exercises: ${bodyweight.length}`);
// -> Bodyweight exercises: 325

// Group exercises by category
const byCategory = exercises.reduce((acc, ex) => {
  acc[ex.category] = (acc[ex.category] || []);
  acc[ex.category].push(ex);
  return acc;
}, {});

// Access multilingual instructions
const ex = exercises[0];
console.log(ex.instructions.en); // English
console.log(ex.instructions.es); // Spanish
console.log(ex.instructions.it); // Italian
console.log(ex.instructions.tr); // Turkish
console.log(ex.instructions.ru); // Russian
console.log(ex.instructions.zh); // Chinese
console.log(ex.instructions.hi); // Hindi
console.log(ex.instructions.pl); // Polish
console.log(ex.instructions.ko); // Korean
console.log(ex.instructions.fr); // French
```

### TypeScript —— 类型安全使用

```typescript
interface Exercise {
  id: string;
  name: string;
  name_zh?: string;
  category: string;
  body_part: string;
  equipment: string;
  instructions: {
    en: string; es: string; it: string; tr: string;
    ru: string; zh: string; hi: string; pl: string;
    ko: string; fr: string;
  };
  instruction_steps: {
    en: string[]; es: string[]; it: string[]; tr: string[];
    ru: string[]; zh: string[]; hi: string[]; pl: string[];
    ko: string[]; fr: string[];
  };
  muscle_group: string;
  secondary_muscles: string[];
  target: string;
  media_id: string;
  image: string;
  gif_url: string;
  attribution: string;
  created_at: string;
}

import exercises from "./data/exercises.json";
const data = exercises as Exercise[];

const randomWorkout: Exercise[] = data.slice(0, 6);
console.log("First 6 exercises:", randomWorkout.map(e => e.name));
```

---

## 📄 许可与使用

本仓库是一个**开发者设置向导和结构化练习数据集**——包含练习元数据、多语言指令翻译和 180×180 练习媒体。

- **代码、工具、数据集结构和指令文本**以 [MIT 许可证](LICENSE) 发布。
- **练习媒体（图片和 GIF）© [Gym visual](https://gymvisual.com/)**，经许可在此以 180×180 分辨率重新分发——参见 [`NOTICE.md`](NOTICE.md) 和 [`LICENSE`](LICENSE) 中的媒体例外条款。保留 `© Gym visual — https://gymvisual.com/` 署名。重复使用受 [Gym visual 的使用条款与条件](https://gymvisual.com/content/3-terms-and-conditions-of-use) 约束；在重用媒体前，请前往该处获取你自己的许可证。
- 本仓库**不**声明对底层练习内容或媒体的所有权。
