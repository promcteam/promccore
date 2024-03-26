package com.promcteam.codex.config.api;

import com.promcteam.codex.CodexDataPlugin;
import com.promcteam.codex.CodexPlugin;
import com.promcteam.codex.data.StorageType;
import com.promcteam.codex.modules.IModule;
import com.promcteam.codex.util.CollectionsUT;
import com.promcteam.codex.util.StringUT;
import org.jetbrains.annotations.NotNull;

public abstract class IConfigTemplate {

    protected CodexPlugin<?> plugin;
    protected JYML           cfg;

    public String   pluginName;
    public String[] cmds;
    public String   lang;

    public int         dataSaveInterval;
    public boolean     dataSaveInstant = false;
    public StorageType dataStorage;
    public String      mysqlLogin;
    public String      mysqlPassword;
    public String      mysqlHost;
    public String      mysqlBase;
    public boolean     dataPurgeEnabled;
    public int         dataPurgeDays;

    public IConfigTemplate(@NotNull CodexPlugin<?> plugin) {
        this.plugin = plugin;
    }

    public final void setup() {
        this.cfg = plugin.getConfigManager().configMain;
        this.cfg.addMissing("general.command-aliases", plugin.getName().toLowerCase());
        this.cfg.addMissing("general.lang", "en");
        this.cfg.addMissing("general.prefix", plugin.getName());

        this.pluginName = StringUT.color(cfg.getString("general.prefix", plugin.getName()));
        this.cmds = cfg.getString("general.command-aliases", "").split(",");
        this.lang = cfg.getString("general.lang", "en").toLowerCase();

        if (this.plugin instanceof CodexDataPlugin) {
            this.cfg.addMissing("data.auto-save", 20);
            this.cfg.addMissing("data.instant-save", false);
            this.cfg.addMissing("data.storage.type", "sqlite");
            this.cfg.addMissing("data.storage.username", "username");
            this.cfg.addMissing("data.storage.password", "password");
            this.cfg.addMissing("data.storage.host", "localhost");
            this.cfg.addMissing("data.storage.database", "none");
            this.cfg.addMissing("data.purge.enabled", false);
            this.cfg.addMissing("data.purge.days", 60);

            String      path        = "data.storage.";
            String      sType       = cfg.getString(path + "type", "sqlite").toUpperCase();
            StorageType storageType = CollectionsUT.getEnum(sType, StorageType.class);
            this.dataStorage = storageType == null ? StorageType.SQLITE : storageType;
            this.dataSaveInterval = cfg.getInt("data.auto-save", 20);
            this.dataSaveInstant = cfg.getBoolean("data.instant-save");

            if (this.dataStorage == StorageType.MYSQL) {
                this.mysqlLogin = cfg.getString(path + "username");
                this.mysqlPassword = cfg.getString(path + "password");
                this.mysqlHost = cfg.getString(path + "host");
                this.mysqlBase = cfg.getString(path + "database");
            }

            path = "data.purge.";
            this.dataPurgeEnabled = cfg.getBoolean(path + "enabled");
            this.dataPurgeDays = cfg.getInt(path + "days", 60);
        }

        this.load();
        this.save();
    }

    protected abstract void load();

    @NotNull
    public JYML getJYML() {
        return this.cfg;
    }

    public final void save() {
        this.cfg.saveChanges();
    }

    public final boolean isModuleEnabled(@NotNull IModule<?> module) {
        return this.isModuleEnabled(module.getId());
    }

    public final boolean isModuleEnabled(@NotNull String module) {
        this.cfg.addMissing("modules." + module + ".enabled", true);
        this.cfg.saveChanges();
        return this.cfg.getBoolean("modules." + module + ".enabled");
    }

    public final void disableModule(@NotNull IModule<?> module) {
        this.cfg.set("modules." + module.getId() + ".enabled", false);
        this.cfg.saveChanges();
    }

    @NotNull
    public final String getModuleName(@NotNull IModule<?> module) {
        this.cfg.addMissing("modules." + module.getId() + ".name",
                StringUT.capitalizeFully(module.getId().replace("_", " ")));
        this.cfg.saveChanges();
        return this.cfg.getString("modules." + module.getId() + ".name", module.getId());
    }
}