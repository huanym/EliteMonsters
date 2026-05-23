# EliteMonsters - Testing and Debugging Guide

## User Testing Checklist

### 1. Install Verification
- Put jar in plugins/ folder, restart server
- Check console: EliteMonsters v1.1.0 enabled!
- Check plugins/EliteMonsters/ has: config.yml, lang.yml, rewards.yml, loot.yml

### 2. Natural Elite Spawn Test
- Set config.yml: generation.base-chance: 1.0
- /elite reload
- Go to dark area at night, watch for elites
- Elites should have: colored name, star prefix, particle ring, health bar

### 3. Command Tests
- /elite info -> Shows affix list
- /elite spawn ZOMBIE FRENZY 5 -> 5-star Frenzy Zombie
- /elite spawn SKELETON -> Random affix skeleton  
- /elite list -> Shows nearby elites
- /elite toggle lightning -> Toggle lightning
- /elite clear 10 -> Clear elites within 10m

### 4. Horde Test
- /elite horde start -> Manual start
- Observe: BossBar, Title, mob spawn
- /elite horde info -> Status
- Defeat all waves -> Completion message + rewards
- /elite horde stop -> Manual stop

### 5. Reward Test
- /elite test reward wave1_default -> Test wave 1 reward

### 6. PlaceholderAPI Test (needs PAPI)
- /papi parse player %elitemonsters_total_elites%
- /papi parse player %elitemonsters_horde_active%
- /papi parse player %elitemonsters_nearest_elite%

---

## Developer Debugging

### Enable Debug Mode
config.yml: debug: true
Restart, all key ops log [Debug] to console.

### /elite test Commands
- /elite test info -> Runtime overview (elites/horde/memory/threads)
- /elite test spawn mob [affix] [star] -> Force spawn with error catch
- /elite test horde -> Immediate horde trigger
- /elite test loot mob [affix] [star] -> Spawn + verify loot
- /elite test reward rewardId -> Grant specific reward
- /elite test stress [count] -> Stress test (max 50)
- /elite test cleanup -> Force clear all elites

### Performance Debug Flow
1. /elite test info (baseline)
2. /elite test stress 50
3. /elite test info (compare memory)
4. /elite test cleanup
5. /elite test info (verify cleanup)

### API Event Verification
Other plugins can listen:
- EliteSpawnEvent
- EliteDeathEvent
- HordeStartEvent
- HordeCompleteEvent
- HordeFailEvent

### Common Issues
- No elite spawn: check whitelist/blacklist/base-chance/world-blacklist, enable debug
- No horde: check horde.enabled/min-players, try /elite test horde
- No particles: check particle-density, client particle settings
- No rewards: /elite test reward id, check rewards.yml format
- TPS drop: /elite test stress 50, timings report
- Memory leak: /elite test info, /elite test cleanup
- Event not firing: listen to API events + log