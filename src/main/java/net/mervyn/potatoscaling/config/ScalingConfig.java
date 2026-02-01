package net.mervyn.potatoscaling.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.mervyn.potatoscaling.PotatoScalingMod;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;

public class ScalingConfig {

    public String comment = "Scaling Config: 'attribute' (e.g. generic.attack_damage), 'operation' (ADD/MULTIPLY), 'valueMultiplier' (coefficient).";

    @SerializedName("scaling_entries")
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

    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(),
            "potatoscaling.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ScalingConfig INSTANCE;

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
                if (INSTANCE.scalingEntries == null) {
                    INSTANCE.scalingEntries = new ArrayList<>();
                }
            } catch (IOException e) {
                PotatoScalingMod.LOGGER.error("Failed to load config", e);
                INSTANCE = new ScalingConfig();
            }
        } else {
            INSTANCE = new ScalingConfig();
        }

        // Ensure defaults if empty
        if (INSTANCE.scalingEntries.isEmpty()) {
            INSTANCE.scalingEntries.add(new ScalingEntry("ranged_weapon:damage", "ADD", 1.0f));
        }

        INSTANCE.cacheAttributes();

        // Save back to ensure comments/defaults are written
        save();
    }

    private void cacheAttributes() {
        cachedEntries.clear();
        for (ScalingEntry entry : scalingEntries) {
            cacheEntry(entry);
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

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            PotatoScalingMod.LOGGER.error("Failed to save config", e);
        }
    }
}
