<p align="center">
  <img src="img/LiteSignInBedrock.png" alt="LiteSignInBedrock 标志" width="128">
</p>

<h1 align="center">LiteSignInBedrock</h1>
<p align="center">为 LiteSignIn 提供基岩版的签到表单</p>
<p align="center">
  <a href="#兼容范围与依赖"><img src="https://img.shields.io/badge/Minecraft-1.16%2B-3fb950" alt="Minecraft 1.16+，依赖要求见下文"></a>
  <a href="#兼容范围与依赖"><img src="https://img.shields.io/badge/LiteSignIn-1.9.0_API-5865F2" alt="LiteSignIn 1.9.0编译API，需要兼容版本"></a>
  <a href="#兼容范围与依赖"><img src="https://img.shields.io/badge/floodgate-2.x-00AAFF" alt="后端需要安装floodgate 2.x"></a>
  <a href="#兼容范围与依赖"><img src="https://img.shields.io/badge/Java-8%2B_bytecode-orange" alt="Java 8字节码，运行Java要求取决于服务端与依赖"></a>
  <a href="#兼容范围与依赖"><img src="https://img.shields.io/badge/Spigot%20%2F%20Paper-supported-blue" alt="支持Spigot与Paper"></a>
  <a href="#兼容范围与依赖"><img src="https://img.shields.io/badge/Folia-conditional-blueviolet" alt="Folia有条件支持，依赖也必须兼容Folia"></a>
</p>

<p align="center"><a href="README.md">English</a> · <b>简体中文</b></p>
<p align="center">
  <a href="https://github.com/che11111/LiteSignInBedrock/releases">下载插件</a> ·
  <a href="https://github.com/che11111/LiteSignInBedrock/issues">反馈问题</a> ·
  <a href="https://bstats.org/plugin/bukkit/LiteSignInBedrock/34823">bStats</a>
</p>

LiteSignInBedrock 是 [LiteSignIn](https://www.spigotmc.org/resources/79584/) 的 Bukkit 附属插件。通过 Geyser 和 floodgate 进入服务器的基岩版玩家可以使用原生表单签到、补签和查看记录。奖励与数据存储仍由 LiteSignIn 处理。本 README 同时作为安装、配置和管理手册。

## 功能演示

<p align="center">
  <img src="img/202610294003846.gif" alt="LiteSignInBedrock 功能演示" width="800">
</p>

## 功能

- 三个表单页面：主页签到与信息页面、整月签到记录页面、补签页面。
- 主页面显示今日状态、连续签到、累计签到、本月累计、补签卡数量和今日排名。
- 一键签到，检查 LiteSignIn 的权限、禁用世界、在线时长和重复签到限制。

## 兼容范围与依赖

| 项目 | 要求与范围 |
| --- | --- |
| 服务端类型 | 「**1.16+**」 Spigot、Paper，Purpur，Folia 等。 |
| LiteSignIn | 「**必需**」，本附属基于其 **1.9.0 API** 构建。|
| floodgate | 「**必需**」，**后端 Bukkit 服务器必须安装**。需要支持现代 Cumulus 表单的2.x版本（构建基线为2.2.2）|
| Geyser | 基岩版连接链路中需要部署，可在后端或代理端；本插件不直接依赖 Geyser 插件。 |
| PlaceholderAPI | 「**必需**」，用于表单和签到聊天文案中的 `%litesignin_...%` 等占位符。 |


**本附属必须装在后端，不能装在代理端**。

## 安装与升级

1. 在后端安装兼容的 LiteSignIn 和 floodgate，配置好 Geyser 连接。代理架构需要开启 floodgate 数据转发，并按 [floodgate 文档](https://geysermc.org/wiki/floodgate/)配置后端 floodgate。
2. 从 [GitHub Releases](https://github.com/che11111/LiteSignInBedrock/releases)下载正常的 `LiteSignInBedrock-<版本>.jar`。
3. 放入服务器 `plugins/`，移除本附属的旧版重复JAR，重启服务器。
4. 修改 `plugins/LiteSignInBedrock/config.yml` 和 `lang/zh_CN.yml` 或 `lang/en_US.yml`，执行 `/litesigninbedrock reload`。
5. 基岩版玩家进入服务器，执行 `/besignin` 打开表单。

升级时保留已有配置与语言文件。启动或重载会补齐缺失配置和文案，保留已有值。无效YAML文件不会被覆盖；无效语言文件会使用内置文案，无效配置则保留上一次可用设置，首次启动没有可用配置时停止启用。补齐缺失键可能重新排版旧YAML并移除注释。

## 命令与权限

命令别名为 `/besignin`，支持与主命令相同的参数。

| 命令或权限 | 作用 | 默认 |
| --- | --- | --- |
| `/litesigninbedrock` | 打开主表单，仅玩家、仅基岩版 | `litesigninbedrock.use` |
| `/litesigninbedrock reload` | 重载配置；支持控制台 | `litesigninbedrock.admin` |
| `litesigninbedrock.use` | 使用表单，点击处理时再次检查 | 所有玩家 |
| `litesigninbedrock.admin` | 重载设置并接收更新提示 | OP |

签到还会检查 **LiteSignIn 的权限配置键**，不是本插件声明的权限节点；具体其权限节点请在 LiteSignIn 配置中查看或设置。本附属的使用权限不会绕过原插件的限制。

## 配置说明

```yaml
# LiteSignInBedrock
# 文案在 lang/<language>.yml 中修改 
language: zh_CN

# 启动5秒后及每6小时检查更新，仅提醒，不自动安装
check-for-updates: true

# bstats统计
bstats:
  enabled: true

# 进服自动弹出签到表单
auto-open-on-join: false
# 延迟秒数，0–86400 / Delay in seconds, 0–86400.
auto-open-delay: 1
# 补签回溯天数，0–3660；每页10个日期
retroactive-days: 30
# 包含本月的可查看月份数，最早为1970年1月
records-months: 12

```

补签卡获取、奖励组、禁用世界等等，在 **LiteSignIn** 中配置。


### 内部占位符

占位符只在对应模板中生效。`{month}`、`{total}`、`{cards}`在不同上下文的含义不同。

| 模板或区域 | 可用占位符 |
| --- | --- |
| `form.main.status-format` | `{status}`、`{continuous}`、`{total}`（累计天数）、`{month}`（本月签到天数）、`{cards}`（持有数量）、`{queue}` |
| `form.retroactive.content` / `page-format` | `{cards}`、`{required}`、`{page}`、`{pages}`、`{count}`（可补签日期数） |
| `form.retroactive.button-date` | `{date}`（含年份完整日期）、`{required}` |
| 记录页header / summary / today模板 | `{year}`、`{month}`（月份）、`{count}`（已签天数）、`{missed}`、`{elapsed}`、`{total}`（当月天数）、`{rate}`；今日模板另有`{status}`、`{date}`（今天几号） |
| `form.records.cell-format` / `legend` | 分别为`{day}`（两位日期）、`{mark}` / `{signed}`、`{missed}`、`{pending}`、`{future}` |
| `date.format` | `{year}`、`{month}`、`{day}`、`{weekday}` |
| `date.full-format` | `{year}`、`{date}` |
| 签到聊天 | `{prefix}`；成功有`{continuous}`，补签成功还有`{date}`；等待提示有`{minute}`或`{second}`；卡不足有`{cards}`（缺少数量） |
| 更新提醒 | `{latest}`、`{current}`、`{url}`；聊天还有`{prefix}` |



## 常见问题与功能限制

| 问题 | 检查内容 |
| --- | --- |
| 提示仅基岩版或不显示表单 | 后端是否安装floodgate，代理数据转发/密钥是否正确，玩家是否被识别为floodgate玩家；只安装Geyser不够。 |
| 没有权限 | 同时检查本附属权限和LiteSignIn中配置的权限映射。 |
| 补签列表为空 | 检查`retroactive-days`、已有历史、LiteSignIn最早补签日期。 |
| 无法补签 | 检查LiteSignIn补签开关、世界限制、补签卡物品/数量、冷却及权限。 |
| 检查更新失败 | 确保能访问`api.github.com`的HTTPS，留意GitHub匿名限流；必须先发布Release。 |

Java版玩家继续使用LiteSignIn原有签到方式，不能打开这些基岩版表单。表单不复刻箱子GUI中的奖励物品图标、奖励预览、多组奖励布局或完整历史排行榜；仅显示LiteSignIn提供的今日排名。本附属不提供独立奖励系统、数据库或资源包。

