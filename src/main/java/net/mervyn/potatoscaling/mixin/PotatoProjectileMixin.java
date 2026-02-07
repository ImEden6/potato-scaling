package net.mervyn.potatoscaling.mixin;

import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;

import net.mervyn.potatoscaling.config.ScalingConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PotatoProjectileEntity.class)
public class PotatoProjectileMixin {

    @ModifyArg(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), index = 1)
    private float modifyDamageAmount(float originalDamage) {
        // Cast to our target entity to access the owner
        PotatoProjectileEntity projectile = (PotatoProjectileEntity) (Object) this;
        Entity owner = projectile.getOwner();

        if (!(owner instanceof PlayerEntity player)) {
            return originalDamage;
        }

        ScalingConfig config = ScalingConfig.get();

        // Base formula: (base * multiplier) + additive
        float currentDamage = (originalDamage * config.damageMultiplier) + config.damageAdditive;

        // Apply attribute bonuses
        for (ScalingConfig.CachedEntry entry : config.cachedEntries) {
            EntityAttribute attribute = Registries.ATTRIBUTE.get(entry.attributeId);
            if (attribute == null)
                continue;

            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance != null) {
                double attrValue = instance.getValue();
                if (Double.isNaN(attrValue) || Double.isInfinite(attrValue)) {
                    continue;
                }

                float adjustment = (float) attrValue * entry.multiplier;

                if (entry.op == ScalingConfig.Operation.ADD) {
                    currentDamage += adjustment;
                } else if (entry.op == ScalingConfig.Operation.MULTIPLY) {
                    // Interpretation: Multiplier applies to the running total
                    currentDamage *= adjustment;
                }
            }
        }

        if (currentDamage < 0.0f) {
            currentDamage = 0.0f;
        }

        return currentDamage;
    }
}
