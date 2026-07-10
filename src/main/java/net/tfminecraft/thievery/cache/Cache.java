package net.tfminecraft.thievery.cache;

import java.util.ArrayList;
import java.util.List;

public class Cache {
    public static int cooldown;
    public static int radius;

    public static boolean coreProtect = false;

    public static List<String> traits = new ArrayList<>();

    public static int categoryPoints = 30;
    public static int pointGainIntervalHours = 24;

    public static double defaultValue = 0.1;

    public static int recentClueMax = 6;
    public static int recentClueCooldownHours = 72;
    public static int criticalCooldownHours = 24;

    public static double riskGainDoorMin = 0.05;
    public static double riskGainDoorMax = 0.15;
    public static double riskGainChestMin = 0.025;
    public static double riskGainChestMax = 0.075;
    public static double riskPickReduction = 0.5;
    public static double riskDecayPerHour = 0.08;

    public static double criticalBase = 0.0;
    public static double criticalRiskWeight = 0.5;
    public static double criticalDexReduction = 0.15;
    public static double criticalStrengthReduction = 0.2;
    public static String criticalClue = "§7This seems to be the work of {character_name}";
}
