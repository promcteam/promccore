package mc.promcteam.engine.utils.actions.actions.list;

import mc.promcteam.engine.NexPlugin;
import mc.promcteam.engine.utils.actions.actions.IActionExecutor;
import mc.promcteam.engine.utils.actions.actions.IActionType;
import mc.promcteam.engine.utils.actions.params.IParamResult;
import mc.promcteam.engine.utils.actions.params.IParamType;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class Action_Broadcast extends IActionExecutor {

    public Action_Broadcast(@NotNull NexPlugin<?> plugin) {
        super(plugin, IActionType.BROADCAST);
    }

    @Override
    @NotNull
    public List<String> getDescription() {
        return plugin.lang().Core_Editor_Actions_Action_Broadcast_Desc.asList();
    }

    @Override
    public void registerParams() {
        this.registerParam(IParamType.MESSAGE);
    }

    @Override
    protected void execute(@NotNull Entity exe, @NotNull Set<Entity> targets, @NotNull IParamResult result) {
        if (!result.hasParam(IParamType.MESSAGE)) return;

        String text = result.getParamValue(IParamType.MESSAGE).getString(null);
        if (text == null) return;

        text = text.replace("%executor%", exe.getName());

        plugin.getServer().broadcastMessage(text);
    }

    @Override
    public boolean mustHaveTarget() {
        return false;
    }

}
