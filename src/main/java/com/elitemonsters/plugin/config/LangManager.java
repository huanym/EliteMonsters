package com.elitemonsters.plugin.config;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.visual.GradientUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LangManager {

    private final EliteMonstersPlugin plugin;
    private FileConfiguration lang;
    private String locale;

    public LangManager(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
        locale = plugin.getConfig().getString("locale", "zh_CN");
        plugin.getLogger().info("LangManager loaded: " + locale);
    }

    public String getLocale() {
        return locale;
    }

    public String getRaw(String key) {
        String path = locale + "." + key;
        String msg = lang.getString(path);
        if (msg == null) {
            msg = lang.getString("en_US." + key);
        }
        return msg;
    }

    public String get(String key, Object... args) {
        String msg = getRaw(key);
        if (msg == null) return key;
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return msg;
    }

    public Component getComponent(String key, Object... args) {
        String msg = get(key, args);
        return GradientUtil.parse(msg);
    }

    public void reload() { load(); }
}