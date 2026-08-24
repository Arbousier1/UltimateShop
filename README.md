# 🛒 UltimateShop 自用 Fork

> 这是 [UltimateShop](https://github.com/ManyouTeam/UltimateShop) 的自用分支，主要用于我自己的构建、验证和维护。

## 这个 Fork 做了什么

相比原项目，本分支有以下代码层面的改动：

- **原生 Sparrow Expression 数学引擎**：使用 `sparrow-expr 1.0` 直接编译和计算表达式，不经过额外语法转换层。
- **原生 SIGMA 自定义函数**：通过 Sparrow `FunctionBinder` 注册 `SIGMA(start, end, body)` 求和函数，支持完整表达式、表达式边界、多次调用及嵌套调用。
- **数学测试与构建验证**：新建 `DecayCalculator.java` 纯 Java 计算类，从 `AmountVariableUtil` 提取核心公式逻辑（p(n)/M(n)/ε(t)/Q/n(t)/假期影响），不依赖 Bukkit API 可直接测试。新增 3 个测试文件共 2176 个测试用例（`DecayCalculatorTest`、`DecayCalculatorExtendedTest`、`AmountVariableUtilPureTest`），覆盖每个公式的边界条件、属性不变式、数值稳定性、并发安全和性能基准；新增 `:core:testDecay`、`:core:testDecayExtended`、`:core:testPureUtils`、`:core:testAll` 构建任务。
- **动态经济模型变量**：新增 `AmountVariableUtil.java`（~360 行）统一管理所有变量替换逻辑；重构 `ObjectLimit`、`ObjectSinglePrice`、`ObjectSingleProduct`，将原来分散在三处的重复变量替换代码集中到一处。新增 70+ 个文章模型变量，包括 `{P}`、`{t}`、`{p_0}`、`{Q_B}`、`{gamma}`、`{T}`、`{epsilon}`、`{iota}`、`{alpha_*}`、`{mu_*}`、`{sigma_*}`、`{lambda}`、`{delta}`、`{tau}`、`{nu}`、`{T_history}` 等，以及 `{epsilon-calculated}` 预计算环境指数、`{Q}` 自动计算额度。
- **历史周期序列存储**：在 `ObjectUseTimesCache` 中新增 `PeriodRecord` 内部类和 `sellHistory`/`buyHistory` 列表，每次周期重置时自动归档当前周期的售出/买入次数和重置时间点。最多保留 `max-history-periods` 个周期（默认 30，可配置）。数据通过 YAML 和 SQL（JSON 列）持久化。`{sell-decayed-player}`、`{sell-decayed-server}`、`{buy-decayed-player}`、`{buy-decayed-server}` 变量基于真实历史周期序列，按文章公式 `FLOOR(n_i(0) / (E ^ (delta * (t_i - tau)) + 1))` 对每个周期分别计算衰减后求和，完全还原文章中的多历史周期序列模型。
- **累计收益 M(n) 追踪**：在 `ObjectUseTimesCache` 中新增 `totalSellRevenue`/`totalBuyCost` 字段，每次交易完成后自动累加实际金额（包括物品和货币），通过 YAML 和 SQL 持久化。提供 `{sell-revenue-player}`/`{M_sell}`、`{buy-cost-player}`/`{M_buy}`、`{sell-revenue-server}`、`{buy-cost-server}` 变量，完全实现文章中的累计收益 M(n) = Σ p(n')。
- **`economy-model` 配置节**：在 `config.yml` 新增 `placeholder.data.economy-model` 配置节，包含价格衰减、时间恢复、经济环境指数、假期影响、额度池等全部模型参数，每个值都支持数字或 PlaceholderAPI 表达式。
- **中国法定节假日 API 集成**：新增 `ChinaHolidayManager.java`，启动时异步拉取 [timor.tech](https://timor.tech/api/holiday) 免费 API，自动获取国务院公告的真实放假和调休数据（不可用时回退到 [NateScarlet/holiday-cn](https://github.com/NateScarlet/holiday-cn)）。区分中长假期（春节、国庆）与小假期（元旦、清明等），自动计算 `mu_i`，提供 `{china-holiday-beta}`、`{is-china-holiday}`、`{is-china-workday}`、`{china-holiday-name}`、`{mu-*-auto}` 等变量。
- **CI 自动构建与依赖更新**：新增 `.github/workflows/build.yml`，push 时自动执行 `testMath` + `testDecay` + `testDecayExtended` + `testPureUtils` 全量测试验证 → `shadowJar` 打包并上传产物。新增 `.github/renovate.json`，Renovate Bot 每周末自动检测 Gradle 依赖更新并提 PR（排除本地 JAR 和 SNAPSHOT 版本）。
- **文档补充**：在 README 中补充了 Sparrow 原生语法（基础运算、常量、布尔、数字函数、三角函数、双曲函数）、SIGMA 用法、文章模型变量映射与公式示例、节假日 API 配置与判定逻辑。

## 🔒 No Need to Worry About Custom Item Changes

**UltimateShop** uses **NBT-based item recognition** instead of comparing entire items.

It fully supports compatibility with most popular item plugins like **MMOItems**, **eco**, **ItemsAdder**, **Nexo**, **Oraxen**, **MythicMobs**, **CraftEngine** etc..

Even if an item is enchanted, renamed with an anvil, or modified by other plugins (like lore changes), UltimateShop can still recognize it correctly and allow it to be sold.

You don't need commands to give players items. UltimateShop's built-in item syntax supports all these plugins natively — just **two lines of configuration** enable buying and selling functionality.

Need to tweak items from other plugins (e.g., change name, replace lore)? UltimateShop fully supports modifying items based on plugin-provided templates.

---

## 🧭 Menu System

UltimateShop includes a fully customizable menu system inspired by **TrMenu** slot configuration.

- Customize item layout for each shop.
- Support auto-check limits, prevent misclicks.
- Set custom click actions for buttons or products.
- Add custom buttons with actions and conditions.
- Fully configurable menus both inside and outside shops.
- One config file working both for Java chest UI and Bedrock form UI. (PREMIUM)
- Support auto update menu buttons and titles. (Update title require PREMIUM)

---

## ⏳ Item Limits and Cooldowns

UltimateShop allows you to configure:

- Global and personal limits for **buy** and **sell** (4 attributes);
- Global and personal cooldowns for **buy** and **sell** (4 attributes).

That's a total of **8 configurable attributes**, usually only found in premium shop plugins!

Reset modes include:

- Daily/weekly/monthly reset;
- Timer-based reset;
- Cron expression reset;
- Permanent (no reset);
- Custom reset via placeholders from other plugins. Support recalculating the reset time with each purchase or selling, or saving the time until the next reset time arrives.

Personal limits can be conditional — for example, VIPs can have higher limits.

Both personal and global limits support math expressions and PlaceholderAPI variables.

You can even create a **real-stock system**, where items can only be bought after others sell them, keeping the economy balanced.

Cooldowns ensure players must wait a period after each buy or sell before repeating.

---

## 💰 Highly Customizable Prices and Products

**UltimateShop** uses a **many-to-many relationship** between products and prices. This means:

- A **single product** can be bought or sold using **multiple different price options**. For example, one item could be purchased with Vault currency, PlayerPoints, or by trading another material.
- At the same time, a **single price rule** can apply to **multiple different products**. So you don't need to duplicate price settings for every item — one price definition can serve all products that link to it.

Prices and products can both be defined using **items or currency**.

Supports:

- 10+ economy plugins;
- Multiple apply times, rules and conditions per single price;
- Math operations and PlaceholderAPI variables;
- Seasonal or time-based pricing;
- Discounts and random shops.

With this flexibility, you can:

- Set VIP discounts;
- Create daily limited offers;
- Product cheaper after buy 10 times;
- Rotate daily random shops;
- Implement a **dynamic market** where frequent purchases increase prices, and frequent sales lower them.

You can even exchange money for points or create custom virtual currencies — no need for extra plugins.

Support:

- Use item or economy as products or prices.
- Use placeholder to check price and use actions to take money.
- Use contains lore or name etc. check item and take them.

---

## 📦 Item and Economy Format

UltimateShop supports **item and economy format** in:

- Prices and products;
- Menus and display items.

Powered by the **ManyouItems**, you can:

- Sell detailed vanilla items (e.g., custom cloaks, mob spawners);
- Support partial Mod items;
- Sell custom tool, custom armor, custom food, etc.;
- Almost all vanilla item component can be easily configure by using ItemFormat.
- Retrieve items directly from other plugins with just two lines of config.

Economy format supports:

- Vanilla XP and XP levels;
- 10+ third-party economy plugins.

---

## 📄 Powerful Placeholders

Support use those placeholders almost everywhere you can!

Including:

- Math Placeholder
- Cron Placeholder
- Random Placeholder
- Conditional Placeholder
- Lang Placeholder
- Compare Placeholder etc.

---

## ⚙️ Actions and Conditions

UltimateShop allows actions and conditions to be triggered by:

- Buying or selling items;
- Fail actions;
- Clicking buttons;
- Opening/Closing menus;
- and much more!

**Available actions:**

- Run commands;
- Spawn entities;
- Play sounds;
- Teleport players;
- and 10+ much more!

**Available conditions:**

- PlaceholderAPI;
- World, biome, or permission checks.

---

## 🧱 Advanced Features (Some PREMIUM Only)

- **Fully MiniMessage and Legacy Color Parser support**.
- **Common Message/Action Bar/Title/Boss Bar/Sound** support in language message!
- **Per Player Language**: Display the corresponding custom text content based on the player's client language.
- **Buy More Menu**: Choose quantity, support buy only, sell only and common buy more menu.
- **Quick-Sell Menu**: Drag and drop items for instant auto-sell.
- **Plugin Enchant Support**: Add plugin enchantments like AdvancedEnchantments via item syntax.
- **Sell Wand**: Quickly sell items inside containers by clicking them.
- **Sell Chest**: Auto selling items inside.
- **Bedrock Menu Support**: Detect Floodgate players and auto-convert GUI to Bedrock FormUI.

---

## 数学表达式

UltimateShop 可以在商品数量、价格、限购、占位符等数值项中直接计算数学表达式。

`config.yml` 中相关配置如下：

```yaml
math:
  enabled: true
  scale: 2
  static-scale: false
```

当 `math.enabled` 为 `false` 时，数值会按普通数字读取；为 `true` 时，表达式会直接交给 Sparrow Expression 计算，并按需要使用 `math.scale` 进行四舍五入。

### 基础写法

```text
1 + 1
(1 + 2) * 3
10 / 3
10 % 3
2 ^ 10
-5 + 8
2(3 + 4)
```

- 支持 `+`、`-`、`*`、`/`、`%`、`^`
- 支持括号控制优先级
- 支持一元 `+` 和 `-`
- 支持隐式乘法，比如 `2(3 + 4)` 等价于 `2 * (3 + 4)`
- 输出里可能出现科学计数法，例如 `1E+2` 表示 `100`
- Sparrow 内部使用 `double`；最终价格仍按 `math.scale` 取舍，但不适合要求超过 15～16 位有效数字的精确整数或高精度记账

### 常量

```text
PI
E
TRUE
FALSE
```

指数写法请直接使用 `E ^ x`。

```text
E ^ 1
E ^ (-0.1 * 10)
1 - E ^ (-0.1 * 5)
```

也可以使用 `EXP(x)` 表示 `E ^ x`。

### 比较与布尔

```text
1 > 0
1 >= 1
1 < 2
1 <= 2
1 == 1
1 != 2
1 < 2 && 2 < 3
1 > 2 || 2 < 3
NOT(1 > 2)
IF(1 < 2, 100, 0)
IF(1 < 2 && 2 < 3, 400, 0)
```

布尔与或请用 `&&` 和 `||`，不要写成 `AND` / `OR` 中缀形式。

### 数字函数

```text
ABS(-5)
SQRT(16)
ROUND(3.14159, 2)
FLOOR(3.9)
CEILING(3.1)
LOG(E)
LOG10(100)
FACT(5)
MIN(3, 1, 4, 2)
MAX(3, 1, 4, 2)
SUM(1, 2, 3)
AVERAGE(1, 2, 3)
RANDOM()
SWITCH(2, 1, 100, 2, 200, 0)
```

`SWITCH(value, case1, result1, case2, result2, default)` 会返回匹配项或默认值。

### 三角函数

Sparrow 原生三角函数使用弧度，反三角函数也返回弧度：

```text
SIN(PI / 2)
COS(PI)
TAN(PI / 4)
COT(PI / 4)
SEC(PI / 3)
CSC(PI / 6)
ASIN(1)
ACOS(-1)
ATAN(1)
ACOT(1)
ATAN2(1, 1)
```

角度与弧度之间可以使用 `RAD` 和 `DEG` 转换：

```text
SIN(RAD(90))
RAD(180)
DEG(PI)
```

不提供 `SINR`、`COSR` 等旧别名；`SIN`、`COS` 等函数本身就是弧度版本。

双曲函数也可用：

```text
SINH(1)
COSH(1)
TANH(1)
ASINH(1)
ACOSH(2)
ATANH(0.5)
ACOTH(2)
COTH(1)
SECH(1)
CSCH(1)
```

### 占位符参与计算

PlaceholderAPI 的值会先被替换，再进行数学计算：

```yaml
amount: "%vault_eco_balance% * 0.05"
```

商品和价格里的动态内容也可以先带上内置占位符再计算：

```yaml
amount: "100 + {buy-times-player} * 5"
amount: "100 - {sell-times-server} * 2"
amount: "MAX(10, 100 + {buy-total-server} - {sell-total-server})"
```

常见使用占位符包括：

```text
{buy-times-player}
{sell-times-player}
{buy-times-server}
{sell-times-server}
{buy-total-player}
{sell-total-player}
{buy-total-server}
{sell-total-server}
```

文本展示里也可以直接用数学占位符：

```text
{math_1+1}
{math_ROUND(10/3, 2)}
{math_SIGMA(1, 10, i)}
```

### 自定义函数 `SIGMA`

UltimateShop 通过 Sparrow `FunctionBinder` 注册一个原生数值函数：

```text
SIGMA(start, end, body)
```

`SIGMA` 会从 `start` 一直循环到 `end`，循环变量是 `i`，第三个参数必须直接写 Sparrow 数值表达式，不能写成字符串。`start` 和 `end` 也可以是表达式；SIGMA 可以嵌入普通表达式、调用多次或嵌套调用。

```text
SIGMA(1, 10, i)
SIGMA(1, 10, i * i)
SIGMA(1, 5, i ^ 3)
SIGMA(1, 4, FACT(i))
SIGMA(1, 10, IF(i % 2 == 0, i, 0))
SIGMA(1, 5, E ^ (-0.1 * i))
```

示例结果：

```text
SIGMA(1, 10, i) = 55
SIGMA(1, 10, i * i) = 385
SIGMA(1, 5, i ^ 3) = 225
SIGMA(5, 1, i) = 0
```

在 YAML 中可以直接写：

```yaml
amount: "SIGMA(1, 10, i * i)"
```

旧的 `SIGMA(1, 10, "i")` 字符串 body 写法不会被兼容转换。`SIGMA` 单次最多允许 100000 次迭代、单个表达式合计最多 1000000 次、最多嵌套 32 层。未知变量不会被当作 `0`，而是按无效表达式报错。

### 文章经济模型参数对照

以下参数整理自文章《[一种解决 Minecraft 服务器常见经济问题的新尝试（理论篇）](https://kyochigo.com/archives/1534)》。这篇文章讨论的是一种按玩家额度、历史售出量、时间恢复和服务器经济环境共同决定收购价格的模型。

结论：插件已经补充了文章公式常用的变量入口，可以直接在商品、价格和限购的 `amount` 表达式里使用。玩家活跃时长 `P`、额度相关参数 `Q/Q_B/T`、当前日期 `t`、经济环境指数 `epsilon`、假期环境参数 `alpha/mu/sigma/beta`、特别物价指数 `iota`、随机扰动 `noise`、衰减系数 `lambda`、时间恢复参数 `delta/tau`、周期上限 `nu` 都有对应变量。

实时多历史周期序列 `n_i(0)` / `n_i(t_i)` 已通过 `PeriodRecord` 存储完整实现。插件在每次周期重置时自动归档当前周期的售出/买入次数和重置时间点到 `sellHistory`/`buyHistory` 列表，按文章公式 `FLOOR(n_i(0) / (E ^ (delta * (t_i - tau)) + 1))` 对每个历史周期分别计算衰减后求和。`{sell-decayed-player}`、`{sell-decayed-server}`、`{buy-decayed-player}`、`{buy-decayed-server}` 变量基于真实的历史周期序列数据计算，无需近似或替代方案。

文章里的累计收益 `M(n) = Σ p(n')` 已通过新增 `totalSellRevenue` / `totalBuyCost` 字段实现，每次交易完成后自动累加实际金额，并提供 `{sell-revenue-player}` / `{M_sell}` 等变量。

### 插件内置变量映射

这些变量可以直接写在商品、价格或限购的 `amount` 里，前提是 `config.yml` 中 `placeholder.data.can-used-in-amount: true`。

| 插件变量 | 可对应文章参数 | 含义 |
| --- | --- | --- |
| `{sell-times-player}` | `n` | 当前周期内该玩家卖出该物品的次数 |
| `{sell-total-player}` | `n` | 该玩家累计卖出该物品的次数 |
| `{sell-times-server}` | 全服 `n` | 当前周期内全服卖出该物品的次数 |
| `{sell-total-server}` | 全服 `n` | 全服累计卖出该物品的次数 |
| `{last-sell-player}` | `t` | 距离该玩家上次卖出该物品的秒数 |
| `{last-reset-sell-player}` | `t` | 距离该玩家该物品卖出次数上次重置的秒数 |
| `{last-sell-server}` | 全服 `t` | 距离全服上次卖出该物品的秒数 |
| `{last-reset-sell-server}` | 全服 `t` | 距离全服该物品卖出次数上次重置的秒数 |
| `{buy-times-player}` | 买入侧 `n` | 当前周期内该玩家买入该物品的次数 |
| `{buy-total-player}` | 买入侧 `n` | 该玩家累计买入该物品的次数 |
| `{buy-times-server}` | 全服买入侧 `n` | 当前周期内全服买入该物品的次数 |
| `{buy-total-server}` | 全服买入侧 `n` | 全服累计买入该物品的次数 |
| `{active-ticks}` | `P` | 玩家累计游玩 tick 数 |
| `{active-seconds}` | `P` | 玩家累计游玩秒数 |
| `{active-minutes}` | `P` | 玩家累计游玩分钟数 |
| `{active-hours}` / `{P}` | `P` | 玩家累计游玩小时数 |
| `{current-day-of-year}` / `{day-of-year}` / `{t}` | `t` | 当前日期在一年中的天数，适合环境指数中的假期中心值计算 |
| `{base-price}` / `{p0}` / `{p_0}` | `p_0` | 物品原始收购价的可选全局配置值，来自 `placeholder.data.economy-model.base-price` |
| `{quota-base}` / `{Q_B}` | `Q_B` | 保底额度，来自 `placeholder.data.economy-model.quota-base` |
| `{active-weight}` / `{gamma}` | `gamma` | 活跃权重，来自 `placeholder.data.economy-model.active-weight` |
| `{total-quota}` / `{T}` | `T` | 当前周期总额度，来自 `placeholder.data.economy-model.total-quota` |
| `{quota}` / `{Q}` | `Q` | 按 `({Q_B} + {gamma} * {P}) * {T}` 算出的模型额度 |
| `{environment-index}` / `{epsilon}` | `epsilon(t)` | 手动配置或 PlaceholderAPI 提供的经济环境指数 |
| `{environment-index-calculated}` / `{epsilon-calculated}` | `epsilon(t)` | 插件按 `alpha/mu/sigma/beta/noise` 预计算出的经济环境指数 |
| `{special-price-index}` / `{iota}` | `iota(t)` | 特别物价指数 |
| `{beta}` | `beta(t)` | 周末、小假期等短期时间因素系数 |
| `{noise}` | `noise(t)` | 随机扰动或周期扰动 |
| `{noise-sigma}` / `{sigma_n}` | `sigma_n` | Gauss 噪声标准差 |
| `{noise-x}` / `{x}` | `x` | Gauss 噪声公式中的随机变量 |
| `{alpha-winter}` / `{alpha_winter}` | `alpha_i` | 寒假影响强度 |
| `{alpha-summer}` / `{alpha_summer}` | `alpha_i` | 暑假影响强度 |
| `{alpha-national}` / `{alpha_national}` | `alpha_i` | 国庆假期影响强度 |
| `{mu-winter}` / `{mu_winter}` | `mu_i` | 寒假时间中值 |
| `{mu-summer}` / `{mu_summer}` | `mu_i` | 暑假时间中值 |
| `{mu-national}` / `{mu_national}` | `mu_i` | 国庆假期时间中值 |
| `{sigma-winter}` / `{sigma_winter}` | `sigma_i` | 寒假影响范围参数 |
| `{sigma-summer}` / `{sigma_summer}` | `sigma_i` | 暑假影响范围参数 |
| `{sigma-national}` / `{sigma_national}` | `sigma_i` | 国庆假期影响范围参数 |
| `{price-decay}` / `{lambda}` | `lambda` | 价格衰减系数 |
| `{decay-delta}` / `{delta}` | `delta` | 时间恢复速度参数 |
| `{decay-tau-days}` / `{tau}` | `tau` | 时间恢复曲线中点，单位是天 |
| `{period-sell-limit}` / `{nu}` | `nu` | 单个周期内的出售数量上限，优先取商品 `sell-times-max-value`，没有时取配置 |
| `{decay-history-days}` / `{T_history}` | 历史 `T` | 按 `tau + LN(nu - 1) / delta` 推导的历史保留天数，文档中写作 `T_history` 以避免和总额度 `T` 混淆 |
| `{sell-decayed-player}` | `n_i(t_i)` | 基于真实历史周期序列，按文章公式对每个周期计算衰减后的玩家卖出量之和 |
| `{sell-decayed-server}` | 全服 `n_i(t_i)` | 基于真实历史周期序列，按文章公式对每个周期计算衰减后的全服卖出量之和 |
| `{buy-decayed-player}` | 买入侧 `n_i(t_i)` | 基于真实历史周期序列，按文章公式对每个周期计算衰减后的玩家买入量之和 |
| `{buy-decayed-server}` | 全服买入侧 `n_i(t_i)` | 基于真实历史周期序列，按文章公式对每个周期计算衰减后的全服买入量之和 |
| `{sell-revenue-player}` / `{M_sell}` | `M(n)` | 该玩家卖出该物品的累计收益（实际交易金额之和） |
| `{buy-cost-player}` / `{M_buy}` | 买入侧 `M(n)` | 该玩家买入该物品的累计花费（实际交易金额之和） |
| `{sell-revenue-server}` | 全服 `M(n)` | 全服卖出该物品的累计收益 |
| `{buy-cost-server}` | 全服买入侧 `M(n)` | 全服买入该物品的累计花费 |
| `{sell-limit-player}` | 额度上限 | 当前玩家个人卖出限制 |
| `{sell-limit-left-player}` | 剩余额度 | 当前玩家个人卖出剩余额度 |
| `{sell-limit-server}` | 全服额度上限 | 当前全服卖出限制 |
| `{sell-limit-left-server}` | 全服剩余额度 | 当前全服卖出剩余额度 |
| `{is-china-holiday}` | `beta(t)` 判定 | 今天是否为中国法定假日或周末，`1` = 是，`0` = 否（需启用 china-holiday） |
| `{is-china-workday}` | `beta(t)` 判定 | 今天是否为中国实际工作日（含调休上班），`1` = 是，`0` = 否（需启用 china-holiday） |
| `{china-holiday-name}` | — | 今天的中国假日名称，如"春节""国庆节"；周末返回"周末"；工作日返回空（需启用 china-holiday） |
| `{china-holiday-beta}` | `beta(t)` | 基于真实中国节假日自动计算的 beta 值：小假期 `0.95`，周末 `0.98`，中长假期/工作日 `1.0`（中长假期影响由 `alpha_i` 承担）（需启用 china-holiday） |
| `{mu-winter-auto}` | `mu_i` | 从 API 自动计算的中长假期（春节）时间中值，可替代手动配置的 `{mu_winter}`（需启用 china-holiday） |
| `{mu-summer-auto}` | `mu_i` | 暑假时间中值（API 无暑假数据，始终为 0，仍需手动配置 `{mu_summer}`） |
| `{mu-national-auto}` | `mu_i` | 从 API 自动计算的中长假期（国庆）时间中值，可替代手动配置的 `{mu_national}`（需启用 china-holiday） |

`last-*` 变量返回的是秒数。如果公式按天计算，可以写成 `{last-sell-player} / 86400`。

这些模型变量的默认值在 `config.yml` 里：

```yaml
placeholder:
  data:
    economy-model:
      base-price: 0
      quota-base: 0
      active-weight: 0
      total-quota: 0
      environment-index: 1
      special-price-index: 1
      beta: 1
      noise: 0
      noise-sigma: 0.025
      noise-x: 0
      alpha-winter: 0.15
      alpha-summer: 0.15
      alpha-national: 0.075
      mu-winter: 0
      mu-summer: 0
      mu-national: 0
      sigma-winter: 65536
      sigma-summer: 65536
      sigma-national: 33.1776
      period-sell-limit: 0
      price-decay: 0.05
      decay-delta: 1
      decay-tau-days: 7
```

这些配置值也可以写 PlaceholderAPI，例如：

```yaml
environment-index: "%custom_environment_index%"
special-price-index: "%custom_special_price_index%"
noise: "{random_daily}"
```

`p_0` 通常是每个商品自己的基础价格，最直接的写法是在商品 `amount` 里写数字，例如 `100 * E ^ ...`。如果你确实需要统一从配置读取基础价，也可以使用 `{p_0}`。

### 中国法定节假日 API 集成

插件支持通过在线 API 自动获取中国法定节假日和调休数据，确保节假日相关变量与国务院办公厅每年公告的真实放假安排一致，包括调休上班日。

实现遵循文章模型中 `alpha_i`（中长假期）与 `beta(t)`（短期时间因素）的分工：
- **中长假期**（春节、国庆等）的主要影响由 `epsilon(t)` 中的 `alpha_i` 承担，`beta(t)` 返回 `1.0`（不在 beta 层重复折扣）
- **小假期**（元旦、清明、劳动节、端午、中秋等）由 `beta(t)` 承担，取 `0.95`
- 这样避免了中长假期在 `epsilon(t)` 和 `beta(t)` 中被双重计算

#### 启用方式

在 `config.yml` 中设置：

```yaml
placeholder:
  data:
    china-holiday:
      enabled: true
      api-url: 'https://timor.tech/api/holiday/year/{year}'
      refresh-interval-ticks: 72000
      major-holiday-winter: '春节'
      major-holiday-summer: ''
      major-holiday-national: '国庆节'
      beta-minor-holiday: 0.95
      beta-weekend: 0.98
      beta-workday: 1.0
```

| 配置项 | 含义 | 默认值 |
| --- | --- | --- |
| `enabled` | 是否启用中国节假日 API | `false` |
| `api-url` | 节假日数据 API 地址，`{year}` 会被替换为当前年份 | `https://timor.tech/api/holiday/year/{year}` |
| `refresh-interval-ticks` | 自动刷新间隔（tick），72000 = 约 1 小时 | `72000` |
| `major-holiday-winter` | 对应文章 `alpha_winter` 的中长假期名称，API 数据中名称包含此值的假日被视为"中长假期" | `春节` |
| `major-holiday-summer` | 对应文章 `alpha_summer` 的中长假期名称（API 无暑假数据，留空即可） | 空 |
| `major-holiday-national` | 对应文章 `alpha_national` 的中长假期名称 | `国庆节` |
| `beta-minor-holiday` | 小假期的 beta 值（元旦、清明、劳动节、端午、中秋等） | `0.95` |
| `beta-weekend` | 普通周末的 beta 值 | `0.98` |
| `beta-workday` | 工作日的 beta 值 | `1.0` |

#### 数据来源

默认使用 [timor.tech](https://timor.tech/api/holiday) 免费节假日 API，数据来源于国务院办公厅公告。如果该 API 不可用，插件会自动回退到 [NateScarlet/holiday-cn](https://github.com/NateScarlet/holiday-cn) 的 GitHub 开源数据。

#### 判定逻辑

遵循文章中 `alpha_i`（中长假期）与 `beta(t)`（短期时间因素）的分工：

- **中长假期**（春节、国庆等）：`is-china-holiday = 1`，`china-holiday-beta = 1.0`。主要经济环境影响由 `epsilon(t)` 中的 `alpha_i` 承担，`beta(t)` 不额外折扣
- **小假期**（元旦、清明、劳动节、端午、中秋等）：`is-china-holiday = 1`，`china-holiday-beta = 0.95`。这些假期没有对应的 `alpha_i`，全部影响由 `beta(t)` 承担
- **调休上班日**（如国庆后的周六补班）：`is-china-workday = 1`，`china-holiday-beta = 1.0`
- **普通周末**：不在 API 数据中的周六周日，`is-china-holiday = 1`，`china-holiday-beta = 0.98`

#### `mu_i` 自动计算

启用 API 后，插件会自动计算中长假期的 `mu_i`（时间中值，单位为一年中的第几天），提供以下变量：

| 变量 | 含义 | 对应文章参数 |
| --- | --- | --- |
| `{mu-winter-auto}` | 春节假期的天数中值 | `mu_winter` |
| `{mu-summer-auto}` | 暑假天数中值（API 无数据，始终为 0） | `mu_summer` |
| `{mu-national-auto}` | 国庆假期的天数中值 | `mu_national` |

可以在 `economy-model` 配置中用这些变量替代手动值：

```yaml
economy-model:
  mu-winter: '{mu-winter-auto}'
  mu-summer: 213
  mu-national: '{mu-national-auto}'
```

#### 可直接复制的公式

**推荐写法**：`{china-holiday-beta}` 与 `{epsilon-calculated}` 配合使用，符合文章分工——中长假期由 `epsilon` 承担主要影响，`beta` 只处理短期因素：

```yaml
amount: "ROUND(MAX(1, {epsilon-calculated} * {china-holiday-beta} * {iota} * 100 * E ^ (-{lambda} * {sell-decayed-player})), 2)"
```

仅使用 `{china-holiday-beta}` 而不用 `{epsilon-calculated}`（不配置 `alpha_i` 时的简化版）：

```yaml
amount: "ROUND(MAX(1, {china-holiday-beta} * {iota} * 100 * E ^ (-{lambda} * {sell-decayed-player})), 2)"
```

仅在假日生效的加价（工作日原价，假日涨价 5%）：

```yaml
amount: "ROUND(MAX(1, (1 + 0.05 * {is-china-holiday}) * 100 * E ^ (-{lambda} * {sell-decayed-player})), 2)"
```

假日禁止收购：

```yaml
amount: "IF({is-china-holiday} == 0, ROUND(MAX(1, 100 * E ^ (-{lambda} * {sell-decayed-player})), 2), -1)"
```

调休上班日额外收购额度：

```yaml
sell-limits:
  default: "ROUND(({Q_B} + {gamma} * {P}) * {T} * (1 + 0.2 * {is-china-workday}), 0)"
```

### 物品参数是否可以单独设置

`economy-model` 下的参数（`lambda`、`beta`、`alpha-*`、`mu-*`、`sigma-*` 等）是**全局配置**，所有商品共用。但每个商品可以在自己的 `amount` 表达式中直接写不同的基础价格和公式，实现等效的"单独设置"：

```yaml
items:
  A:
    sell-prices:
      1:
        economy-plugin: Vault
        amount: "ROUND(MAX(1, 100 * E ^ (-0.05 * {sell-decayed-player})), 2)"
  B:
    sell-prices:
      1:
        economy-plugin: Vault
        amount: "ROUND(MAX(1, 50 * E ^ (-0.1 * {sell-decayed-player})), 2)"
```

上面物品 A 的基础价是 100、衰减系数 0.05，物品 B 的基础价是 50、衰减系数 0.1，互不影响。如果需要更复杂的 per-item 参数，可以通过 PlaceholderAPI 在 `amount` 中引用外部变量。

### 插件变量公式写法

最简单的玩家个人动态收购价：

```yaml
amount: "ROUND(MAX(1, 100 * E ^ (-{lambda} * {sell-total-player})), 2)"
```

只按当前周期卖出次数衰减：

```yaml
amount: "ROUND(MAX(1, 100 * E ^ (-{lambda} * {sell-times-player})), 2)"
```

带经济环境指数和特别物价指数：

```yaml
amount: "ROUND(MAX(1, ({epsilon} + {noise}) * {iota} * 100 * E ^ (-{lambda} * {sell-total-player})), 2)"
```

同时考虑玩家个人售出量和全服售出量：

```yaml
amount: "ROUND(MAX(1, 100 * E ^ (-{lambda} * {sell-total-player}) * E ^ (-0.001 * {sell-total-server})), 2)"
```

使用插件提供的近似时间恢复售出量：

```yaml
amount: "ROUND(MAX(1, ({epsilon} + {noise}) * {iota} * 100 * E ^ (-{lambda} * {sell-decayed-player})), 2)"
```

如果该商品配置了 sell limit，可用剩余额度控制是否继续收购：

```yaml
amount: "IF({sell-limit-left-player} != 0, ROUND(MAX(1, ({epsilon} + {noise}) * {iota} * 100 * E ^ (-{lambda} * {sell-decayed-player})), 2), -1)"
```

按文章额度池思路计算玩家额度，可写在 sell limit 里：

```yaml
sell-limits:
  default: "ROUND(({Q_B} + {gamma} * {P}) * {T}, 0)"
```

#### 玩家额度池

| 参数 | 含义 |
| --- | --- |
| `Q` | 单个玩家在周期内获得的收购额度 |
| `Q_B` | 保底收购额度 |
| `P` | 玩家活跃时长或活跃系数 |
| `gamma` | 活跃时长权重系数 |
| `T` | 当前周期的总收购额度 |

可写成近似权重表达式：

```text
(Q_B + gamma * P) * T
```

落到配置里时可以直接使用插件变量，例如作为个人卖出限额：

```yaml
sell-limits:
  default: "ROUND(({Q_B} + {gamma} * {P}) * {T}, 0)"
```

#### 动态收购价格

| 参数 | 含义 |
| --- | --- |
| `p(n)` | 某玩家售出某物品达到历史数量 `n` 后的单价 |
| `M(n)` | 玩家通过该物品累计获得的货币量 |
| `n` | 玩家对该物品的历史售出数量 |
| `lambda` | 价格衰减系数，越大则价格下降越快 |
| `p_0` | 物品原始收购价 |
| `epsilon(t)` | 经济环境指数 |
| `iota(t)` | 特别物价指数，例如活动期间指定物品涨价 |
| `t` | 当前时间、周期或距离出售记录产生的时间 |

文章中的核心单价模型可以转成 Sparrow 原生表达式（插件内置已做 `round{·, 2}` 处理）：

```text
ROUND(MAX(0.01, epsilon * iota * p_0 * E ^ (-lambda * n)), 2)
```

如果直接使用 UltimateShop 的玩家历史卖出总量，可以写成：

```yaml
amount: "{epsilon} * {iota} * 100 * E ^ (-{lambda} * {sell-total-player})"
```

也可以使用 `min-amount` 或 `MAX` 给价格设置底线：

```yaml
amount: "MAX(1, {epsilon} * {iota} * 100 * E ^ (-{lambda} * {sell-total-player}))"
```

#### 时间恢复与历史售出量衰减

| 参数 | 含义 |
| --- | --- |
| `n_i(0)` | 第 `i` 个历史周期内记录的售出数量 |
| `n_i(t_i)` | 第 `i` 个历史周期经过时间 `t_i` 后仍然计入的有效售出数量 |
| `delta` | 逆时间系数，控制恢复速度 |
| `tau` | 时间系数，控制衰减曲线中点 |
| `nu` | 单个周期内的出售数量上限 |
| `T_history` | 需要保留和计算的最大历史时间，避免和总额度 `T` 混淆 |

插件会在每次周期重置时将当前周期的售出/买入次数归档到历史序列中（最多保留 `max-history-periods` 个周期，默认 30）。每个历史周期记录包含售出次数、买入次数和重置时间点。

单个周期的有效售出量按文章公式计算：

```text
FLOOR(n_i(0) / (E ^ (delta * (t_i - tau)) + 1))
```

其中 `t_i` 是从该周期重置到当前经过的天数。

`{sell-decayed-player}` 等变量会对所有历史周期分别计算衰减值后求和，再加上当前周期的时间衰减值，得到完整的 `n(t)` 估计。

```yaml
amount: "MAX(1, 100 * E ^ (-{lambda} * {sell-decayed-player}))"
```

`{nu}` 会优先读取商品的 `sell-times-max-value`，没有配置时读取 `placeholder.data.economy-model.period-sell-limit`。`{T_history}` 会按 `tau + LN(nu - 1) / delta` 的含义在插件内部用自然对数推导出历史保留天数。

如果想按买入量做时间恢复，可以使用：

```yaml
amount: "MAX(1, 100 * E ^ (-{lambda} * {buy-decayed-player}))"
```

#### 经济环境指数

| 参数 | 含义 |
| --- | --- |
| `alpha_i` | 中长假期对经济环境指数的影响强度 |
| `mu_i` | 某个中长假期的时间中值 |
| `sigma_i` | 某个中长假期影响范围的宽度参数 |
| `beta(t)` | 周末、小假期等短期时间因素的系数 |
| `noise(t)` | 随机扰动，用于制造不可预测的小幅波动 |
| `sigma_n` | Gauss 噪声标准差 |
| `x` | Gauss 分布密度函数中的随机变量 |

文章中给出的参考取值：

| 参数 | 参考值 |
| --- | --- |
| `alpha_winter` | `0.15` |
| `alpha_summer` | `0.15` |
| `alpha_national` | `0.075` |
| `sigma_winter` | 满足 `sigma_winter ^ 0.25 = 16` |
| `sigma_summer` | 满足 `sigma_summer ^ 0.25 = 16` |
| `sigma_national` | 满足 `sigma_national ^ 0.25 = 2.4` |
| `sigma_n` | `0.025` |
| `beta(t)` | 小假期为 `0.95`，普通周末为 `0.98`，其它时间为 `1` |

插件提供了 `{epsilon-calculated}` 来按文章思路预计算环境指数。`{t}` 是当前日期在一年中的天数，`mu_*` 也建议按同一单位配置。预计算结构相当于：

```text
(1 - (
  {alpha_winter} * E ^ (-(({t} - {mu_winter}) ^ 6) / (2 * {sigma_winter} ^ 2)) +
  {alpha_summer} * E ^ (-(({t} - {mu_summer}) ^ 6) / (2 * {sigma_summer} ^ 2)) +
  {alpha_national} * E ^ (-(({t} - {mu_national}) ^ 6) / (2 * {sigma_national} ^ 2))
)) * {beta} + {noise}
```

如果要直接放进价格，建议使用预计算后的变量，避免 Sparrow 在极大负指数上产生非有限结果：

```yaml
amount: "ROUND(MAX(1, {epsilon-calculated} * {iota} * 100 * E ^ (-{lambda} * {sell-decayed-player})), 2)"
```

`noise(t)` 这类随机扰动不适合直接放进商店价格表达式里每次点击都重新随机，否则玩家看到的价格可能频繁跳动。更稳的做法是用定时任务、脚本或随机占位符按周期生成一个固定值，再通过 PlaceholderAPI 或自定义占位符传入。

```yaml
amount: "({epsilon} + {noise}) * {iota} * 100 * E ^ (-{lambda} * {sell-total-player})"
```

#### 特别物价指数

| 参数 | 含义 |
| --- | --- |
| `iota(t)` | 人工设置的特别物价指数 |

它适合用在活动、节日或主题收购里。例如伐木节期间让原木类物品价格提高：

```yaml
amount: "{epsilon} * 1.25 * 100 * E ^ (-{lambda} * {sell-total-player})"
```

#### 四舍五入

文章里的 `round(x, n)` 对应 Sparrow 原生表达式的：

```text
ROUND(x, n)
```

例如：

```yaml
amount: "ROUND({epsilon} * 100 * E ^ (-{lambda} * {sell-total-player}), 2)"
```

---

### ❤️ UltimateShop — The Most Flexible Economy Shop System

Consider respect my work and buy the plugin here, you can get free support, submit suggestion service. [Click to buy](https://www.spigotmc.org/resources/ultimateshop-premium-menu-dynamic-price-limits-apply-settings-sell-all-and-more-1-17-1-20.113069/)

You can also get free version here. [Click to download](https://www.spigotmc.org/resources/ultimateshop-menus-limits-apply-settings-10-directly-hook-and-more-1-17-1-20.110601/)
