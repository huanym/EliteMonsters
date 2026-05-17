# EliteMonsters - 精英怪与尸潮系统

> Purpur 26.1.2 | Java 25 | Paper API 1.21.5-R0.1-SNAPSHOT | Maven 3.9.9
> 构建命令: `mvn clean package -DskipTests` | 输出: `target/EliteMonsters-1.0.0.jar`

---

## 环境配置

| 项目 | 值 |
|------|-----|
| JDK | Eclipse Temurin 25.0.3 (`C:\Users\Huany\jdk-25`) |
| Maven | 3.9.9 (`C:\Users\Huany\.m2\wrapper\dists\apache-maven-3.9.9`) |
| Paper API | `1.21.5-R0.1-SNAPSHOT` |
| 编码 | **UTF-8 无 BOM** (所有文件用 `WriteAllBytes` 写入) |
| 工作目录 | `D:\File\New project\EliteMonsters` |

> ⚠️ **编码铁律**: 所有 `.java` `.yml` `.xml` 文件必须用 `[System.IO.File]::WriteAllBytes` 写入 UTF-8 无 BOM。绝对不能用 `Set-Content -Encoding UTF8`（会加 BOM）。绝对不能用 `Get-Content` 含 `WriteAllText`（中文会损坏）。

---

## 架构思路

```
玩家交互层
└─ EliteCommand     /elite 指令系统 (spawn/reload/info/list/horde/toggle/clear)

业务逻辑层
├─ EliteGenerationListener   精英怪生成监听 + 属性赋值 + 盔甲 + 战斗追踪
├─ HordeManager              尸潮核心 (Session模式, 波次管理, Title+BossBar, 超时失败)
├─ SkillManager              技能系统 (11技能 + 受击反馈)
├─ VisualManager             视觉特效 (HP条/粒子/音效/警报)
└─ AffixManager              词缀加载 + 随机

基础设施层
├─ ConfigManager             配置加载 + 动态概率计算
├─ LangManager               国际化系统 (zh_CN/en_US, 渐变色解析)
└─ GradientUtil              Adventure Component 渐变色工具 (<g:#RRGGBB:#RRGGBB>text</g>)
```

### 核心设计原则
- **消息全部走 Adventure Component**: `LangManager.getComponent()` 返回 `net.kyori.adventure.text.Component`, Paper 1.21.5 原生支持
- **渐变色语法**: `<g:#FF4444:#FFAA00>尸潮</g>` 用于 lang.yml, `GradientUtil.parse()` 解析
- **六进制颜色**: `&#RRGGBB` 格式在 lang.yml 中直接写, Paper 自带 LegacyComponentSerializer 支持
- **尸潮 Session 模式**: 内部类 `HordeSession` 管理单次尸潮生命周期, `registerParticipant()` 追踪参战玩家
- **奖励只给参战者**: `EliteGenerationListener.onHordeCombat` 自动登记战斗参与

---

## 项目结构

```
EliteMonsters/
├── pom.xml
├── PROJECT.md
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw.cmd
└── src/main/
     ├── resources/
     │   ├── plugin.yml
     │   ├── config.yml
     │   └── lang.yml
     └── java/com/elitemonsters/plugin/
          ├── EliteMonstersPlugin.java
          ├── config/
          │   ├── ConfigManager.java
          │   └── LangManager.java
          ├── affix/
          │   ├── AffixData.java
          │   └── AffixManager.java
          ├── generation/
          │   ├── EliteMobData.java
          │   └── EliteGenerationListener.java
          ├── skill/
          │   └── SkillManager.java
          ├── visual/
          │   ├── VisualManager.java
          │   ├── ParticleManager.java
          │   └── GradientUtil.java
          ├── horde/
          │   ├── HordeManager.java
          │   └── HordeNightListener.java
          └── command/
               └── EliteCommand.java
```

---

## 已完成功能

### 精英怪系统 ✅
| 功能 | 关键文件 | 说明 |
|------|----------|------|
| 自然生成 (5% 可配置) | `EliteGenerationListener.onCreatureSpawn` | 高度/群系/时间/难度 条件乘数 |
| 每区块上限 (`max-per-chunk: 3`) | `EliteGenerationListener.onCreatureSpawn` | 尸潮期间自动绕过 |
| 指令生成 `/elite spawn` | `EliteCommand.handleSpawn` | 支持词缀/等级/坐标 |
| 黑白名单 | `config.yml generation.whitelist/blacklist` | 白名单优先 |
| 11 词缀 + 属性倍率 | `AffixManager` + `config.yml affixes` | 狂战士/疾风剑豪/吸血伯爵/炎魔/凋零之王/爆破鬼才/不灭之盾/暗影刺客/不死之身/亡灵术士/锁魂之链 |
| 怪物名字 lang 可定制 | `lang.yml name-format` + `mob-names` | `{stars}{affix_name}{mob_name}` 模板 |
| 怪物名字渐变色 | `entity.customName(GradientUtil.parse(...))` | Adventure Component API |
| 1-5 星级系统 + 盔甲 | `EliteGenerationListener.equipArmor` | 皮革→链甲→铁→钻石→下界合金 |

### 技能系统 ✅
| 技能 | 激活方式 | 受击反馈 |
|------|----------|----------|
| FRENZY_ENRAGE | HP<50% 自动激活 | 击飞+减速+横扫粒子 |
| SWIFT_DASH | 闪现到背后 (8s CD) | 30%失明+云雾粒子 |
| VAMPIRIC_LIFESTEAL | 攻击时触发 | 吸血30%+虚弱+爱心粒子 |
| FLAMING_AURA | 范围持续燃烧 | 点燃3秒+火焰/岩浆粒子 |
| WITHERING_AURA | 范围持续凋零 | 凋零II+减速+烟雾/灵魂火 |
| BOMBARDING_THROW | 投掷TNT (6s CD) | 💥爆炸+击飞+溅射+波状粒子 |
| SHIELDED_BARRIER | 护盾 (15s CD) | 击退+铁块碎裂粒子 |
| INVISIBLE_CLOAK | 隐身 (12s CD) | 40%失明+20%黑暗+传送门粒子 |
| REGENERATING_HEAL | 回复 (10s CD) | 中毒+自身回复+史莱姆粒子 |
| SUMMONING_MINIONS | 召唤小弟 (20s CD) | 反胃+虚弱+女巫粒子 |
| CHAINING_PULL | 拉拽玩家 (10s CD) | 减速III+虚弱II+闪电+电火花 |

### 尸潮系统 ✅
| 功能 | 关键文件 | 说明 |
|------|----------|------|
| 指令开启/停止/查看 | `EliteCommand.handleHorde` | `/elite horde start/stop/info` |
| 自动间隔尸潮 | `HordeManager.startAutoTask` | `auto-interval: 7200` (2小时) |
| 夜晚随机尸潮 | `HordeManager.checkRandomHorde` | `random-chance: 0.15` |
| 中心点可配置 | `horde.center-mode` | `random_player` / `world_spawn` |
| 生成半径可配置 | `horde.spawn-radius` / `spawn-min-radius` | 怪物不会贴脸刷 |
| 安全生成位置检测 | `getSafeSpawnLocation` | 10次重试, 避开水/岩浆/墙壁 |
| 每波怪物数量/类型/精英率 | `config.yml horde.waves.N` | 5波递增难度 |
| Title + BossBar 波次提示 | `HordeSession` | 屏幕中央大标题 + BossBar进度条 |
| BossBar 波间倒计时 | `startCountdown(10)` | 10秒黄色倒计时条 |
| 怪物高亮标记 | `horde.glow-mobs: true` | 可配置关闭 |
| 波次超时强制失败 | `wave-timeout: 300` | 0=不限时, 超时广播+Title+清除怪物 |
| 奖励只给参战者 | `registerParticipant` + `onHordeCombat` | 打/被打才算参战 |
| 完成消息显示玩家名 | `%players%` 变量 | "玩家A, 玩家B 成功抵御了 5 波尸潮!" |
| 奖励波次清除后发放 | `giveWaveRewards` 在 waveTask 内触发 | 打完才给, 不是开始时 |
| 爆炸不破坏地形 | `EntityExplodeEvent.blockList().clear()` | TNT标记 `elitemonsters-tnt` |

### 指令系统 ✅
| 命令 | 权限 | 说明 |
|------|------|------|
| `/elite spawn <mob> [affix] [level]` | `elitemonsters.command.spawn` | 生成精英怪 |
| `/elite reload` | `elitemonsters.command.reload` | 重载配置+语言 |
| `/elite info` | — | 查看词缀列表 |
| `/elite list` | — | 附近精英怪 |
| `/elite horde start [mob]` | `elitemonsters.command.horde` | 开启尸潮 |
| `/elite horde stop` | `elitemonsters.command.horde` | 停止尸潮 |
| `/elite horde info` | — | 尸潮状态 |
| `/elite clear [范围|chunk|world|type]` | `elitemonsters.command.clear` | 清除精英怪 |
| `/elite toggle lightning` | `elitemonsters.command.reload` | 开关闪电 (保存到 config) |
| `/elite toggle alert` | `elitemonsters.command.reload` | 开关提醒 (保存到 config) |

### 视觉效果 ✅
| 功能 | 说明 |
|------|------|
| HP血量条 | 怪物头顶显示, 基础名+血量, 不会越叠越长 |
| 生成特效 | 螺旋+爆炸粒子 + 闪电 (可关) |
| 死亡特效 | 球体+圆形粒子 + 词缀特效音效 |
| 范围警报 | 生成时通知范围内玩家 (可关) |
| 词缀粒子循环 | 每秒循环词缀对应粒子 |

---

## 配置文件关键路径

### config.yml
```yaml
locale: zh_CN                          # 语言切换
generation:
  base-chance: 0.05                     # 基础生成概率
  max-per-chunk: 3                      # 每区块上限 (-1=不限)
  conditions: {height, biome, time, difficulty}
spawn-effects: {lightning, global-alert}
star-system: {max-level, star-char, levels.N.*}
affixes: {FRENZY, SWIFT, VAMPIRIC, ...} # 11词缀全中文名
horde:
  center-mode: random_player            # random_player / world_spawn
  glow-mobs: true                       # 尸潮怪物高亮
  spawn-radius: 15.0                    # 最远生成距离
  spawn-min-radius: 8.0                 # 最近生成距离
  waves.N: {mob-count, mob-types, elite-chance, wave-timeout, boss, rewards}
  global-rewards: {enabled, completion-commands}  # 支持 %players% %waves%
```

### lang.yml
- 顶级键: `zh_CN` / `en_US`
- `name-format: "{stars}{affix_name}{mob_name}"` — 怪物名字模板
- `mob-names.ZOMBIE: "僵尸"` — 30+ 生物中英文映射
- 渐变色语法: `<g:#FF4444:#FFAA00>尸潮</g>`
- 六进制颜色: `&#FF5555错误消息`
- 占位符: `{0}`, `{1}`, ...

---

## 待办事项

### 高优先级
- [ ] **/elite reload 重载时恢复所有精英怪状态** — 当前 reload 后已有精英怪不会重新加载数据
- [ ] **ChatColor → Adventure Component 迁移** — EliteCommand 中 `affix.getColor()` 还在用旧格式
- [ ] **NamespacedKey 构造函数** — `new NamespacedKey(plugin, key)` 可能在新版 API 中改变

### 中优先级
- [ ] **CustomModelData** — 自定义材质包支持
- [ ] **LuckPerms 上下文** — 按权限组调整精英怪概率
- [ ] **精英怪掉落表** — 自定义掉落配置
- [ ] **尸潮难度自适应** — 根据在线人数动态调整
- [ ] **center-mode: all_players** — 所有玩家周围分别刷怪

### 低优先级
- [ ] **粒子颜色支持** (DustOptions for REDSTONE particle)
- [ ] **更多粒子图案** (心形/星形/漩涡)
- [ ] **性能优化** (粒子任务统一管理, Entity 缓存)
- [ ] **PlaceholderAPI 支持**

---

## 重要修改记录

| 日期 | 变更 |
|------|------|
| 2026-05-16 | 初始搭建: pom.xml, plugin.yml, 包结构 |
| | ConfigManager + config.yml (11词缀/5星/生成条件) |
| | EliteGenerationListener (自然生成/属性/盔甲/粒子/音效) |
| | SkillManager (11技能) + VisualManager (HP条/粒子/音效/警报) |
| | EliteCommand (spawn/reload/info/list) + 尸潮系统 |
| | BUG修复: BOM → UTF-8 无 BOM, Sound/Particle 枚举名称更新 |
| | BUG修复: 名字变长 (baseDisplayName), CMI 兼容 |
| | 生物中文映射 + 盔甲 + 闪电可配置 |
| | 尸潮奖励 (Vault/物品/XP/指令) + ParticleManager |
| | lang.yml 国际化 + LangManager, 所有消息迁移到 lang.yml |
| | **重构: 怪物名字 lang 可定制** — `name-format` + `mob-names` 移入 lang.yml |
| | **重构: 尸潮 Title+BossBar** — 大标题播报 + 进度条 + 倒计时条 |
| | **重构: RGB渐变色** — GradientUtil + `<g:#>` 语法 + `&#RRGGBB` 六进制 |
| | **重构: 尸潮生成半径可配置** — `spawn-radius` / `spawn-min-radius` |
| | **重构: 受击反馈系统** — 11词缀各自独立受击效果 |
| | **API迁移**: Sound.valueOf → Registry.SOUNDS, OldEnum.name → Keyed.getKey |
| | **BUG: 尸潮重复开启** — startHorde返回false+指令检测 |
| | **BUG: 渐变色失效** — LegacyComponentSerializer character('&').hexColors() |
| | **BUG: 开关不保存** — toggle 后 saveConfig() |
| | **BUG: 模型名渐变色** — entity.setCustomName → entity.customName(Component) |
| | **词缀中文化** — 11词缀趣味中文名 (狂战士/炎魔/...) |
| | **BUG: 爆炸不破坏地形** — EntityExplodeEvent.blockList().clear() |
| | **区块精英上限** — generation.max-per-chunk, 尸潮期间绕过 |
| | **新指令: /elite clear** — 范围/区块/世界/类型组合 |
| | **BUG: 奖励时机** — 波次清除后发放, 非开始时 |
| | **参战奖励** — 只有打/被打的玩家获得奖励 |
| | **尸潮生成位置修复** — getSafeSpawnLocation 避开水/岩浆/墙壁 |
| | **尸潮怪物高亮** — glow-mobs 可配置 + HORDE_META_KEY 标记 |
| | **尸潮超时失败** — wave-timeout 配置 + failHorde() |
| | **center-mode 可配置** — random_player / world_spawn |
| | **完成消息显示参战玩家** — %players% 变量 |
| | 编译通过, 0 warning |

---

## 部署步骤

1. 编译: `cd D:\File\New project\EliteMonsters && mvn clean package -DskipTests`
2. 将 `target\EliteMonsters-1.0.0.jar` 复制到服务器 `plugins/`
3. **首次部署**: 删除旧 `plugins/EliteMonsters/config.yml` (如有)
4. 重启服务器
5. 编辑 `plugins/EliteMonsters/lang.yml` 自定义语言
6. `/elite reload` 热重载

---

## 下次会话快速启动

```powershell
# 设置环境
$env:JAVA_HOME = "$env:USERPROFILE\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 进入项目
cd "D:\File\New project\EliteMonsters"

# 编译
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd" clean package -DskipTests

# 输出
# target\EliteMonsters-1.0.0.jar
```