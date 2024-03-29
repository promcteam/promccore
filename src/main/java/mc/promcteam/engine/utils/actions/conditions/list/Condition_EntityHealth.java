package mc.promcteam.engine.utils.actions.conditions.list;

import mc.promcteam.engine.NexPlugin;
import mc.promcteam.engine.utils.EntityUT;
import mc.promcteam.engine.utils.actions.conditions.IConditionType;
import mc.promcteam.engine.utils.actions.conditions.IConditionValidator;
import mc.promcteam.engine.utils.actions.params.IParamResult;
import mc.promcteam.engine.utils.actions.params.IParamType;
import mc.promcteam.engine.utils.actions.params.IParamValue;
import mc.promcteam.engine.utils.actions.params.IParamValue.IOperator;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Condition_EntityHealth extends IConditionValidator {

    public Condition_EntityHealth(@NotNull NexPlugin<?> plugin) {
        super(plugin, IConditionType.ENTITY_HEALTH);
    }

    @Override
    @NotNull
    public List<String> getDescription() {
        return plugin.lang().Core_Editor_Actions_Condition_EntityHealth_Desc.asList();
    }

    @Override
    public void registerParams() {
        this.registerParam(IParamType.AMOUNT);
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

        IParamValue valHp = result.getParamValue(IParamType.AMOUNT);
        if (!valHp.hasDouble()) return null;

        double    hpReq     = valHp.getDouble(-1);
        boolean   isPercent = valHp.getBoolean();
        IOperator operator  = valHp.getOperator();

        return target -> {
            if (!(target instanceof LivingEntity)) return false;

            LivingEntity livingEntity = (LivingEntity) target;
            double       hpTarget     = livingEntity.getHealth();
            double       hpTargetMax  = EntityUT.getAttribute(livingEntity, Attribute.GENERIC_MAX_HEALTH);

            if (isPercent) {
                hpTarget = hpTarget / hpTargetMax * 100D;
            }

            return operator.check(hpTarget, hpReq);
        };
    }

}
