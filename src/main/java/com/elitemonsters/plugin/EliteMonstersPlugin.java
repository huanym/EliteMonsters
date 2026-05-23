package com.elitemonsters.plugin;

import com.elitemonsters.plugin.affix.AffixManager;
import com.elitemonsters.plugin.command.EliteCommand;
import com.elitemonsters.plugin.config.ConfigManager;
import com.elitemonsters.plugin.config.LangManager;
import com.elitemonsters.plugin.generation.EliteGenerationListener;
import com.elitemonsters.plugin.horde.HordeManager;
import com.elitemonsters.plugin.reward.RewardManager;
import com.elitemonsters.plugin.skill.SkillManager;
import com.elitemonsters.plugin.visual.VisualManager;
import com.elitemonsters.plugin.integration.WorldGuardHelper;
import com.elitemonsters.plugin.equipment.EquipmentManager;
import com.elitemonsters.plugin.integration.MythicMobsHelper;
import com.elitemonsters.plugin.gui.EliteGUI;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class EliteMonstersPlugin extends JavaPlugin {

    private static final int BUNDLED_CONFIG_VERSION = 4;
    private static final int BUNDLED_LANG_VERSION = 1;
    private static final int BUNDLED_REWARDS_VERSION = 1;

    private static EliteMonstersPlugin instance;
    private ConfigManager configManager;
    private LangManager langManager;
    private AffixManager affixManager;
    private SkillManager skillManager;
    private VisualManager visualManager;
    private EliteGenerationListener generationListener;
    private HordeManager hordeManager;
    private Economy economy;
    private ErrorLogger errorLogger;
    private LootManager lootManager;
    private RewardManager rewardManager;
    private WorldGuardHelper worldGuardHelper;
    private EquipmentManager equipmentManager;
    private MythicMobsHelper mythicMobsHelper;
    private EliteGUI eliteGUI;

    @Override
    public void onEnable() {
        instance = this;

        errorLogger = new ErrorLogger(this);
        saveDefaultConfig();
        saveResource("lang.yml", false);
        saveResource("rewards.yml", false);

        migrateConfigs();

        configManager = new ConfigManager(this);
        configManager.load();
        langManager = new LangManager(this);
        langManager.load();
        if (!setupEconomy()) getLogger().warning("Vault not found, economy features disabled");
        affixManager = new AffixManager(this);
        affixManager.loadAffixes();
        skillManager = new SkillManager(this);
        skillManager.loadSkills();
        visualManager = new VisualManager(this);
        lootManager = new LootManager(this);
        lootManager.load();
        rewardManager = new RewardManager(this);
        rewardManager.load();

        generationListener = new EliteGenerationListener(this);
        getServer().getPluginManager().registerEvents(generationListener, this);
        hordeManager = new HordeManager(this);
        hordeManager.startAutoTask();
        getServer().getPluginManager().registerEvents(new com.elitemonsters.plugin.horde.HordeNightListener(this), this);
        EliteCommand eliteCommand = new EliteCommand(this);
        getCommand("elite").setExecutor(eliteCommand);
        getCommand("elite").setTabCompleter(eliteCommand);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ElitePlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled");
        }
        worldGuardHelper = new WorldGuardHelper(this);
        worldGuardHelper.load();
        equipmentManager = new EquipmentManager(this);
        equipmentManager.load();
        mythicMobsHelper = new MythicMobsHelper(this);
        mythicMobsHelper.load();
        eliteGUI = new EliteGUI(this);
        getLogger().info("EliteMonsters v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        if (generationListener != null) {
            generationListener.revertAllElites();
        }

        if (hordeManager != null) {
            hordeManager.cleanup();
            hordeManager = null;
        }

        generationListener = null;
        skillManager = null;
        visualManager = null;
        affixManager = null;
        configManager = null;
        langManager = null;
        rewardManager = null;

        getLogger().info("EliteMonsters disabled!");
        instance = null;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    private void migrateConfigs() {
        migrateConfig("config.yml", BUNDLED_CONFIG_VERSION);
        migrateConfig("lang.yml", BUNDLED_LANG_VERSION);
        migrateConfig("rewards.yml", BUNDLED_REWARDS_VERSION);
    }

    private void migrateConfig(String fileName, int bundledVersion) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) return;

        FileConfiguration fc = YamlConfiguration.loadConfiguration(file);
        int oldVersion = fc.getInt("config-version", 0);
        if (oldVersion >= bundledVersion) return;

        File backupDir = new File(getDataFolder(), "backup");
        if (!backupDir.exists()) backupDir.mkdirs();
        File backup = new File(backupDir, fileName + ".v" + oldVersion + ".bak");
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            getLogger().warning("Failed to backup " + fileName + ": " + e.getMessage());
            return;
        }

        saveResource(fileName, true);

        getLogger().warning("");
        getLogger().warning("================================================");
        getLogger().warning("  " + fileName + " migrated: v" + oldVersion + " -> v" + bundledVersion);
        getLogger().warning("  Backup: plugins/EliteMonsters/backup/" + backup.getName());
        getLogger().warning("  Old settings preserved in backup. Review new config.");
        getLogger().warning("================================================");
        getLogger().warning("");
    }

    public void broadcastComponent(Component component) {
        getServer().broadcast(component);
    }

    public static EliteMonstersPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public LangManager getLangManager() { return langManager; }
    public AffixManager getAffixManager() { return affixManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public VisualManager getVisualManager() { return visualManager; }
    public EliteGenerationListener getGenerationListener() { return generationListener; }
    public HordeManager getHordeManager() { return hordeManager; }
    public Economy getEconomy() { return economy; }
    public ErrorLogger getErrorLogger() { return errorLogger; }
    public LootManager getLootManager() { return lootManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public WorldGuardHelper getWorldGuardHelper() { return worldGuardHelper; }
    public EquipmentManager getEquipmentManager() { return equipmentManager; }
    public MythicMobsHelper getMythicMobsHelper() { return mythicMobsHelper; }
    public EliteGUI getEliteGUI() { return eliteGUI; }
    public void reload() { configManager.load(); langManager.load(); affixManager.loadAffixes(); skillManager.loadSkills(); visualManager = new VisualManager(this); lootManager.load();
        rewardManager.load(); if (worldGuardHelper != null) worldGuardHelper.load(); if (equipmentManager != null) equipmentManager.load(); if (mythicMobsHelper != null) mythicMobsHelper.load(); }
}