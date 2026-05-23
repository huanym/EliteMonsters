# EliteMonsters - 精英怪与尸潮系统

> Purpur 26.1.2 | Java 25 | Paper API 1.21.5-R0.1-SNAPSHOT | Maven 3.9.9
> 版本: **v1.2.0** | 构建: `mvn clean package -DskipTests` | 输出: `target/EliteMonsters-1.2.0.jar`
> GitHub: https://github.com/huanym/EliteMonsters
> Git: `D:\Git\bin\git.exe` | 工作目录: `E:\服务端\插件\EliteMonsters`

---

## 环境配置

| 项目 | 值 |
|------|-----|
| JDK | Eclipse Temurin 25.0.3 (`%USERPROFILE%\jdk-25`) |
| Maven | 3.9.9 (`%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9`) |
| Paper API | `1.21.5-R0.1-SNAPSHOT` |
| PlaceholderAPI | `2.11.6` (provided) |
| Vault | `1.7.1` (provided) |
| Git | PortableGit 2.49.0 (`D:\Git\bin`) |
| 编码 | **UTF-8 无 BOM** |

> 编码铁律: 所有 .java .yml .xml 文件必须 UTF-8 无 BOM。

---

## 关键决策

| 决策 | 说明 |
|------|------|
| Adventure Component 全消息 | LangManager.getComponent() -> net.kyori.adventure.text.Component |
| 渐变色语法 <g:#:#>text</g> | GradientUtil.parse() 解析, lang.yml 中使用 |
| 六进制色 &#RRGGBB | Paper LegacyComponentSerializer, lang.yml 直接写 |
| 尸潮 Session 模式 | 内部类 HordeSession, registerParticipant() 追踪参战 |
| 奖励只给参战者 | onHordeCombat 自动登记, 只发放给参战玩家 |
| 配置反射同步 | ConfigManager.load() 反射替换 JavaPlugin.config |
| 配置自动迁移 | 检测 config-version, 旧版备份到 backup/ |
| 精英怪卸载恢复 | EliteMobData 存原始属性, onDisable() -> revertAllElites() |
| 独立奖励系统 | rewards.yml 6 种类型, 概率/CustomModelData/头颅纹理 |
| 独立掉落表 | loot.yml 按生物类型/词缀/星级分层 |
| 全局属性倍率 | global-attribute-scale (默认 0.5) 统一缩放 |
| 动态难度 | dynamic-difficulty 根据附近玩家装备自适应星级 |
| 错误日志 | ErrorLogger 文件持久化 + 内存缓存 + /elite test errors |
| API 事件 | 5 个自定义事件, 供其他插件监听 |
| PlaceholderAPI | 7 个变量, ElitePlaceholders 扩展 |
| 粒子密度 | particle-density low/medium/high 三档 |
| 粒子 task 管理 | Map<UUID,BukkitTask> 集中管理, 防泄漏 |
| EliteMobData 清理 | 每 10 秒定时清理无效实体引用 |
| Debug 模式 | config.yml debug:true, ErrorLogger + ConfigManager.debugLog() |

---

## 架构思路

```
玩家交互层
  EliteCommand     /elite 指令 (spawn/reload/info/list/horde/toggle/test/clear)

业务逻辑层
  EliteGenerationListener   精英怪生成 + 属性/盔甲/战斗/卸载 + 掉落触发 + 世界黑名单
  HordeManager              尸潮核心 (Session/波次/Title+BossBar/超时/奖励/事件)
  SkillManager              11 技能 + 受击反馈 (tick 随机偏移防扎堆)
  VisualManager             视觉 (HP条/粒子密度/音效/警报, distanceSquared 优化)
  AffixManager              词缀加载/随机/验证
  RewardManager             6 种奖励 (ITEM/EXP/VAULT/COMMAND/PERMISSION/GROUP)
  LootManager               掉落表 (loot.yml 分层匹配)

基础设施层
  ConfigManager             配置加载 + 动态概率 + 反射同步 + 高频值缓存 + debugLog
  LangManager               国际化 (zh_CN/en_US + 渐变色)
  GradientUtil              Adventure Component 渐变
  ParticleManager           粒子几何图形 (圆/螺旋/球/爆发/线)
  ErrorLogger               错误日志 (errors.log + 内存 50 条 + 堆栈跟踪)
  ElitePlaceholders         PlaceholderAPI 扩展 (7 变量)

API 事件层
  EliteSpawnEvent / EliteDeathEvent
  HordeStartEvent / HordeCompleteEvent / HordeFailEvent
```

---

## 项目结构

```
EliteMonsters/
  pom.xml                    # Maven v1.2.0, Paper 1.21.5, Vault, PlaceholderAPI
  PROJECT.md                 # 本文档
  README.md                  # GitHub 首页
  TESTING.md                 # 测试与调试指南
  .gitignore
  .mvn/wrapper/maven-wrapper.properties
  mvnw.cmd
  src/main/
    resources/
      plugin.yml             # softdepend: Vault, LuckPerms, PlaceholderAPI
      config.yml             # v4: debug/dynamic-difficulty/world-blacklist/particle-density
      lang.yml               # v1: zh_CN/en_US (含 test 命令消息)
      rewards.yml            # v1: 6 种奖励类型
      loot.yml               # v1: 掉落表 (default/star_5/summoning/boss_mobs)
    java/com/elitemonsters/plugin/
      EliteMonstersPlugin.java    # 入口 (版本检测/迁移/PlugMan/PAPI注册/ErrorLogger)
      ErrorLogger.java            # 错误日志 (文件+内存双写, 50条缓存)
      ElitePlaceholders.java      # PlaceholderAPI 扩展 (7变量)
      LootManager.java            # 掉落表 (分层匹配, 首个命中即返回)
      config/
        ConfigManager.java        # 配置加载 (反射同步/高频缓存/debugLog/动态难度/世界黑名单)
        LangManager.java
      affix/
        AffixData.java            # 词缀数据 (含 ParticleConfig record)
        AffixManager.java         # 词缀加载/验证 (ErrorLogger 接入)
      api/                        # API 事件包
        EliteSpawnEvent.java
        EliteDeathEvent.java
        HordeStartEvent.java
        HordeCompleteEvent.java
        HordeFailEvent.java
      command/
        EliteCommand.java         # 指令系统 (含 /elite test 8子命令 + Tab补全)
      generation/
        EliteGenerationListener.java # 生成监听 (task管理/清理线程/事件/掉落/动态难度)
        EliteMobData.java         # 精英怪数据 (含原始属性/活跃技能)
      horde/
        HordeManager.java         # 尸潮核心 (Session/事件/StringBuilder优化)
        HordeNightListener.java   # 夜晚检测
      reward/
        RewardData.java           # 奖励抽象基类 + parse工厂
        RewardManager.java
        ItemRewardData.java       # 物品 (CustomModelData/头颅纹理)
        ExpRewardData.java
        VaultRewardData.java
        CommandRewardData.java
        PermissionRewardData.java
        GroupRewardData.java      # 组合奖励 (递归嵌套)
      skill/
        SkillManager.java         # 11 技能 (tick 40L + 随机偏移)
      visual/
        VisualManager.java        # 视觉 (粒子密度/distanceSquared/BukkitTask返回)
        ParticleManager.java      # 粒子图形
        GradientUtil.java         # Adventure 渐变
```

---

## 已完成功能

### 精英怪系统
- [x] CreatureSpawnEvent 自然生成 (排除 SLIME/MAGMA_CUBE/CUSTOM/SPAWNER_EGG/COMMAND)
- [x] 动态概率: base-chance x 高度 x 群系 x 时间 x 难度
- [x] 每区块上限 max-per-chunk
- [x] 白名单/黑名单模式
- [x] 世界黑名单 world-blacklist
- [x] 全局属性倍率 global-attribute-scale
- [x] 动态难度 dynamic-difficulty (根据附近玩家装备自适应星级)
- [x] 精英怪卸载恢复 EliteMobData + revertAllElites()
- [x] EliteMobData 清理线程 (每 10s 清除无效实体)

### 词缀系统
- [x] 11 种词缀: 狂战士/疾风剑豪/吸血伯爵/炎魔/凋零之王/爆破鬼才/不灭之盾/暗影刺客/不死之身/亡灵术士/锁魂之链
- [x] 各词缀独立属性/技能/粒子/音效配置
- [x] 词缀中文化趣味名称
- [x] ErrorLogger 接入 (无效粒子类型自动记录)

### 星级系统
- [x] 1~5 星, 每级独立 attribute-multiplier/skill-count/armor-tier
- [x] 动态难度自适应星级

### 技能系统
- [x] 11 种技能: 狂暴/突刺/吸血/烈焰/冰冻/爆炸/护盾/隐身/再生/召唤/锁链
- [x] 受击反馈系统 (11 词缀各自独立受击效果)
- [x] 技能 CD 系统 (每精英怪 UUID 独立冷却)
- [x] tick 减半 40L + 随机偏移 (防扎堆)

### 尸潮系统
- [x] Session 模式 + 多波次 + BossBar + Title 播报
- [x] 自动/夜晚随机/手动 三种触发
- [x] 超时失败 failHorde()
- [x] 参战奖励 (仅战斗参与者)
- [x] 安全生成 getSafeSpawnLocation()
- [x] 尸潮怪物高亮 glow-mobs
- [x] center-mode: random_player/world_spawn
- [x] stream->for-loop + StringBuilder 优化

### 奖励与掉落
- [x] 6 种奖励: ITEM/EXP/VAULT/COMMAND/PERMISSION/GROUP
- [x] 独立掉落表 loot.yml (按生物/词缀/星级分层)
- [x] CustomModelData + PLAYER_HEAD 头颅纹理
+ [x] 组合奖励 GroupRewardData 递归嵌套

### 视觉与性能
- [x] RGB 渐变色 <g:#:#>text</g>
- [x] 粒子密度 low/medium/high
- [x] distanceSquared 替代 distance()
- [x] 粒子 task 集中管理 Map<UUID,BukkitTask>
- [x] ConfigManager 高频值缓存 (6字段)

### 可观测性
- [x] Debug 模式 (控制台 + ErrorLogger)
- [x] errors.log 文件持久化
- [x] /elite test 命令套件 (info/spawn/horde/loot/reward/stress/errors/cleanup)
- [x] TESTING.md 测试调试指南

### API 与集成
- [x] 5 个 API 事件
- [x] PlaceholderAPI 7 变量
- [x] plugin.yml softdepend: Vault/LuckPerms/PlaceholderAPI

---

## 待办

### 高优先级
- [ ] MythicMobs 兼容 — 让 MythicMobs 生成的怪物也能成为精英怪
- [ ] 精英怪 AI 增强 — 巡逻/仇恨/逃跑
- [ ] PlaceholderAPI 变量扩展 — 更多玩家侧变量

### 中优先级
- [ ] 副本/Arena 系统 — Boss 竞技场
- [ ] NPC 系统 — NPC 交互/任务
- [ ] GUI 管理面板 — /elite gui 箱子菜单
- [ ] Web 控制面板 — 类似 EliteMobs webapp
- [ ] bStats 统计 — 匿名使用统计

### 低优先级
- [ ] 粒子颜色支持 (DustOptions for REDSTONE)
- [ ] 更多粒子图案 (心形/星形/漩涡)
- [ ] GitHub Actions CI/CD

---

## 重要修改记录

| 日期 | 变更 |
|------|------|
| 2026-05-16 | 初始搭建: pom.xml, plugin.yml, 包结构 |
| | ConfigManager + config.yml (11词缀/5星) |
| | EliteGenerationListener + SkillManager + VisualManager |
| | EliteCommand + 尸潮系统 |
| | 多轮 BUG 修复 (BOM/Sound/Particle枚举/名字/尸潮重复/渐变色/爆炸等) |
| 2026-05-17 | 独立奖励系统 rewards.yml (6类型) |
| | 配置自动迁移 / 卸载恢复 / 热重载修复 |
| | debug 模式 / Git 仓库 / README / PROJECT |
| | v1.1.0 发布 (编译通过 0 error) |
| 2026-05-22-23 | **v1.2.0 大规模迭代** |
| | API 事件系统 (EliteSpawn/Death, HordeStart/Complete/Fail) |
| | PlaceholderAPI 集成 (7 变量) |
| | 独立掉落表 loot.yml + LootManager |
| | 动态难度 dynamic-difficulty (根据玩家装备自适应) |
| | 世界黑名单 world-blacklist |
| | 错误日志 ErrorLogger (errors.log + 内存缓存) |
| | /elite test 调试命令套件 (8子命令) |
| | 粒子密度 particle-density (low/medium/high) |
| | 性能优化: distanceSquared/config缓存/stream->loop/tick减半 |
| | 粒子 task 集中管理 + EliteMobData 定时清理 |
| | TESTING.md 测试调试文档 |
| | pom.xml 更新 + pom.xml 项目信息刷新 + 编码修复 |
| | v1.2.0 编译通过 (30源文件 0 error) |

---

## 部署步骤

1. 编译: cd "E:\服务端\插件\EliteMonsters" && mvn clean package -DskipTests
2. 将 target\EliteMonsters-1.2.0.jar 复制到服务器 plugins/
3. 若已有旧版: 自动检测 config-version, 备份旧配置到 backup/ 并替换新模板
4. 重启服务器
5. 编辑 plugins/EliteMonsters/config.yml / rewards.yml / loot.yml
6. /elite reload 热重载

---

## 下次会话快速启动

```powershell
# Git PATH
$env:Path = "D:\Git\bin;$env:Path"

# JDK
$env:JAVA_HOME = "$env:USERPROFILE\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 项目
cd "E:\服务端\插件\EliteMonsters"

# 拉取
git pull

# 编译
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd" clean package -DskipTests

# 输出: target\EliteMonsters-1.2.0.jar
```