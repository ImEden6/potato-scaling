package net.mervyn.potatoscaling.config;

import net.tinyconfig.versioning.VersionableConfig;
import net.mervyn.potatoscaling.PotatoScalingMod;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

public class ScalingConfig extends VersionableConfig {

    public String _comment = "Formula: Final Damage = (Base + Additive) * (1 + Attribute Bonus / Scaling Factor)";
    public String _usage = "Example: Set 'damageAdditive' to 5.0 for flat +5 base damage. Set 'damageScalingFactor' to control attribute strength.";

    public float damageAdditive = 0.0f;
    public float damageScalingFactor = 1.0f;
    public List<ScalingEntry> scalingEntries = new ArrayList<>();

    public transient List<CachedEntry> cachedEntries = new ArrayList<>();

    public ScalingConfig() {
        scalingEntries.add(new ScalingEntry("ranged_weapon:damage", "ADD", 1.0f));
    }

    public static class ScalingEntry {
        public String attribute;
        public String operation;
        public float valueMultiplier;

        public ScalingEntry() {
        }

        public ScalingEntry(String attribute, String operation, float valueMultiplier) {
            this.attribute = attribute;
            this.operation = operation;
            this.valueMultiplier = valueMultiplier;
        }
    }

    public record CachedEntry(Identifier attributeId, Operation op, float multiplier) {
    }

    public enum Operation {
        ADD, MULTIPLY
    }

    public void cacheAttributes() {
        cachedEntries.clear();
        if (scalingEntries != null) {
            for (ScalingEntry entry : scalingEntries) {
                try {
                    Identifier id = new Identifier(entry.attribute);
                    Operation op;
                    try {
                        op = Operation.valueOf(entry.operation.toUpperCase());
                    } catch (Exception e) {
                        op = Operation.ADD;
                    }
                    cachedEntries.add(new CachedEntry(id, op, entry.valueMultiplier));
                } catch (Exception e) {
                    PotatoScalingMod.LOGGER.warn("Invalid attribute in config: " + entry.attribute);
                }
            }
        }
    }
}
