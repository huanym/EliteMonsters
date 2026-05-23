# EliteMonsters - 开发会话交接文档

> 最后更新: 2026-05-23 | 版本: v1.3.0 | 编译: BUILD SUCCESS (34文件 0错误)
> 工作目录: E:\服务端\插件\EliteMonsters | Git: D:\Git\bin\git.exe
> GitHub: https://github.com/huanym/EliteMonsters
> 测试服务器: E:\服务端\插件\26.1.2 (Purpur 26.1.2-2585)

---

## 环境速查

```powershell
# JDK
$env:JAVA_HOME = "$env:USERPROFILE\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Git
$env:Path = "D:\Git\bin;$env:Path"

# Maven
$mvn = "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd"

# 编译
cd "E:\服务端\插件\EliteMonsters"
& $mvn clean package -DskipTests
# 输出: target\EliteMonsters-1.2.0.jar

# 部署+测试（编译后自动部署到测试服）
Copy-Item target\EliteMonsters-1.2.0.jar 'E:\服务端\插件\26.1.2\plugins\EliteMonsters-1.2.0.jar' -Force
cd 'E:\服务端\插件\26.1.2'
java -Xmx2G -jar purpur-26.1.2-2585.jar nogui
```

---

## 架构全景

```
玩家交互层
  EliteCommand     /elite 指令 (spawn/reload/info/list/horde/toggle/test/clear/gui)
  EliteGUI          GUI 管理面板 (7个菜单: 生成/列表/尸潮/设置/快捷)

业务逻辑层
  EliteGenerationListener   精英怪生成 + 属性/盔甲/战斗/卸载 + 掉落触发 + 世界黑名单 + WG检查
  HordeManager              尸潮核心 (Session/波次/Title+BossBar/超时/奖励/事件)
  SkillManager              11 技能 + 受击反馈 (tick 40L+随机偏移防扎堆)
  VisualManager             视觉 (HP条/粒子密度/音效/警报, distanceSquared 优化)
  AffixManager              词缀加载/随机/验证
  RewardManager             6 种奖励 (ITEM/EXP/VAULT/COMMAND/PERMISSION/GROUP)
  LootManager               掉落表 (loot.yml 分层匹配, 首个命中即返回)
  EquipmentManager          精英装备掉落 (equipment.yml 14模板, 星级+词缀匹配)

基础设施层
  ConfigManager             配置加载 + 动态概率 + 反射同步 + 高频值缓存 + debugLog
  LangManager               国际化 (zh_CN/en_US + 渐变色)
  GradientUtil              Adventure Component 渐变
  ParticleManager           粒子几何图形 (圆/螺旋/球/爆发/线)
  ErrorLogger               错误日志 (errors.log + 内存 50 条 + 堆栈跟踪)
  ElitePlaceholders         PlaceholderAPI 扩展 (7 变量)

集成层
  WorldGuardHelper          WorldGuard 区域黑白名单 (纯反射, 零编译依赖)
  MythicMobsHelper          MythicMobs 精英转换 (纯反射, 零编译依赖)

API 事件层
  EliteSpawnEvent / EliteDeathEvent
  HordeStartEvent / HordeCompleteEvent / HordeFailEvent
```

---

## 关键决策 (21项)

| # | 决策 | 位置 |
|---|------|------|
| 1 | Adventure Component 全消息 | LangManager.getComponent() |
| 2 | 渐变色语法 <g:#:#>text</g> | GradientUtil.parse() |
| 3 | 六进制色 &#RRGGBB | Paper LegacyComponentSerializer |
| 4 | 尸潮 Session 模式 | HordeManager.HordeSession |
| 5 | 奖励只给参战者 | onHordeCombat 自动登记 participant |
| 6 | 配置反射同步 | ConfigManager.load() 反射 JavaPlugin.config |
| 7 | 配置自动迁移 | migrateConfigs() 检测 config-version |
| 8 | 精英怪卸载恢复 | EliteMobData 存原始属性, onDisable() -> revertAllElites() |
| 9 | 独立奖励系统 | rewards.yml 6 种类型, 概率/CustomModelData/头颅纹理 |
| 10 | 独立掉落表 | loot.yml 按生物类型/词缀/星级分层 |
| 11 | 全局属性倍率 | global-attribute-scale (默认 0.5) 统一缩放 |
| 12 | 动态难度 | dynamic-difficulty 根据附近玩家装备自适应星级 |
| 13 | 粒子密度 | particle-density low/medium/high 三档 |
| 14 | 粒子 task 管理 | Map<UUID,BukkitTask> 集中管理, 防泄漏 |
| 15 | EliteMobData 清理 | 每 10 秒定时清理无效实体引用 |
| 16 | distanceSquared 优化 | VisualManager 全部用距离平方代替距离 |
| 17 | ConfigManager 缓存 | starChar 等 6 字段高频值缓存 |
| 18 | Stream->for-loop | HordeManager 波次生成改用循环 |
| 19 | 技能 tick 减半+偏移 | SkillManager 40L + 随机偏移防扎堆 |
| 20 | GUI 用 PDC 标记 | EliteGUI 用 PersistentDataContainer 存储 action |
| 21 | 集成用纯反射 | WorldGuard/MythicMobs 零编译依赖 |

---

## 文件清单 (38文件)

```
EliteMonsters/
  pom.xml                             # Maven v1.2.0, Paper 1.21.5, shade 3.6.0
  PROJECT.md / README.md / TESTING.md / SESSION.md
  .gitignore
  src/main/resources/
    plugin.yml                        # softdepend: Vault/LuckPerms/PlaceholderAPI/WorldGuard
    config.yml                        # v4: bstats/worldguard/mythicmobs/world-blacklist/dynamic-difficulty
    lang.yml                          # v1: zh_CN/en_US 完整双语 (含 gui 帮助)
    rewards.yml                       # v1: 6种奖励 (修复 config-version 注释)
    loot.yml                          # v1: 掉落表 (config-version 正常)
    equipment.yml                     # v1: 装备掉落模板 14套
  src/main/java/com/elitemonsters/plugin/
    EliteMonstersPlugin.java          # 入口 (迁移/PlugMan/WorldGuard/Equipment/MythicMobs/GUI)
    ErrorLogger.java                  # 错误日志 (文件+内存双写, 50条缓存)
    ElitePlaceholders.java            # PlaceholderAPI 扩展 (7变量)
    LootManager.java                  # 掉落表 (分层匹配, 首个命中即返回)
    api/                              # 5个API事件
    affix/                            # AffixData + AffixManager (11词缀)
    command/EliteCommand.java         # /elite全套 (含 gui 子命令)
    config/                           # ConfigManager + LangManager
    equipment/EquipmentManager.java   # 装备掉落 (14模板, 星级+词缀匹配)
    generation/                       # EliteGenerationListener + EliteMobData
    gui/EliteGUI.java                 # GUI管理面板 (7菜单, PDC标记)
    horde/                            # HordeManager + HordeNightListener
    integration/                      # WorldGuardHelper + MythicMobsHelper (反射)
    reward/                           # 7文件 (6类型+Manager, 共享Random)
    skill/SkillManager.java           # 11技能 + 受击反馈
    visual/                           # VisualManager + ParticleManager + GradientUtil
```

---

## 已完成功能

### 精英怪核心
- [x] 自然生成 (动态概率/区块上限/黑白名单/世界黑名单/WorldGuard区域)
- [x] 全局属性倍率 (global-attribute-scale 0.5)
- [x] 动态难度 (dynamic-difficulty 根据玩家装备自适应星级)
- [x] 卸载恢复 (EliteMobData + revertAllElites)
- [x] EliteMobData 清理线程 (每 10s 清除无效实体)
- [x] getEntity() 安全化 (内置 isValid+!isDead 检查)

### 词缀系统
- [x] 11 种词缀: 狂战士/疾风剑豪/吸血伯爵/炎魔/凋零之王/爆破鬼才/不灭之盾/暗影刺客/不死之身/亡灵术士/锁魂之链
- [x] 各词缀独立属性/技能/粒子/音效配置
- [x] 词缀中文化趣味名称

### 星级系统
- [x] 1~5 星, 每级独立 attribute-multiplier/skill-count/armor-tier
- [x] 动态难度自适应星级

### 技能系统
- [x] 11 种技能: 狂暴/突刺/吸血/烈焰/冰冻/爆炸/护盾/隐身/再生/召唤/锁链
- [x] 吸血伯爵 VAMPIRIC_LIFESTEAL 已实现 (周期AOE吸血+自愈, 8s CD)
- [x] 受击反馈系统 (11 词缀各自独立受击效果)
- [x] 技能 CD 系统 (每精英怪 UUID 独立冷却)
- [x] tick 减半 40L + 随机偏移 (防扎堆)

### 尸潮系统
- [x] Session 模式 + 多波次 + BossBar + Title 播报
- [x] 自动/夜晚随机/手动 三种触发
- [x] 超时失败 failHorde()
- [x] 参战奖励 (仅战斗参与者)
- [x] 仅玩家击杀推进波次 (playerKilledMobs 计数)
- [x] 安全生成 getSafeSpawnLocation()
- [x] 尸潮怪物高亮 glow-mobs
- [x] center-mode: random_player/world_spawn

### 奖励与掉落
- [x] 6 种奖励: ITEM/EXP/VAULT/COMMAND/PERMISSION/GROUP
- [x] 独立掉落表 loot.yml (按生物/词缀/星级分层)
- [x] 独立装备掉落 equipment.yml (14模板, 星级+词缀匹配, 附魔/名称/描述)
- [x] CustomModelData + PLAYER_HEAD 头颅纹理
- [x] 组合奖励 GroupRewardData 递归嵌套
- [x] 共享 Random 实例 (避免每次 new)

### 视觉与性能
- [x] RGB 渐变色 <g:#:#>text</g>
- [x] 粒子密度 low/medium/high
- [x] distanceSquared 替代 distance()
- [x] 粒子 task 集中管理 Map<UUID,BukkitTask>
- [x] ConfigManager 高频值缓存 (6字段)
- [x] VisualManager 合并玩家遍历 (2次->1次)
- [x] onCreatureSpawn 用 .name() 代替 .getKey().getKey()
- [x] onEliteDeath 用 .name() 代替 .getKey().getKey()

### GUI 管理面板
- [x] /elite gui 主菜单 (7个入口)
- [x] 生物类型选择 (分页, 过滤不可生成类型)
- [x] 词缀选择 (11种+随机)
- [x] 星级选择 (1~5 可视化材质)
- [x] 精英怪列表 (分页, 按距离排序)
- [x] 尸潮控制 (启动/停止/状态)
- [x] 设置面板 (Debug/动态难度 开关)
- [x] 快捷生成 (7个预设)
- [x] PDC 标记防物品拖走
- [x] safeMaterial 兜底防崩溃

### 可观测性
- [x] Debug 模式 (控制台 + ErrorLogger)
- [x] errors.log 文件持久化
- [x] /elite test 命令套件 (info/spawn/horde/loot/reward/stress/errors/cleanup)
- [x] TESTING.md 测试调试指南
- [x] 减少噪音日志 (反射失败仅debug/文件已存在无警告)

### API 与集成
- [x] 5 个 API 事件
- [x] PlaceholderAPI 7 变量
- [x] WorldGuard 区域黑白名单 (纯反射, 零编译依赖)
- [x] MythicMobs 兼容 (纯反射, 零编译依赖)
- [x] plugin.yml softdepend: Vault/LuckPerms/PlaceholderAPI/WorldGuard
- [x] onDisable 完整清理所有 Manager

---

## 待办

### 高优先级
- [ ] MythicMobs 兼容 实机验证
- [ ] WorldGuard 兼容 实机验证
- [ ] 精英怪 AI 增强 — 巡逻/仇恨/逃跑
- [ ] PlaceholderAPI 变量扩展 — 更多玩家侧变量

### 中优先级
- [ ] bStats 统计 — 需解决 shade+ASM Java25 兼容 (暂搁置)
- [ ] 副本/Arena 系统 — Boss 竞技场
- [ ] NPC 系统 — NPC 交互/任务
- [ ] GUI 自定义 — 可通过配置自定义 GUI 物品/布局

### 低优先级
- [ ] 粒子颜色支持 (DustOptions for REDSTONE)
- [ ] 更多粒子图案 (心形/星形/漩涡)
- [ ] GitHub Actions CI/CD

---

## Bug 修复记录 (v1.3.0)

| # | 问题 | 严重度 | 状态 |
|---|------|:--:|:--:|
| 1 | VAMPIRIC_LIFESTEAL 技能空 lambda | 🔴 | ✅ |
| 2 | /elite reload 后粒子密度不更新 | 🟡 | ✅ |
| 3 | ConfigManager.save() 编码乱码 | 🟡 | ✅ |
| 4 | 高度条件 if-else 互斥 | 🟡 | ✅ |
| 5 | 尸潮非玩家击杀也推进波次 | 🟡 | ✅ |
| 6 | 生成特效双重触发（闪电+提醒 x2） | 🔴 | ✅ |
| 7 | handleClear 类型过滤逻辑反了 | 🔴 | ✅ |
| 8 | config-version 被注释导致每次重启配置覆盖 | 🔴 | ✅ |
| 9 | RewardManager instanceof Map 失败→0 rewards | 🔴 | ✅ |
| 10 | onEnable 漏了3个新Manager初始化 | 🔴 | ✅ |
| 11 | GUI GIANT_SPAWN_EGG 崩溃 | 🔴 | ✅ |
| 12 | GUI 物品可拖走/点击无效 | 🔴 | ✅ |

---

## 配置模板注意事项

### 编码铁律
- 所有 .java .yml .xml 必须 **UTF-8 无 BOM**
- 用 `[System.Text.UTF8Encoding]::new($false)` 读写

### config-version 铁律
- 各配置文件第一行 **必须是非注释的** `config-version: N`
- 模板改配置项时需递增版本号
- 迁移逻辑: 检测 config-version, 旧版备份到 backup/

### lang.yml 铁律
- 新增 key 必须 zh_CN + en_US 都加

### PowerShell 坑
- 换行符用 `\r\n` 匹配 Windows 文件
- `.Replace()` 的 old/new 字符串中不要含单引号（会被 PowerShell 解析）
- pipe 字符会被 shell 解析, 需转义

---

## Git 记录

```
3e8973c chore: remove generated file
af837bb fix: GUI rewrite - fix item dragging, click detection, crash on missing spawn eggs
d4b9d05 chore: add dependency-reduced-pom.xml to .gitignore
4f91106 feat: GUI management panel /elite gui
225f5df v1.3.0: bug fixes + WorldGuard/equipment/MythicMobs + optimizations
3e2b875 docs: SESSION.md session handoff document
c59a927 docs: update PROJECT.md README.md to v1.2.0, i18n test commands, bump version
c2fd286 v1.2.0: API events, PlaceholderAPI, loot table, dynamic difficulty, error logger, performance, test commands
dc38de3 docs: PROJECT.md
7c7daa7 docs: PROJECT update
e172557 fix: reflection+debug+PlugMan
88427a3 fix: reload+horde+scale
d4fa1fa docs: README
8d3f720 fix: horde+migrate+revert+v1.1.0
fb020ed feat: reward system
e4c6116 feat: elite+affix+skill+horde+cmd
ad33ce7 feat: core+config+lang
1ea08b9 chore: skeleton
```

---

## /elite 指令全集

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
| `/elite gui` | 打开 GUI 管理面板 |
| `/elite test info` | 运行时状态总览 |
| `/elite test spawn <生物> [词缀] [星级]` | 精确生成测试精英怪 |
| `/elite test horde` | 立即触发尸潮测试 |
| `/elite test loot <生物> [词缀] [星级]` | 验证掉落表 |
| `/elite test reward <id>` | 测试奖励发放 |
| `/elite test stress [数量]` | 压力测试 (最多50只) |
| `/elite test errors` | 查看错误历史 |
| `/elite test cleanup` | 强制清理所有精英怪 |

---

## PlaceholderAPI 变量

| 变量 | 说明 |
|------|------|
| `%elitemonsters_total_elites%` | 全服精英怪数量 |
| `%elitemonsters_horde_active%` | 尸潮是否进行中 |
| `%elitemonsters_horde_wave%` | 当前波次 |
| `%elitemonsters_horde_total_waves%` | 总波次数 |
| `%elitemonsters_nearest_elite%` | 最近精英怪名称 |
| `%elitemonsters_nearest_elite_star%` | 最近精英怪星级 |
| `%elitemonsters_nearest_elite_health%` | 最近精英怪血量 |

---

## API 事件

```java
// com.elitemonsters.plugin.api
EliteSpawnEvent(EliteMobData, LivingEntity, AffixData, int starLevel)
EliteDeathEvent(EliteMobData, LivingEntity)
HordeStartEvent(Location center, int totalWaves)
HordeCompleteEvent(Location center, int totalWaves, Set<UUID> participants)
HordeFailEvent(Location center, int failedWave, int totalWaves)
```

---

## config.yml 新增项 (v1.3.0)

```yaml
# WorldGuard 区域控制
generation:
  worldguard:
    enabled: false
    mode: blacklist        # blacklist / whitelist
    regions: ["spawn"]
    affect-horde: true

# MythicMobs 兼容
mythicmobs:
  enabled: false
  elite-chance: 0.05
  blacklist: []
```

## equipment.yml 结构

```yaml
equipment:
  <id>:
    enabled: true
    min-star: 1 / max-star: 5
    affixes: []           # 空=全词缀, [VAMPIRIC]=专属
    material: IRON_SWORD
    name: "&f%affix% &7的装备"
    lore: ["&7由 &e%star%星 %affix% &7掉落"]
    enchantments:
      SHARPNESS: 2
    chance: 0.15
```

---

## 踩坑记录

1. **编码**: UTF-8无BOM, 用 `[System.Text.UTF8Encoding]::new($false)`
2. **换行**: Windows用\r\n, PS中表示为 `` `r`n ``
3. **apply_patch**: 本项目不可用, 用 PowerShell `.Replace()` 做文件编辑
4. **PS 替换匹配**: old/new 中不能含单引号 `'`, 会被 PS 解析
5. **PS 换行匹配**: `.Replace()` 的匹配串必须用 `\r\n` 而非 `\n`
6. **静默异常**: 全替换为 ErrorLogger
7. **config-version**: **不能注释**, 改默认配置需递增
8. **lang.yml**: 新增key必须 zh_CN+en_US 都加
9. **PAPI仓库**: repo.extendedclip.com (MavenCentral没有)
10. **Git**: 不同用户需 safe.directory 例外
11. **编译**: JAVA_HOME=jdk-25, Maven用 .m2 下的路径
12. **shade+Java25**: maven-shade-plugin 3.6.0 的 ASM 9.6 不支持 Java 25 class version 69; 需要 ASM 9.7.1+。目前无 relocation 可用, 有 relocation 则崩溃。bStats 暂缓。
13. **Material.valueOf**: GIANT_SPAWN_EGG 等不存在, 需 safeMaterial() 兜底
14. **GUI 物品锁定**: 必须拦截 InventoryDragEvent + 取消所有点击; 用 PDC 标记而不用 displayName 匹配
15. **RewardManager**: ConfigurationSection.get() 返回 MemorySection 不是 Map, 必须用 getConfigurationSection().getValues()