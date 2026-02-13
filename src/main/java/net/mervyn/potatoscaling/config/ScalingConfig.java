package net.mervyn.potatoscaling.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.mervyn.potatoscaling.PotatoScalingMod;
import net.minecraft.util.Identifier;

public class ScalingConfig {

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("potatoscaling.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static ScalingConfig INSTANCE = new ScalingConfig();

    public String _comment = "Formula: Final Damage = (Base * Multiplier) + Additive + Attribute Bonus";
    public String _usage = "Example: Set 'damageMultiplier' to 1.5 for 50% more damage. 'scalingEntries' format: attribute,operation,value";

    public float damageMultiplier = 1.0f;
    public float damageAdditive = 0.0f;
    public List<ScalingEntry> scalingEntries = new ArrayList<>();

    // Internal cache, not serialized
    public transient List<CachedEntry> cachedEntries = new ArrayList<>();

    public static class ScalingEntry {
        public String attribute;
        public String operation; // "ADD" or "MULTIPLY"
        public float valueMultiplier;

        public ScalingEntry(String attribute, String operation, float valueMultiplier) {
            this.attribute = attribute;
            this.operation = operation;
            this.valueMultiplier = valueMultiplier;
        }

        @Override
        public String toString() {
            return attribute + "," + operation + "," + valueMultiplier;
        }
    }

    public static class CachedEntry {
        public Identifier attributeId;
        public Operation op;
        public float multiplier;

        public CachedEntry(Identifier id, Operation op, float mult) {
            this.attributeId = id;
            this.op = op;
            this.multiplier = mult;
        }
    }

    public enum Operation {
        ADD, MULTIPLY
    }

    public static ScalingConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ScalingConfig.class);

                // Ensure comments are always present and up-to-date
                INSTANCE._comment = "Formula: Final Damage = (Base * Multiplier) + Additive + Attribute Bonus";
                INSTANCE._usage = "Example: Set 'damageMultiplier' to 1.5 for 50% more damage. 'scalingEntries' format: attribute,operation,value";

                if (INSTANCE.scalingEntries == null) {
                    INSTANCE.scalingEntries = new ArrayList<>();
                }
            } catch (IOException e) {
                PotatoScalingMod.LOGGER.error("Failed to load potatoscaling.json", e);
            }
        } else {
            // Setup defaults for a new file
            INSTANCE.scalingEntries.add(new ScalingEntry("ranged_weapon:damage", "ADD", 1.0f));
            save();
        }

        INSTANCE.cacheAttributes();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            PotatoScalingMod.LOGGER.error("Failed to save potatoscaling.json", e);
        }
    }

    private void cacheAttributes() {
        if (cachedEntries == null)
            cachedEntries = new ArrayList<>();
        cachedEntries.clear();

        if (scalingEntries != null) {
            for (ScalingEntry entry : scalingEntries) {
                cacheEntry(entry);
            }
        }
    }

    private void cacheEntry(ScalingEntry entry) {
        try {
            Identifier id = new Identifier(entry.attribute);
            Operation op = Operation.valueOf(entry.operation.toUpperCase());
            cachedEntries.add(new CachedEntry(id, op, entry.valueMultiplier));
        } catch (IllegalArgumentException e) {
            PotatoScalingMod.LOGGER.warn("Invalid operation in config: " + entry.operation + ". defaulting to ADD.");
            try {
                Identifier id = new Identifier(entry.attribute);
                cachedEntries.add(new CachedEntry(id, Operation.ADD, entry.valueMultiplier));
            } catch (Exception ex) {
                PotatoScalingMod.LOGGER.warn("Invalid attribute ID: " + entry.attribute);
            }
        } catch (Exception e) {
            PotatoScalingMod.LOGGER.warn("Invalid attribute ID in config: '" + entry.attribute + "'. Ignoring.");
        }
    }
}
