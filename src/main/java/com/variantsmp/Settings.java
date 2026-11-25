package com.variantsmp;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;

public class Settings {

    public static final String MACE_NO_COOLDOWN = "nocooldown";
    public static final String MACE_VANILLA = "vanilla";
    public static final String MACE_NO_WIND_BURST = "nowindburst";
    public static final String MACE_DISABLED = "disabled";
    public static final String[] MACE_MODES = {MACE_NO_COOLDOWN, MACE_VANILLA, MACE_NO_WIND_BURST, MACE_DISABLED};

    public boolean pvpFeatures = true;
    public boolean combatLogEnabled = true;
    public boolean flightEnabled = false;

    public int maceCooldownSeconds = 20;
    public double maceMultiplier = 1.5;
    public String maceCooldownMode = MACE_DISABLED;
    public double maceLandGraceSeconds = 1.0;
    public boolean maceKillReset = true;

    public int spearCooldownSeconds = 15;
    public boolean spearSneakSave = true;

    public int combatSeconds = 120;
    public boolean combatItemLimits = true;
    public int godAppleCount = 2;
    public int totemCount = 1;
    public int pearlCount = 16;
    public double logoutHearts = 8.0;

    public boolean flightDrainOutsideCombat = false;
    public int flightStartSeconds = 20;
    public int flightMaxSeconds = 25;
    public int flightUnlockSeconds = 15;
    public double flightDamageHearts = 10.0;
    public double flightPerDamageSeconds = 2.5;
    public int flightPerKillSeconds = 5;

    public boolean pearlCatchEnabled = true;
    public double pearlCollisionRadius = 2.5;
    public double pearlMinFlightDistance = 3.0;
    public double pearlBlockZone = 4.0;
    public double pearlBlendZone = 1.0;
    public double pearlIpcWindowMs = 6.0;
    public double pearlIpcOffhandWindowMs = 20.0;

    public void copyFrom(Settings o) {
        pvpFeatures = o.pvpFeatures;
        combatLogEnabled = o.combatLogEnabled;
        flightEnabled = o.flightEnabled;
        maceCooldownSeconds = o.maceCooldownSeconds;
        maceMultiplier = o.maceMultiplier;
        maceCooldownMode = o.maceCooldownMode;
        maceLandGraceSeconds = o.maceLandGraceSeconds;
        maceKillReset = o.maceKillReset;
        spearCooldownSeconds = o.spearCooldownSeconds;
        spearSneakSave = o.spearSneakSave;
        combatSeconds = o.combatSeconds;
        logoutHearts = o.logoutHearts;
        combatItemLimits = o.combatItemLimits;
        godAppleCount = o.godAppleCount;
        totemCount = o.totemCount;
        pearlCount = o.pearlCount;
        flightDrainOutsideCombat = o.flightDrainOutsideCombat;
        flightStartSeconds = o.flightStartSeconds;
        flightMaxSeconds = o.flightMaxSeconds;
        flightUnlockSeconds = o.flightUnlockSeconds;
        flightDamageHearts = o.flightDamageHearts;
        flightPerDamageSeconds = o.flightPerDamageSeconds;
        flightPerKillSeconds = o.flightPerKillSeconds;
        pearlCatchEnabled = o.pearlCatchEnabled;
        pearlCollisionRadius = o.pearlCollisionRadius;
        pearlMinFlightDistance = o.pearlMinFlightDistance;
        pearlBlockZone = o.pearlBlockZone;
        pearlBlendZone = o.pearlBlendZone;
        pearlIpcWindowMs = o.pearlIpcWindowMs;
        pearlIpcOffhandWindowMs = o.pearlIpcOffhandWindowMs;
    }

    public void sanitize() {
        if (godAppleCount < 0) godAppleCount = 2;
        if (totemCount < 0) totemCount = 1;
        if (pearlCount < 0) pearlCount = 16;
        if (maceCooldownMode == null || maceModeIndex(maceCooldownMode) < 0) maceCooldownMode = MACE_DISABLED;
        if (pearlCollisionRadius < 0.5) pearlCollisionRadius = 2.5;
        if (pearlMinFlightDistance < 0) pearlMinFlightDistance = 3.0;
        if (pearlBlockZone < 0) pearlBlockZone = 4.0;
        if (pearlBlendZone < 0) pearlBlendZone = 1.0;
        if (pearlIpcWindowMs < 0) pearlIpcWindowMs = 6.0;
        if (pearlIpcOffhandWindowMs <= 0) pearlIpcOffhandWindowMs = 20.0;
    }

    public static int maceModeIndex(String mode) {
        for (int i = 0; i < MACE_MODES.length; i++) {
            if (MACE_MODES[i].equalsIgnoreCase(mode)) return i;
        }
        return -1;
    }

    public enum Kind { BOOL, INT, DOUBLE, MODE }

    public record Def(String id, int slot, Item icon, String label, String unit, double step, double min, double max,
                      Kind kind, ToDoubleFunction<Settings> get, ObjDoubleConsumer<Settings> set, String desc) {

        public String display(Settings s) {
            double v = get.applyAsDouble(s);
            return switch (kind) {
                case BOOL -> v > 0 ? "§aON" : "§cOFF";
                case MODE -> "§f" + MACE_MODES[Math.max(0, Math.min(MACE_MODES.length - 1, (int) v))];
                default -> "§f" + (unit.equals("x") ? "x" + Abilities.fmt(v) : Abilities.fmt(v) + unit);
            };
        }

        public String plain(Settings s) {
            double v = get.applyAsDouble(s);
            return switch (kind) {
                case BOOL -> v > 0 ? "on" : "off";
                case MODE -> MACE_MODES[Math.max(0, Math.min(MACE_MODES.length - 1, (int) v))];
                default -> unit.equals("x") ? "x" + Abilities.fmt(v) : Abilities.fmt(v) + unit;
            };
        }
    }

    private static List<Def> defs;

    public static List<Def> defs() {
        if (defs != null) return defs;
        List<Def> l = new ArrayList<>();
        l.add(new Def("pvpfeatures", 49, Items.NETHER_STAR, "PvP features", "", 1, 0, 1, Kind.BOOL,
                s -> s.pvpFeatures ? 1 : 0, (s, v) -> s.pvpFeatures = v > 0, "Off makes mace, lunge and elytra fully vanilla"));
        l.add(new Def("combatitemlimits", 15, Items.SHIELD, "Combat item limits", "", 1, 0, 1, Kind.BOOL,
                s -> s.combatItemLimits ? 1 : 0, (s, v) -> s.combatItemLimits = v > 0, "Caps god apples, totems and pearls per combat"));
        l.add(new Def("godapples", 16, Items.ENCHANTED_GOLDEN_APPLE, "God apples per combat", "", 1, 0, 64, Kind.INT,
                s -> s.godAppleCount, (s, v) -> s.godAppleCount = (int) v, "Then disabled until combat ends"));
        l.add(new Def("totems", 24, Items.TOTEM_OF_UNDYING, "Totems per combat", "", 1, 0, 64, Kind.INT,
                s -> s.totemCount, (s, v) -> s.totemCount = (int) v, "Then totems stop working until combat ends"));
        l.add(new Def("pearls", 25, Items.ENDER_PEARL, "Pearls per combat", "", 1, 0, 64, Kind.INT,
                s -> s.pearlCount, (s, v) -> s.pearlCount = (int) v, "Then disabled until combat ends"));
        l.add(new Def("combatlog", 28, Items.BARRIER, "Combat log", "", 1, 0, 1, Kind.BOOL,
                s -> s.combatLogEnabled ? 1 : 0, (s, v) -> s.combatLogEnabled = v > 0, "Logout damage while tagged"));
        l.add(new Def("flight", 32, Items.ELYTRA, "Flight time", "", 1, 0, 1, Kind.BOOL,
                s -> s.flightEnabled ? 1 : 0, (s, v) -> s.flightEnabled = v > 0, "Limit elytra in combat"));
        l.add(new Def("macecooldown", 10, Items.MACE, "Mace cooldown", "s", 1, 0, 3600, Kind.INT,
                s -> s.maceCooldownSeconds, (s, v) -> s.maceCooldownSeconds = (int) v, "After landing from the empowered state"));
        l.add(new Def("macemultiplier", 11, Items.HEAVY_CORE, "Mace multiplier", "x", 0.05, 0, 100, Kind.DOUBLE,
                s -> s.maceMultiplier, (s, v) -> s.maceMultiplier = v, "Damage from hit 3 until landing"));
        l.add(new Def("macecooldownmode", 19, Items.WIND_CHARGE, "Mace during cooldown", "", 1, 0, MACE_MODES.length - 1, Kind.MODE,
                s -> Math.max(0, maceModeIndex(s.maceCooldownMode)), (s, v) -> s.maceCooldownMode = MACE_MODES[(int) v],
                "What the mace does during its cooldown"));
        l.add(new Def("macelandgrace", 20, Items.SLIME_BALL, "Mace land grace", "s", 0.25, 0, 10, Kind.DOUBLE,
                s -> s.maceLandGraceSeconds, (s, v) -> s.maceLandGraceSeconds = v, "Delay after landing before the cooldown"));
        l.add(new Def("macekillreset", 21, Items.SKELETON_SKULL, "Mace kill reset", "", 1, 0, 1, Kind.BOOL,
                s -> s.maceKillReset ? 1 : 0, (s, v) -> s.maceKillReset = v > 0, "A player kill clears the mace cooldown"));
        l.add(new Def("spearcooldown", 12, spear(), "Spear cooldown", "s", 1, 0, 3600, Kind.INT,
                s -> s.spearCooldownSeconds, (s, v) -> s.spearCooldownSeconds = (int) v, "After a ground lunge or an air chain"));
        l.add(new Def("spearsneaksave", 23, Items.LEATHER_BOOTS, "Spear sneak save", "", 1, 0, 1, Kind.BOOL,
                s -> s.spearSneakSave ? 1 : 0, (s, v) -> s.spearSneakSave = v > 0, "Sneaking jabs never lunge or cost hunger"));
        l.add(new Def("combatseconds", 29, Items.CLOCK, "Combat timer", "s", 1, 0, 3600, Kind.INT,
                s -> s.combatSeconds, (s, v) -> s.combatSeconds = (int) v, "Tag length after a PvP hit"));
        l.add(new Def("logouthearts", 30, Items.REDSTONE, "Logout damage", " hearts", 0.5, 0, 100, Kind.DOUBLE,
                s -> s.logoutHearts, (s, v) -> s.logoutHearts = v, "Ignores armor, protection and resistance"));
        l.add(new Def("flightoutsidecombat", 33, Items.PHANTOM_MEMBRANE, "Drain outside combat", "", 1, 0, 1, Kind.BOOL,
                s -> s.flightDrainOutsideCombat ? 1 : 0, (s, v) -> s.flightDrainOutsideCombat = v > 0, "Off means free flight out of combat"));
        l.add(new Def("flightstart", 34, Items.FEATHER, "Flight start", "s", 1, 0, 3600, Kind.INT,
                s -> s.flightStartSeconds, (s, v) -> s.flightStartSeconds = (int) v, "What new players get"));
        l.add(new Def("flightmax", 37, Items.FIREWORK_ROCKET, "Flight max", "s", 1, 0, 3600, Kind.INT,
                s -> s.flightMaxSeconds, (s, v) -> s.flightMaxSeconds = (int) v, "Cap"));
        l.add(new Def("flightunlock", 38, Items.IRON_BARS, "Flight unlock", "s", 1, 0, 3600, Kind.INT,
                s -> s.flightUnlockSeconds, (s, v) -> s.flightUnlockSeconds = (int) v, "Needed to fly again after running out"));
        l.add(new Def("flightdamagehearts", 39, Items.GOLDEN_SWORD, "Damage per reward", " hearts", 0.5, 0.5, 100, Kind.DOUBLE,
                s -> s.flightDamageHearts, (s, v) -> s.flightDamageHearts = v, "Hearts dealt to players per reward"));
        l.add(new Def("flightperdamage", 40, Items.SUGAR, "Flight per reward", "s", 0.5, 0, 3600, Kind.DOUBLE,
                s -> s.flightPerDamageSeconds, (s, v) -> s.flightPerDamageSeconds = v, "Seconds gained per reward"));
        l.add(new Def("flightperkill", 41, Items.TOTEM_OF_UNDYING, "Flight per kill", "s", 1, 0, 3600, Kind.INT,
                s -> s.flightPerKillSeconds, (s, v) -> s.flightPerKillSeconds = (int) v, "Seconds gained per player kill"));
        defs = l;
        return l;
    }

    public static Def def(String id) {
        for (Def d : defs()) {
            if (d.id().equalsIgnoreCase(id)) return d;
        }
        return null;
    }

    private static Item spear() {
        Item item = Registries.ITEM.get(Identifier.ofVanilla("iron_spear"));
        return item == null || item == Items.AIR ? Items.TRIDENT : item;
    }
}
