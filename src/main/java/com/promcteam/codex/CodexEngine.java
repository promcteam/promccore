package com.promcteam.codex;

import com.promcteam.codex.api.armor.ArmorListener;
import com.promcteam.codex.bungee.BungeeListener;
import com.promcteam.codex.bungee.BungeeUtil;
import com.promcteam.codex.commands.UnstuckCommand;
import com.promcteam.codex.commands.api.IGeneralCommand;
import com.promcteam.codex.commands.list.Base64Command;
import com.promcteam.codex.config.legacy.LegacyConfigManager;
import com.promcteam.codex.core.Version;
import com.promcteam.codex.core.config.CoreConfig;
import com.promcteam.codex.core.config.CoreLang;
import com.promcteam.codex.hooks.HookManager;
import com.promcteam.codex.hooks.Hooks;
import com.promcteam.codex.hooks.external.*;
import com.promcteam.codex.hooks.external.citizens.CitizensHK;
import com.promcteam.codex.items.CodexItemManager;
import com.promcteam.codex.legacy.item.ItemBuilder;
import com.promcteam.codex.legacy.placeholder.PlaceholderRegistry;
import com.promcteam.codex.legacy.riseitem.DarkRiseItemImpl;
import com.promcteam.codex.listeners.BoatListener;
import com.promcteam.codex.listeners.InteractListener;
import com.promcteam.codex.listeners.JoinListener;
import com.promcteam.codex.manager.api.menu.MenuManager;
import com.promcteam.codex.manager.editor.EditorManager;
import com.promcteam.codex.mccore.chat.ChatCommander;
import com.promcteam.codex.mccore.chat.ChatListener;
import com.promcteam.codex.mccore.commands.CommandLog;
import com.promcteam.codex.mccore.config.Config;
import com.promcteam.codex.mccore.scoreboard.BoardListener;
import com.promcteam.codex.mccore.scoreboard.CycleTask;
import com.promcteam.codex.mccore.scoreboard.ScoreboardCommander;
import com.promcteam.codex.mccore.scoreboard.UpdateTask;
import com.promcteam.codex.mccore.util.VersionManager;
import com.promcteam.codex.nms.NMS;
import com.promcteam.codex.nms.packets.PacketManager;
import com.promcteam.codex.util.Debugger;
import com.promcteam.codex.util.ItemUT;
import com.promcteam.codex.util.Reflex;
import com.promcteam.codex.util.actions.ActionsManager;
import com.promcteam.codex.util.craft.CraftManager;
import com.promcteam.codex.util.messages.MessageUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class CodexEngine extends CodexPlugin<CodexEngine> implements Listener {

    private static final Hashtable<String, Config> configs = new Hashtable<>();
    private static       CodexEngine               instance;
    private final        Set<CodexPlugin<?>>       plugins;
    @Getter
    private              NMS                       NMS;
    @Getter
    private              PluginManager             pluginManager;
    @Getter
    private              PacketManager             packetManager;
    @Getter
    private              ActionsManager            actionsManager;
    @Getter
    private              CraftManager              craftManager;
    @Getter
    private              MenuManager               menuManager;
    @Getter
    private              VaultHK                   vault;
    @Getter
    private              CitizensHK                citizens;
    @Getter
    private              WorldGuardHK              worldGuard;
    @Getter
    private              IMythicHook               mythicMobs;
    private              CoreConfig                cfg;
    private              CoreLang                  lang;
    @Getter
    private              HookManager               hooksManager;
    @Getter
    private              CodexItemManager          itemManager;

    /**
     * -- GETTER --
     * Checks whether Codex's chat management is enabled
     *
     * @return true if enabled, false otherwise
     */
    @Getter
    private boolean chatEnabled;
    /**
     * -- GETTER --
     * Retrieves the message to be shown when a command is on cooldown
     *
     * @return command cooldown message
     */
    @Getter
    private String  commandMessage = "&4Please wait &6{time} seconds &4before using the command again.";
    @Getter
    private boolean scoreboardsEnabled;

    private UnstuckCommand unstuck;
    private CycleTask      cTask;
    private UpdateTask     uTask;

    public CodexEngine() {
        setInstance();
        this.plugins = new HashSet<>();
    }

    public CodexEngine(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        setInstance();
        this.plugins = new HashSet<>();
    }

    private void setInstance() {
        instance = this;
        this.pluginManager = this.getServer().getPluginManager();
        ItemUT.setEngine(this);
        Reflex.setEngine(this);
    }

    @NotNull
    public static CodexEngine get() {
        return instance;
    }

    final boolean loadCore() {
        unstuck = new UnstuckCommand();

        ConfigurationSerialization.registerClass(ItemBuilder.class);
        ConfigurationSerialization.registerClass(DarkRiseItemImpl.class, "DarkRiseItemImpl");
        ConfigurationSerialization.registerClass(DarkRiseItemImpl.DivineItemsMeta.class, "DarkRiseItemImpl_Divine");

        // Register bungee messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, BungeeUtil.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, BungeeUtil.CHANNEL, new BungeeListener());


        if (!this.setupNMS()) {
            this.error("Could not setup NMS version. Plugin will be disabled.");
            return false;
        }

        MessageUtil.load(LegacyConfigManager.loadConfigFile(new File(getDataFolder() + File.separator + "lang",
                "messages_en.yml"), getResource("lang/messages_en.yml")), this);
        // Placeholder registration
        PlaceholderRegistry.load();

        setupManagers();

        return true;
    }

    protected void registerEvents() {
        getPluginManager().registerEvents(this, this);
        getPluginManager().registerEvents(new ArmorListener(), this);
        getPluginManager().registerEvents(unstuck, this);
        getPluginManager().registerEvents(new BoatListener(), this);
        getPluginManager().registerEvents(new InteractListener(cfg().getJYML()), this);
        getPluginManager().registerEvents(new JoinListener(this, cfg().getJYML()), this);
    }

    private void setupManagers() {
        this.hooksManager = new HookManager(this);
        this.hooksManager.setup();

        this.itemManager = new CodexItemManager(this);
        this.itemManager.init();

        this.packetManager = new PacketManager(this);
        this.packetManager.setup();

        this.actionsManager = new ActionsManager(this);
        this.actionsManager.setup();

        this.craftManager = new CraftManager(this);
        this.craftManager.setup();

        this.menuManager = new MenuManager(this);
        this.menuManager.setup();
    }

    private boolean setupNMS() {
        Version  current    = Version.CURRENT;
        String[] split      = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        String   rawVersion = split[split.length - 1];
        this.info("You are running MC version " + current + " (RAW: " + rawVersion + ")");
        if (current == null) {
            this.error("===== Codex Initialization Failure =====");
            this.error(rawVersion + " is not currently supported. Is this a new version of Spigot?");
            this.error("If this is a new version, please be patient and wait for a new build supporting the new version");
            this.error("If this is a version older than 1.16.5, sorry. We don't support <1.16.5");
            this.error("============================================");
            return false;
        }

        String   pack  = NMS.class.getPackage().getName();
        Class<?> clazz = Reflex.getClass(pack, current.name());
        if (clazz == null) return false;

        try {
            this.NMS = (NMS) clazz.getConstructor().newInstance();
            this.info("Loaded NMS version: " + current.name());
        } catch (Exception e) {
            this.warn("Failed to set up NMS version: " + current.name() + ". " + e.getMessage());
            e.printStackTrace();
        }
        return this.NMS != null;
    }

    @Override
    public void enable() {
        EditorManager.setup();

        CommandLog.callback = VersionManager::initialize;
        if (!Version.TEST.isCurrent())
            getServer().dispatchCommand(new CommandLog(), "version");

        getLogger().info("Codex has been enabled!");
    }

    @Override
    public void disable() {
        // Unregister Custom Actions Engine
        if (this.actionsManager != null) {
            this.actionsManager.shutdown();
            this.actionsManager = null;
        }
        if (this.packetManager != null) {
            this.packetManager.shutdown();
        }
        if (this.hooksManager != null) {
            this.hooksManager.shutdown();
        }
        if (this.craftManager != null) {
            this.craftManager.shutdown();
            this.craftManager = null;
        }

        if (isScoreboardsEnabled()) {
            cTask.cancel();
            uTask.cancel();
        }

        EditorManager.shutdown();
    }

    @Override
    public void setConfig() {
        this.cfg = new CoreConfig(this);
        this.cfg.setup();

        commandMessage = this.cfg.getJYML()
                .getString("Settings.command-cooldown-message",
                        "&4Please wait &6{time} seconds &4before using the command again.");
        chatEnabled = this.cfg.getJYML().getBoolean("Features.chat-enabled", true);
        scoreboardsEnabled = this.cfg.getJYML().getBoolean("Features.scoreboards-enabled", true);

        // Load config data
        BUNGEE_ID = cfg().getJYML().getString("bungee_id", "server");
        IS_BUNGEE = cfg().getJYML().getBoolean("bungee", false);

        boolean debug = cfg().getJYML().getBoolean("debug", false);
        Debugger.setDebug(debug);

        if (chatEnabled) {
            new ChatCommander(this);
            new ChatListener(this);
        }
        if (scoreboardsEnabled) {
            new ScoreboardCommander(this);
            new BoardListener(this);
            cTask = new CycleTask(this);
            uTask = new UpdateTask(this);
        }

        this.lang = new CoreLang(this);
        this.lang.setup();
    }

    @Override
    public void registerHooks() {
        try {
            this.vault = this.registerHook(Hooks.VAULT, VaultHK.class);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void registerCommands(@NotNull IGeneralCommand<CodexEngine> mainCommand) {
        mainCommand.addSubCommand(new Base64Command(this));

        PluginCommand unstuckCommand = getCommand("stuck");
        if (unstuckCommand != null) unstuckCommand.setExecutor(unstuck);
    }

    @Override
    public void registerEditor() {

    }

    @Override
    @NotNull
    public CoreConfig cfg() {
        return this.cfg;
    }

    @Override
    @NotNull
    public CoreLang lang() {
        return this.lang;
    }


    void hookChild(@NotNull CodexPlugin<?> child) {
        this.plugins.add(child);
    }

    @NotNull
    public Set<CodexPlugin<?>> getChildPlugins() {
        return this.plugins;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onHookLate(PluginEnableEvent e) {
        String name = e.getPlugin().getName();
        try {
            if (this.mythicMobs == null && name.equalsIgnoreCase(Hooks.MYTHIC_MOBS)) {
                boolean mythic4 = true;
                try {
                    Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
                } catch (ClassNotFoundException classNotFoundException) {
                    mythic4 = false;
                }

                this.mythicMobs = mythic4
                        ? this.registerHook(Hooks.MYTHIC_MOBS, MythicMobsHK.class)
                        : this.registerHook(Hooks.MYTHIC_MOBS, MythicMobsHKv5.class);
                return;
            }
            if (this.worldGuard == null && name.equalsIgnoreCase(Hooks.WORLD_GUARD)) {
                this.worldGuard = this.registerHook(Hooks.WORLD_GUARD, WorldGuardHK.class);
                return;
            }
            if (this.citizens == null && name.equalsIgnoreCase(Hooks.CITIZENS)) {
                this.citizens = this.registerHook(Hooks.CITIZENS, CitizensHK.class);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * <p>Retrieves the configuration file for a plugin</p>
     * <p>If the config file hasn't been loaded yet, this will
     * load the file first.</p>
     * <p>Configs retrieved via this method are handled by MCCore
     * and automatically saved when MCCore disables.</p>
     * <p>This should not be used for settings configs
     * that admins may want to edit while the server is running
     * as the auto save will overwrite any changes they make.</p>
     *
     * @param file file name
     * @return config manager for the file
     */
    public Config getConfigFile(JavaPlugin plugin, String file) {
        if (!configs.containsKey(file.toLowerCase() + plugin.getName())) {
            Config config = new Config(plugin, file);
            registerConfig(config);
            return config;
        }
        return configs.get(file.toLowerCase() + plugin.getName());
    }

    /**
     * <p>Registers the Config with MCCore for auto saving.</p>
     * <p>If the Config was already registered, this method will
     * not do anything.</p>
     *
     * @param config config to register
     */
    public void registerConfig(Config config) {
        configs.put(config.getFile().toLowerCase() + config.getPlugin().getName(), config);
    }
}