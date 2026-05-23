# EliteMonsters - 开发会话交接文档

> 最后更新: 2026-05-23 | 版本: v1.2.0 | 编译: BUILD SUCCESS (30文件 0错误)
> 工作目录: E:\服务端\插件\EliteMonsters | Git: D:\Git\bin\git.exe
> GitHub: https://github.com/huanym/EliteMonsters

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
# 输出: target\EliteMonsters-1.2.0.jar (123 KB)
```

---

## 架构全景

### 指令层
- EliteCommand (/elite) — spawn/reload/info/list/horde/toggle/clear + test 8个子命令

### 业务层
- EliteGenerationListener — 生成+属性+战斗+掉落+清理线程
- HordeManager (HordeSession) — 波次+BossBar+Title+事件
- SkillManager — 11技能+CD (40tick+随机偏移)
- RewardManager + LootManager — 双轨奖励 (rewards.yml + loot.yml)
- VisualManager — 粒子密度+distanceSquared优化

### 基础设施层
- ConfigManager — 反射同步+6字段缓存+世界黑名单+动态难度
- LangManager — zh_CN/en_US+渐变色
- ErrorLogger — errors.log文件+内存50条
- ElitePlaceholders — PAPI 7变量
- ParticleManager / GradientUtil

### API层
- 5事件: EliteSpawn/Death, HordeStart/Complete/Fail

---

## 关键决策

| # | 决策 | 位置 |
|---|------|------|
| 1 | Adventure Component全消息 | LangManager |
| 2 | 渐变色 <g:#:#> 语法 | GradientUtil |
| 3 | 六进制色 &#RRGGBB | LegacyComponentSerializer |
| 4 | 尸潮Session内部类 | HordeManager |
| 5 | 奖励仅参战者 | onHordeCombat |
| 6 | 配置反射同步 | ConfigManager.load() |
| 7 | 配置自动迁移 | migrateConfigs() |
| 8 | 精英怪卸载恢复 | revertAllElites() |
| 9 | 全局属性倍率0.5 | global-attribute-scale |
| 10 | 粒子task集中管理 | Map<UUID,BukkitTask> |
| 11 | 10s清理无效实体 | startCleanupTask() |
| 12 | distanceSquared优化 | VisualManager |
| 13 | ConfigManager缓存 | starChar等6字段 |
| 14 | Stream->for-loop | HordeManager |
| 15 | 技能tick减半+偏移 | SkillManager 40L |
| 16 | 动态难度配方 | 护甲4/3/2/1+附魔x0.5 |
| 17 | 错误日志双写 | errors.log+内存 |
| 18 | API事件5个 | api/包 |
| 19 | PAPI 7变量 | ElitePlaceholders |
| 20 | 掉落表分层匹配 | LootManager |

---

## 文件清单 (34文件)

```
EliteMonsters/
  pom.xml                         # Maven v1.2.0
  PROJECT.md / README.md / TESTING.md / SESSION.md
  src/main/resources/
    plugin.yml                    # softdepend: Vault/LuckPerms/PlaceholderAPI
    config.yml                    # v4
    lang.yml                      # zh_CN/en_US 完整双语
    rewards.yml                   # 6种奖励
    loot.yml                      # 掉落表
  src/main/java/com/elitemonsters/plugin/
    EliteMonstersPlugin.java      # 入口
    ErrorLogger.java              # 错误日志
    ElitePlaceholders.java        # PAPI
    LootManager.java              # 掉落管理
    api/                          # 5个API事件
    affix/                        # AffixData + AffixManager
    command/EliteCommand.java     # /elite全套
    config/                       # ConfigManager + LangManager
    generation/                   # EliteGenerationListener + EliteMobData
    horde/                        # HordeManager + HordeNightListener
    reward/                       # 7文件 (6类型+Manager)
    skill/SkillManager.java       # 11技能
    visual/                       # VisualManager + ParticleManager + GradientUtil
```

---

## 已完成功能

### 精英怪
- [x] 自然生成(动态概率/区块上限/黑白名单/世界黑名单)
- [x] 全局属性倍率 + 动态难度(装备自适应)
- [x] 卸载恢复 + 10s清理线程

### 词缀+星级
- [x] 11词缀 + 5星(独立属性/技能/护甲)

### 技能
- [x] 11技能 + 受击反馈 + CD + tick 40L+偏移

### 尸潮
- [x] Session模式/波次/BossBar/Title/超时/参战奖励
- [x] 自动/夜晚随机/手动触发

### 奖励+掉落
- [x] 6种奖励(CustomModelData/头颅/组合)
- [x] 掉落表(分层匹配)

### 性能(v1.2.0)
- [x] 粒子密度/ distanceSquared / Config缓存
- [x] task集中管理 / tick减半 / stream->loop

### 可观测性
- [x] ErrorLogger / debug模式 / /elite test 8子命令 / TESTING.md

### API+集成
- [x] 5事件 / PAPI 7变量 / 3个softdepend

---

## 待办

### 高
- [ ] MythicMobs兼容
- [ ] 精英怪AI增强(巡逻/仇恨/逃跑)
- [ ] PAPI变量扩展

### 中
- [ ] 副本/Arena系统
- [ ] NPC系统
- [ ] GUI管理面板
- [ ] bStats统计

### 低
- [ ] 粒子颜色(DustOptions)
- [ ] 更多粒子图案
- [ ] GitHub Actions CI/CD

---

## 修改记录

| 日期 | Commit | 变更 |
|------|--------|------|
| 05-16 | 1ea08b9 | 骨架 |
| 05-16 | ad33ce7 | 核心+Config+Lang |
| 05-16 | e4c6116 | 精英/词缀/技能/尸潮/指令 |
| 05-17 | fb020ed | 奖励系统 |
| 05-17 | 8d3f720 | v1.1.0 |
| 05-17 | d4fa1fa~dc38de3 | 文档+BugFix |
| 05-22 | c2fd286 | **v1.2.0** |
| 05-23 | c59a927 | 文档+国际化+版本号 |

---

## 踩坑记录

1. 编码: UTF-8无BOM, 用[IO.File]::WriteAllText
2. 换行: Windows用\r\n, PS用[char]13+[char]10
3. apply_patch: 本项目不可用, 用.Replace()
4. PS转义: pipe字符在shell中会被解析
5. 静默异常: 已全替换为ErrorLogger
6. config-version: 改默认配置需递增
7. lang.yml: 新增key必须zh_CN+en_US都加
8. PAPI仓库: repo.extendedclip.com (MavenCentral没有)
9. Git: 不同用户需safe.directory例外
10. 编译: JAVA_HOME=jdk-25, Maven用.m2下的路径

---

## PAPI变量

| 变量 | 说明 |
|------|------|
| %elitemonsters_total_elites% | 全服精英怪数 |
| %elitemonsters_horde_active% | 尸潮进行中 |
| %elitemonsters_horde_wave% | 当前波次 |
| %elitemonsters_horde_total_waves% | 总波次 |
| %elitemonsters_nearest_elite% | 最近精英怪名 |
| %elitemonsters_nearest_elite_star% | 最近精英怪星级 |
| %elitemonsters_nearest_elite_health% | 最近精英怪血量 |

---

## API事件

```java
// com.elitemonsters.plugin.api
EliteSpawnEvent(EliteMobData, LivingEntity, AffixData, int starLevel)
EliteDeathEvent(EliteMobData, LivingEntity)
HordeStartEvent(Location center, int totalWaves)
HordeCompleteEvent(Location center, int totalWaves, Set<UUID> participants)
HordeFailEvent(Location center, int failedWave, int totalWaves)
```

---

## /elite test 命令

| 命令 | 作用 |
|------|------|
| test info | 运行时状态(精英/尸潮/内存/线程) |
| test spawn 生物 [词缀] [星级] | 精确生成 |
| test horde | 立即尸潮 |
| test loot 生物 [词缀] [星级] | 验证掉落 |
| test reward id | 测试奖励 |
| test stress [数量] | 压力测试(<=50) |
| test errors | 错误历史(50条) |
| test cleanup | 强制清理 |

---

## config.yml 关键项

```yaml
debug: true/false
locale: zh_CN/en_US
generation.base-chance: 0.05
generation.global-attribute-scale: 0.5
generation.dynamic-difficulty: false
generation.world-blacklist: [world_nether]
generation.max-per-chunk: 3
visual.particle-density: high   # low/medium/high
horde.enabled: true
horde.auto-interval: 7200
horde.center-mode: random_player
```

---

## Git记录

```
c59a927 docs: v1.2.0, i18n, bump
c2fd286 v1.2.0: events+PAPI+loot+dynamic+errors+perf+test
dc38de3 docs: PROJECT rewrite
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