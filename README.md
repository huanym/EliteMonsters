# EliteMonsters

<p align="center">
  <b>精英怪与尸潮系统</b> — Purpur/Paper 1.21.5 插件
</p>

## 功能

- **精英怪自然生成** — 5% 可配置概率，支持高度/群系/时间/难度/世界黑名单条件乘数
- **动态难度** — 根据附近玩家装备自动调整精英怪星级 (可开关)
- **11 种词缀** — 狂战士、疾风剑豪、吸血伯爵、炎魔、凋零之王、爆破鬼才、不灭之盾、暗影刺客、不死之身、亡灵术士、锁魂之链
- **星级系统 (1~5⭐)** — 每级递增属性倍率、技能数、护甲材质
- **11 种技能** — 狂暴/突刺/吸血/烈焰/冰冻/爆炸/护盾/隐身/再生/召唤/锁链
- **尸潮系统** — 多波次、BossBar、Title 播报、倒计时、超时失败、参与奖励
- **独立奖励系统** — 6 种类型 (ITEM/EXP/VAULT/COMMAND/PERMISSION/GROUP)，支持 CustomModelData/头颅纹理
- **独立掉落表** — loot.yml 按生物类型/词缀/星级分层配置掉落
- **API 事件** — 5 个事件 (EliteSpawn/Death, HordeStart/Complete/Fail)，供其他插件监听
- **PlaceholderAPI 集成** — 7 个变量 (%elitemonsters_total_elites% 等)
- **RGB 渐变色 & 国际化** — Adventure Component，zh_CN / en_US 双语
- **粒子密度可配** — low/medium/high 三档适配不同性能服务器
- **配置自动迁移** — 版本升级时自动备份旧配置并替换新模板
- **错误日志系统** — errors.log 文件持久化 + /elite test errors 在线查看

## 环境要求

| 项目 | 版本 |
|------|------|
| 服务端 | Purpur / Paper 1.21.5+ |
| Java | 25 (Eclipse Temurin) |
| 可选依赖 | Vault (经济)、LuckPerms (权限)、PlaceholderAPI (变量) |

## 安装

1. 从 [Releases](https://github.com/huanym/EliteMonsters/releases) 下载 `EliteMonsters-1.2.0.jar`
2. 放入服务器 `plugins/` 目录
3. 重启服务器或使用 PlugMan 加载
4. 编辑 `plugins/EliteMonsters/config.yml`、`rewards.yml`、`loot.yml`
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
| `/elite toggle <lightning|alert>` | 开关特效 |
| `/elite clear [范围|chunk|world|type]` | 清除精英怪 |
| `/elite test info` | 运行时状态总览 |
| `/elite test spawn <生物> [词缀] [星级]` | 精确生成测试精英怪 |
| `/elite test horde` | 立即触发尸潮测试 |
| `/elite test loot <生物> [词缀] [星级]` | 验证掉落表 |
| `/elite test reward <id>` | 测试奖励发放 |
| `/elite test stress [数量]` | 压力测试 (最多50只) |
| `/elite test errors` | 查看错误历史 |
| `/elite test cleanup` | 强制清理所有精英怪 |

## 配置文件

```
plugins/EliteMonsters/
├── config.yml          # 主配置 (生成/词缀/尸潮/星级/动态难度/世界黑名单)
├── lang.yml            # 语言文件 (zh_CN / en_US)
├── rewards.yml         # 奖励配置 (6种类型)
├── loot.yml            # 掉落表 (按生物/词缀/星级分层)
├── errors.log          # 错误日志 (自动生成)
└── backup/             # 配置升级自动备份
```

### PlaceholderAPI 变量

| 变量 | 说明 |
|------|------|
| `%elitemonsters_total_elites%` | 全服精英怪数量 |
| `%elitemonsters_horde_active%` | 尸潮是否进行中 |
| `%elitemonsters_horde_wave%` | 当前波次 |
| `%elitemonsters_horde_total_waves%` | 总波次数 |
| `%elitemonsters_nearest_elite%` | 最近精英怪名称 |
| `%elitemonsters_nearest_elite_star%` | 最近精英怪星级 |
| `%elitemonsters_nearest_elite_health%` | 最近精英怪血量 |

### API 事件

```java
// 其他插件可监听:
@EventHandler
public void onEliteSpawn(EliteSpawnEvent e) { }
@EventHandler
public void onEliteDeath(EliteDeathEvent e) { }
@EventHandler
public void onHordeStart(HordeStartEvent e) { }
@EventHandler
public void onHordeComplete(HordeCompleteEvent e) { }
@EventHandler
public void onHordeFail(HordeFailEvent e) { }
```

## 构建

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd EliteMonsters
mvn clean package -DskipTests
# 输出: target\EliteMonsters-1.2.0.jar
```

## 开源协议

MIT License