package mc.promcteam.engine.utils.actions.conditions.list;

import mc.promcteam.engine.NexPlugin;
import mc.promcteam.engine.utils.actions.conditions.IConditionType;
import mc.promcteam.engine.utils.actions.conditions.IConditionValidator;
import mc.promcteam.engine.utils.actions.params.IParamResult;
import mc.promcteam.engine.utils.actions.params.IParamType;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Condition_EntityType extends IConditionValidator {

    public Condition_EntityType(@NotNull NexPlugin<?> plugin) {
        super(plugin, IConditionType.ENTITY_TYPE);
    }

    @Override
    @NotNull
    public List<String> getDescription() {
        return plugin.lang().Core_Editor_Actions_Condition_EntityType_Desc.asList();
    }

    @Override
    public void registerParams() {
        this.registerParam(IParamType.NAME);
        this.registerParam(IParamType.TARGET);
    }

    @Override
    public boolean mustHaveTarget() {
        return true;
    }

    @Override
    @Nullable
    protected Predicate<Entity> validate(
            @NotNull Entity exe, @NotNull Set<Entity> targets, @NotNull IParamResult result) {

        String types = result.getParamValue(IParamType.NAME).getString("");
        if (types == null || types.isEmpty()) return null;

        return target -> types.equalsIgnoreCase(target.getType().name());
    }

}
