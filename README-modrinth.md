<p align="center">
  <img src="https://raw.githubusercontent.com/che11111/LiteSignInBedrock/main/img/LiteSignInBedrock.png" alt="LiteSignInBedrock Logo" width="128">
</p>

<h1 align="center">LiteSignInBedrock</h1>
<p align="center">Provides Bedrock Edition sign-in forms for LiteSignIn</p>
<p align="center">
  <a href="#compatibility-and-dependencies"><img src="https://img.shields.io/badge/Minecraft-1.16%2B-3fb950" alt="Minecraft 1.16+, see requirements below"></a>
  <a href="#compatibility-and-dependencies"><img src="https://img.shields.io/badge/LiteSignIn-1.9.0_API-5865F2" alt="Built against the LiteSignIn 1.9.0 API; a compatible version is required"></a>
  <a href="#compatibility-and-dependencies"><img src="https://img.shields.io/badge/floodgate-2.x-00AAFF" alt="Floodgate 2.x is required on the backend server"></a>
  <a href="#compatibility-and-dependencies"><img src="https://img.shields.io/badge/Java-8%2B_bytecode-orange" alt="Java 8 bytecode; the runtime Java requirement depends on the server and its dependencies"></a>
  <a href="#compatibility-and-dependencies"><img src="https://img.shields.io/badge/Spigot%20%2F%20Paper-supported-blue" alt="Spigot and Paper supported"></a>
  <a href="#compatibility-and-dependencies"><img src="https://img.shields.io/badge/Folia-conditional-blueviolet" alt="Conditional Folia support; all dependencies must also be Folia-compatible"></a>
</p>

<p align="center"><b>English</b> · <a href="README.zh-CN.md">简体中文</a></p>
<p align="center">
  <a href="https://github.com/che11111/LiteSignInBedrock/releases">Download</a> ·
  <a href="https://github.com/che11111/LiteSignInBedrock/issues">Report an Issue</a> ·
  <a href="https://bstats.org/plugin/bukkit/LiteSignInBedrock/34823">bStats</a>
</p>

LiteSignInBedrock is a Bukkit addon for [LiteSignIn](https://www.spigotmc.org/resources/79584/). It allows Bedrock Edition players connecting through Geyser and Floodgate to sign in, perform retroactive sign-ins, and view sign-in records through native Bedrock forms. Rewards and data storage are still handled by LiteSignIn. This README also serves as the installation, configuration, and administration manual.

## Demo

<p align="center">
  <img src="https://raw.githubusercontent.com/che11111/LiteSignInBedrock/main/img/202610294003846.gif" alt="LiteSignInBedrock demo" width="800">
</p>

## Features

- Three form pages: a main sign-in and information page, a monthly sign-in records page, and a retroactive sign-in page.
- The main page displays today's sign-in status, consecutive sign-in streak, total sign-in days, sign-ins this month, available retroactive sign-in cards, and today's ranking.
- One-click sign-in, with checks for LiteSignIn permissions, disabled worlds, online time requirements, and duplicate sign-ins.

## Compatibility and Dependencies

| Component | Requirements and Compatibility |
| --- | --- |
| Server software | **1.16+**: Spigot, Paper, Purpur, Folia, and other compatible server implementations. |
| LiteSignIn | **Required**. This addon is built against the **LiteSignIn 1.9.0 API**. |
| Floodgate | **Required**. Must be installed on the backend Bukkit server. Requires a 2.x version that supports modern Cumulus forms (build baseline: 2.2.2). |
| Geyser | Required as part of the Bedrock connection setup. It can be deployed on the backend server or proxy. This plugin does not directly depend on the Geyser plugin. |
| PlaceholderAPI | **Required**. Used for placeholders such as `%litesignin_...%` in form text and sign-in chat messages. |

**This addon must be installed on the backend server, not on the proxy.**

## Installation and Upgrading

1. Install compatible versions of LiteSignIn and Floodgate on the backend server, and configure the Geyser connection. For proxy-based setups, enable Floodgate data forwarding and configure Floodgate on the backend according to the [Floodgate documentation](https://geysermc.org/wiki/floodgate/).
2. Download the appropriate `LiteSignInBedrock-<version>.jar` from [GitHub Releases](https://github.com/che11111/LiteSignInBedrock/releases).
3. Place the JAR in the server's `plugins/` directory, remove duplicate older versions of this addon, and restart the server.
4. Edit `plugins/LiteSignInBedrock/config.yml` and either `lang/zh_CN.yml` or `lang/en_US.yml`, then run `/litesigninbedrock reload`.
5. Bedrock Edition players can join the server and run `/besignin` to open the forms.

When upgrading, keep your existing configuration and language files. On startup or reload, missing configuration keys and messages are added while existing values are preserved. Invalid YAML files will not be overwritten. Invalid language files fall back to the built-in messages, while an invalid configuration retains the last known working settings. If no valid configuration is available on the first startup, the plugin will fail to enable.

Adding missing keys may reformat existing YAML files and remove comments.

## Commands and Permissions

The command alias is `/besignin`, which supports the same arguments as the main command.

| Command or Permission | Description | Default |
| --- | --- | --- |
| `/litesigninbedrock` | Opens the main form. Players only; Bedrock Edition players only. | `litesigninbedrock.use` |
| `/litesigninbedrock reload` | Reloads the configuration. Can also be executed from the console. | `litesigninbedrock.admin` |
| `litesigninbedrock.use` | Allows access to the forms. Permissions are checked again when an action is processed. | All players |
| `litesigninbedrock.admin` | Allows configuration reloading and receiving update notifications. | OP |

Sign-in actions also check the **permission configuration key used by LiteSignIn itself**, rather than a permission node declared by this addon. Refer to the LiteSignIn configuration to find or set the relevant permission node.

The addon's own usage permission does not bypass any restrictions imposed by LiteSignIn.

## Configuration

```yaml
# LiteSignInBedrock
# Edit messages in lang/<language>.yml
language: en_US

# Check for updates 5 seconds after startup and every 6 hours.
# Notifications only; updates are never installed automatically.
check-for-updates: true

# bStats metrics
bstats:
  enabled: true

# Automatically open the sign-in form when a player joins
auto-open-on-join: false

# Delay in seconds, 0–86400
auto-open-delay: 1

# Number of days to look back for retroactive sign-ins, 0–3660.
# 10 dates are displayed per page.
retroactive-days: 30

# Number of viewable months, including the current month.
# The earliest available month is January 1970.
records-months: 12
```

Retroactive sign-in cards, reward groups, disabled worlds, and other sign-in rules are configured in **LiteSignIn**.



## Frequently Asked Questions and Limitations

| Issue | What to Check |
| --- | --- |
| "Bedrock Edition only" message or forms do not appear | Check whether Floodgate is installed on the backend, proxy forwarding and keys are configured correctly, and the player is recognized as a Floodgate player. Installing Geyser alone is not sufficient. |
| Permission denied | Check both this addon's permissions and the permission mapping configured in LiteSignIn. |
| Retroactive sign-in list is empty | Check `retroactive-days`, existing sign-in history, and LiteSignIn's earliest allowed retroactive sign-in date. |
| Retroactive sign-in fails | Check LiteSignIn's retroactive sign-in settings, world restrictions, retroactive sign-in card item and quantity, cooldowns, and permissions. |
| Update checks fail | Ensure the server can access `api.github.com` over HTTPS, and check for GitHub anonymous API rate limits. A GitHub Release must be published before an update can be detected. |

Java Edition players will continue to use LiteSignIn's existing sign-in methods and cannot open these Bedrock forms.

The forms do not reproduce reward item icons from chest GUIs, reward previews, multi-group reward layouts, or the full historical leaderboard. Only the daily ranking provided by LiteSignIn is displayed.

This addon does not provide an independent reward system, database, or resource pack.