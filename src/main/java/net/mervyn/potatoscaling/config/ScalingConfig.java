package net.mervyn.potatoscaling.config;

import net.fabricmc.loader.api.FabricLoader;
import net.mervyn.potatoscaling.PotatoScalingMod;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ScalingConfig {

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

        public static ScalingEntry fromString(String str) {
            String[] parts = str.split(",");
            if (parts.length >= 3) {
                return new ScalingEntry(parts[0].trim(), parts[1].trim(), Float.parseFloat(parts[2].trim()));
            }
            return null;
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
            "potatoscaling.properties");
    private static ScalingConfig INSTANCE;

    public static ScalingConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        INSTANCE = new ScalingConfig();
        Properties props = new Properties();

        if (CONFIG_FILE.exists()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                props.load(in);

                String dmgMult = props.getProperty("damage_multiplier");
                if (dmgMult != null)
                    INSTANCE.damageMultiplier = Float.parseFloat(dmgMult);

                String dmgAdd = props.getProperty("damage_additive");
                if (dmgAdd != null)
                    INSTANCE.damageAdditive = Float.parseFloat(dmgAdd);

                String entries = props.getProperty("scaling_entries");
                if (entries != null && !entries.isEmpty()) {
                    for (String entryStr : entries.split(";")) {
                        if (entryStr.trim().isEmpty())
                            continue;
                        try {
                            ScalingEntry entry = ScalingEntry.fromString(entryStr);
                            if (entry != null)
                                INSTANCE.scalingEntries.add(entry);
                        } catch (Exception e) {
                            PotatoScalingMod.LOGGER.error("Failed to parse scaling entry: " + entryStr, e);
                        }
                    }
                }
            } catch (IOException e) {
                PotatoScalingMod.LOGGER.error("Failed to load config", e);
            }
        }

        // Ensure defaults if empty (only if parsing failed or file didn't exist/didn't
        // have entries)
        if (INSTANCE.scalingEntries.isEmpty()) {
            // Default: ranged_weapon:damage ADD 1.0 (if that's a valid attribute?)
            // Or maybe minecraft:generic.attack_damage?
            // The previous code had "ranged_weapon:damage". Keeping it.
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
        Properties props = new Properties();
        props.setProperty("damage_multiplier", String.valueOf(INSTANCE.damageMultiplier));
        props.setProperty("damage_additive", String.valueOf(INSTANCE.damageAdditive));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < INSTANCE.scalingEntries.size(); i++) {
            sb.append(INSTANCE.scalingEntries.get(i).toString());
            if (i < INSTANCE.scalingEntries.size() - 1) {
                sb.append(";");
            }
        }
        props.setProperty("scaling_entries", sb.toString());

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Potato Scaling Configuration\n" +
                    "damage_multiplier: Multiplier for the base damage (default 1.0)\n" +
                    "damage_additive: Additive damage applied after multiplier (default 0.0)\n" +
                    "scaling_entries: Semicolon-separated list of entries in format: attribute,operation,value\n" +
                    "  attribute: The attribute ID (e.g. minecraft:generic.attack_damage)\n" +
                    "  operation: ADD or MULTIPLY\n" +
                    "  value: The multiplier for the attribute value");
        } catch (IOException e) {
            PotatoScalingMod.LOGGER.error("Failed to save config", e);
        }
    }
}
