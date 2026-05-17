# EliteMonsters - 精英怪与尸潮系统

> Purpur 26.1.2 | Java 25 | Paper API 1.21.5-R0.1-SNAPSHOT | Maven 3.9.9
> 版本: **v1.1.0** | 构建: `mvn clean package -DskipTests` | 输出: `target/EliteMonsters-1.1.0.jar`
> GitHub: https://github.com/huanym/EliteMonsters
> Git: `D:\Git\bin\git.exe` | 工作目录: `D:\File\New project\EliteMonsters`

---

## 环境配置

| 项目 | 值 |
|------|-----|
| JDK | Eclipse Temurin 25.0.3 (`%USERPROFILE%\jdk-25`) |
| Maven | 3.9.9 (`%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9`) |
| Paper API | `1.21.5-R0.1-SNAPSHOT` |
| Git | PortableGit 2.49.0 (`D:\Git\bin`) |
| 编码 | **UTF-8 无 BOM** (所有文件用 WriteAllBytes 写入) |

> 编码铁律: 所有 .java .yml .xml 文件必须用 [System.IO.File]::WriteAllBytes 写入 UTF-8 无 BOM。不能用 Set-Content -Encoding UTF8 (会加 BOM)，不能用 Get-Content + WriteAllText (中文会损坏)。

---

## 关键决策

| 决策 | 说明 |
|------|------|
| Adventure Component 全消息 | 所有消息走 LangManager.getComponent() -> net.kyori.adventure.text.Component |
| 渐变色语法 <g:#:#>text</g> | GradientUtil.parse() 解析, 用于 lang.yml |
| 六进制色 &#RRGGBB | Paper 自带 LegacyComponentSerializer 支持, lang.yml 直接写 |
| 尸潮 Session 模式 | 内部类 HordeSession 管理生命周期, registerParticipant() 追踪参战 |
| 奖励只给参战者 | onHordeCombat 自动登记战斗参与, 打/被打都计入 |
| 配置反射同步 | ConfigManager.load() 用反射替换 JavaPlugin.config, plugin.getConfig() 热重载生效 |
| 配置自动迁移 | 启动时检测 config-version, 旧版备份到 backup/ 并替换新模板 |
| 精英怪卸载恢复 | EliteMobData 存储原始属性, onDisable() 调用 revertAllElites() 重置 |
| 独立奖励系统 | rewards.yml 独立配置 6 种奖励类型, 支持概率/CustomModelData/头颅纹理 |
| 全局属性倍率 | generation.global-attribute-scale (默认 0.5) 统一缩放精英怪强度 |
| Debug 模式 | config.yml 设 debug: true 输出详细日志, ConfigManager.debugLog() 统一入口 |

---

## 架构思路

```
玩家交互层
  EliteCommand     /elite 指令系统 (spawn/reload/info/list/horde/toggle/clear)

业务逻辑层
  EliteGenerationListener   精英怪生成监听 + 属性赋值 + 盔甲 + 战斗追踪 + 卸载恢复
  HordeManager              尸潮核心 (Session模式/波次管理/Title+BossBar/超时失败/奖励)
  SkillManager              技能系统 (11技能 + 受击反馈)
  VisualManager             视觉特效 (HP条/粒子/音效/警报)
  AffixManager              词缀加载 + 随机
  RewardManager             奖励系统 (6类型: ITEM/EXP/VAULT/COMMAND/PERMISSION/GROUP)

基础设施层
  ConfigManager             配置加载 + 动态概率计算 + 反射同步 getConfig + debug日志
  LangManager               国际化系统 (zh_CN/en_US, 渐变色解析)
  GradientUtil              Adventure Component 渐变色工具
```

---

## 项目结构

```
EliteMonsters/
  pom.xml                         # Maven 1.1.0, Paper 1.21.5, Vault
  PROJECT.md                      # 本文档
  README.md                       # GitHub 首页
  .gitignore
  .mvn/wrapper/maven-wrapper.properties
  mvnw.cmd
  src/main/
    resources/
      plugin.yml
      config.yml             # 主配置 (v4: debug/global-attribute-scale/rewards引用)
      lang.yml               # 语言文件 (v1: zh_CN/en_US)
      rewards.yml            # 独立奖励配置 (v1: 5波次默认 + 7示例)
    java/com/elitemonsters/plugin/
      EliteMonstersPlugin.java    # 入口 (版本检测+迁移+PlugMan兼容)
      config/
        ConfigManager.java        # 配置加载 (反射同步/getConfig修复/debugLog)
        LangManager.java
      affix/
        AffixData.java
        AffixManager.java
      generation/
        EliteMobData.java         # 含原始属性字段 (卸载恢复用)
        EliteGenerationListener.java  # 含 revertElite/revertAllElites
      skill/
        SkillManager.java
      visual/
        VisualManager.java
        ParticleManager.java
        GradientUtil.java
      horde/
        HordeManager.java         # completeHorde/failHorde 清除 activeHorde
        HordeNightListener.java
      reward/                     # v1.1 新增
        RewardData.java           # 抽象基类 + 工厂方法 parse()
        ItemRewardData.java       # 物品 (名称/Lore/附魔/CustomModelData/头颅)
        ExpRewardData.java        # 经验/等级 (范围随机)
        VaultRewardData.java      # 游戏币 (Vault, 范围随机)
        CommandRewardData.java    # 控制台指令 ({player}/{x}/{y}/{z})
        PermissionRewardData.java # LuckPerms 权限 (限时/永久)
        GroupRewardData.java      # 组合奖励 (递归嵌套)
        RewardManager.java        # 加载/解析/发放 (debug日志)
      command/
        EliteCommand.java         # horde stop 无尸潮时反馈
```

---

## 已完成功能

### 精英怪系统
- 自然生成 (5% 可配置) — 高度/群系/时间/难度 条件乘数
- 每区块上限 (max-per-chunk: 3) — 尸潮期间自动绕过
- 指令生成 /elite spawn — 支持词缀/等级/坐标
- 黑白名单 — 白名单优先
- 11 词缀 + 属性倍率 — 狂战士/疾风剑豪/吸血伯爵/炎魔/凋零之王/爆破鬼才/不灭之盾/暗影刺客/不死之身/亡灵术士/锁魂之链
- 怪物名字 lang 可定制 — {stars}{affix_name}{mob_name} 模板
- 1-5 星级系统 + 盔甲 — 皮革->链甲->铁->钻石->下界合金
- 全局强度倍率 — generation.global-attribute-scale (默认 0.5)
- 卸载恢复原状 — onDisable 时重置属性/卸甲/清名

### 技能系统
- FRENZY_ENRAGE: HP<50% 自动激活, 击飞+减速+横扫粒子
- SWIFT_DASH: 闪现到背后 (8s CD), 30%失明+云雾粒子
- VAMPIRIC_LIFESTEAL: 攻击触发吸血30%+虚弱+爱心粒子
- FLAMING_INFERNO: 范围燃烧, 点燃3秒+火焰/岩浆粒子
- FREEZING_FROST: 范围减速, 缓慢III+雪/冰粒子
- EXPLOSIVE_BLAST: 投掷TNT (6s CD), 爆炸+击飞+波状粒子
- SHIELDED_BARRIER: HP<30%开盾, 吸收50%伤害
- INVISIBLE_CLOAK: 15s隐身 (25s CD), 隐身+传送门粒子
- REGENERATING_HEAL: HP<40%回血, 持续回复+心形粒子
- SUMMONING_MINIONS: HP<60%召唤 (30s CD), 召唤2-4只
- CHAINING_PULL: 拉人 (12s CD), 拉到身边+仇恨

### 尸潮系统
- 多波次生成 — 每波可配怪物数/类型/精英概率/Boss
- 自动定时 — auto-interval 秒间隔自动触发
- 夜晚随机 — random-chance 概率每夜检查
- 指令控制 — /elite horde start/stop/info
- Title 播报 — 每波开始/完成/失败全屏大标题
- BossBar 进度 — 当前波次剩余怪物数 + 倒计时条
- 波次倒计时 — wave-interval 秒波间间隔
- 超时失败 — wave-timeout 秒未清完则失败
- 参战奖励 — 仅打/被打玩家获得波次奖励
- 结束后可重开 — completeHorde/failHorde 清除 activeHorde
- stop 无尸潮反馈 — 命令层检查 isHordeActive(), 发送 horde-inactive
- 生成位置安全 — getSafeSpawnLocation 避开水/岩浆/墙壁
- 怪物高亮 — glow-mobs 配置
- 生成半径 — spawn-radius/spawn-min-radius 环形区域

### 奖励系统 (v1.1 新增)
- ITEM: 物品 — 材质/数量/名称/Lore/附魔/CustomModelData/头颅纹理(Base64)
- EXP: 经验值或等级 — 支持 min-max 范围随机
- VAULT: 游戏币 (Vault) — 支持金额范围含小数
- COMMAND: 控制台指令 — 占位符: {player} {x} {y} {z}
- PERMISSION: LuckPerms 权限/称号 — 支持限时(7d)或永久
- GROUP: 组合奖励 — 包含多个子奖励(可递归嵌套), 按顺序发放

### 配置系统
- 配置自动迁移 — 启动时检测 config-version, 旧版备份到 backup/ 并替换新模板
- 反射同步 getConfig — ConfigManager.load() 用反射替换 JavaPlugin.config, 修复热重载不生效
- Debug 模式 — config.yml 设 debug: true 输出精英生成/尸潮波次/奖励发放日志
- 版本管理 — config.yml(v4)/lang.yml(v1)/rewards.yml(v1)
- config.yml 双格式兼容 — rewards 字段支持新格式 ["id"] 和旧格式 {money/xp/items}

---

## 待办事项

### 高优先级
- [ ] /elite reload 重载时恢复所有精英怪状态 — 当前 reload 后已有精英怪不会重新加载数据
- [ ] ChatColor -> Adventure Component 迁移 — EliteCommand 中 affix.getColor() 还在用旧格式
- [ ] NamespacedKey 构造函数 — new NamespacedKey(plugin, key) 可能在新版 API 中改变

### 中优先级
- [ ] CustomModelData 材质包支持 — 已支持 ItemStack 层面, 需 GUI 预览
- [ ] LuckPerms 上下文 — 按权限组调整精英怪概率
- [ ] 精英怪掉落表 — 自定义掉落配置 (独立 loot.yml)
- [ ] 尸潮难度自适应 — 根据在线人数动态调整波次强度
- [ ] center-mode: all_players — 所有玩家周围分别刷怪

### 低优先级
- [ ] 粒子颜色支持 (DustOptions for REDSTONE particle)
- [ ] 更多粒子图案 (心形/星形/漩涡)
- [ ] 性能优化 (粒子任务统一管理, Entity 缓存)
- [ ] PlaceholderAPI 支持
- [ ] GitHub Actions 自动构建 + Release

---

## 重要修改记录

| 日期 | 变更 |
|------|------|
| 2026-05-16 | 初始搭建: pom.xml, plugin.yml, 包结构 |
| | ConfigManager + config.yml (11词缀/5星/生成条件) |
| | EliteGenerationListener (自然生成/属性/盔甲/粒子/音效) |
| | SkillManager (11技能) + VisualManager (HP条/粒子/音效/警报) |
| | EliteCommand (spawn/reload/info/list) + 尸潮系统 |
| | BUG: BOM -> UTF-8 无 BOM, Sound/Particle 枚举名称更新 |
| | BUG: 名字变长 (baseDisplayName), CMI 兼容 |
| | 生物中文映射 + 盔甲 + 闪电可配置 |
| | 尸潮奖励 (Vault/物品/XP/指令) + ParticleManager |
| | lang.yml 国际化 + LangManager, 所有消息迁移到 lang.yml |
| | 重构: 怪物名字 lang 可定制 — name-format + mob-names 移入 lang.yml |
| | 重构: 尸潮 Title+BossBar — 大标题播报 + 进度条 + 倒计时条 |
| | 重构: RGB渐变色 — GradientUtil + <g:#> 语法 + &#RRGGBB 六进制 |
| | 重构: 尸潮生成半径可配置 — spawn-radius / spawn-min-radius |
| | 重构: 受击反馈系统 — 11词缀各自独立受击效果 |
| | API迁移: Sound.valueOf -> Registry.SOUNDS, OldEnum.name -> Keyed.getKey |
| | BUG: 尸潮重复开启 — startHorde返回false+指令检测 |
| | BUG: 渐变色失效 — LegacyComponentSerializer character('&').hexColors() |
| | BUG: 开关不保存 — toggle 后 saveConfig() |
| | BUG: 模型名渐变色 — entity.setCustomName -> entity.customName(Component) |
| | 词缀中文化 — 11词缀趣味中文名 (狂战士/炎魔/...) |
| | BUG: 爆炸不破坏地形 — EntityExplodeEvent.blockList().clear() |
| | 区块精英上限 — generation.max-per-chunk, 尸潮期间绕过 |
| | 新指令: /elite clear — 范围/区块/世界/类型组合 |
| | BUG: 奖励时机 — 波次清除后发放, 非开始时 |
| | 参战奖励 — 只有打/被打的玩家获得奖励 |
| | 尸潮生成位置修复 — getSafeSpawnLocation 避开水/岩浆/墙壁 |
| | 尸潮怪物高亮 — glow-mobs 可配置 + HORDE_META_KEY 标记 |
| | 尸潮超时失败 — wave-timeout 配置 + failHorde() |
| | center-mode 可配置 — random_player / world_spawn |
| | 完成消息显示参战玩家 — %players% 变量 |
| 2026-05-17 | 重构: 独立奖励系统 — rewards.yml + 6种类型 (ITEM/EXP/VAULT/COMMAND/PERMISSION/GROUP) |
| | 重构: 奖励支持材质包 — CustomModelData + PLAYER_HEAD Base64头颅纹理 |
| | BUG: config-version 自动迁移 — 备份旧配置到 backup/ 文件夹 + 替换新模板 |
| | BUG: 尸潮结束后无法重开 — completeHorde/failHorde 清除 HordeManager.activeHorde |
| | BUG: 卸载后精英怪残留 — EliteMobData 存原始属性 + revertAllElites() 重置 |
| | BUG: 热重载配置不生效 — ConfigManager.load() 反射替换 JavaPlugin.config |
| | BUG: /elite horde stop 无尸潮无反馈 — 命令层 isHordeActive() 检查 + horde-inactive |
| | BUG: 精英怪初始过强 — generation.global-attribute-scale (默认0.5) |
| | 新功能: debug 模式 — config.yml debug:true, ConfigManager.debugLog() |
| | Git 仓库 — git init, 结构化 commit, GitHub: huanym/EliteMonsters |
| | README.md — GitHub 首页功能介绍/安装/指令/配置示例 |
| | PROJECT.md 全面更新 — 关键决策/架构/待办/修改记录 |
| | 编译通过 0 error, v1.1.0 |

---

## 部署步骤

1. 编译: cd "D:\File\New project\EliteMonsters" && mvn clean package -DskipTests
2. 将 target\EliteMonsters-1.1.0.jar 复制到服务器 plugins/
3. 若已有旧版: 插件启动后自动检测 config-version, 备份旧配置到 backup/ 并替换新模板
4. 重启服务器或 /plugman load EliteMonsters
5. 编辑 plugins/EliteMonsters/config.yml: 调整 global-attribute-scale / debug 等
6. 编辑 plugins/EliteMonsters/rewards.yml: 自定义奖励
7. /elite reload 热重载

---

## 下次会话快速启动

```powershell
# Git PATH
$env:Path = "D:\Git\bin;$env:Path"

# JDK
$env:JAVA_HOME = "$env:USERPROFILE\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 项目
cd "D:\File\New project\EliteMonsters"

# 拉取
git pull

# 编译
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd" clean package -DskipTests

# 输出: target\EliteMonsters-1.1.0.jar

# 推送 (手动, 需认证)
# git push
```