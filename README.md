# EliteMonsters

<p align="center">
  <b>精英怪与尸潮系统</b> — Purpur/Paper 1.21.5 插件
</p>

## 功能

- **精英怪自然生成** — 5% 可配置概率，支持高度/群系/时间/难度条件乘数
- **11 种词缀** — 狂战士、疾风剑豪、吸血伯爵、炎魔、凋零之王、爆破鬼才、不灭之盾、暗影刺客、不死之身、亡灵术士、锁魂之链
- **星级系统 (1~5⭐)** — 每级递增属性倍率、技能数、护甲材质
- **11 种技能** — 狂暴/突刺/吸血/烈焰/冰冻/爆炸/护盾/隐身/再生/召唤/锁链
- **尸潮系统** — 多波次、BossBar、Title 播报、倒计时、超时失败、参与奖励
- **独立奖励系统** — 6 种类型 (ITEM/EXP/VAULT/COMMAND/PERMISSION/GROUP)，支持 CustomModelData/头颅纹理
- **RGB 渐变色 & 国际化** — Adventure Component，zh_CN / en_US 双语
- **配置自动迁移** — 版本升级时自动备份旧配置并替换新模板

## 环境要求

| 项目 | 版本 |
|------|------|
| 服务端 | Purpur / Paper 1.21.5+ |
| Java | 25 (Eclipse Temurin) |
| 可选依赖 | Vault (经济奖励)、LuckPerms (权限奖励) |

## 安装

1. 从 [Releases](https://github.com/huanym/EliteMonsters/releases) 下载 `EliteMonsters-1.1.0.jar`
2. 放入服务器 `plugins/` 目录
3. 重启服务器或使用 PlugMan 加载
4. 编辑 `plugins/EliteMonsters/config.yml` 和 `plugins/EliteMonsters/rewards.yml`
5. 执行 `/elite reload` 热重载

## 指令

| 指令 | 说明 |
|------|------|
| `/elite spawn <生物> [词缀] [等级]` | 生成精英怪 |
| `/elite reload` | 重载所有配置 |
| `/elite info` | 查看词缀列表 |
| `/elite list` | 查看附近精英怪 |
| `/elite horde start` | 手动开启尸潮 |
| `/elite horde stop` | 停止当前尸潮 |
| `/elite horde info` | 查看尸潮状态 |
| `/elite toggle <lightning\|alert>` | 开关特效 |
| `/elite clear [范围\|chunk\|world\|type]` | 清除精英怪 |

## 配置文件

```
plugins/EliteMonsters/
├── config.yml          # 主配置 (生成/词缀/尸潮/星级)
├── lang.yml            # 语言文件 (zh_CN / en_US)
├── rewards.yml         # 奖励配置 (6种类型)
└── backup/             # 配置升级自动备份
```

### rewards.yml 奖励类型

```yaml
rewards:
  my_reward:
    type: ITEM              # ITEM | EXP | VAULT | COMMAND | PERMISSION | GROUP
    material: DIAMOND_SWORD
    amount: 1
    name: "&6神剑"
    lore:
      - "&7传说之剑"
    enchantments:
      SHARPNESS: 5
    custom_model_data: 10001
    chance: 1.0             # 概率 0.0~1.0
```

## 构建

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd EliteMonsters
mvn clean package -DskipTests
# 输出: target\EliteMonsters-1.1.0.jar
```

## 开源协议

MIT License