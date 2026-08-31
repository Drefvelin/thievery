package net.tfminecraft.thievery.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cache {
    public static int cooldown;
    public static int radius;

    public static boolean coreProtect = false;

    public static List<String> traits = new ArrayList<>();

    public static int categoryPoints = 30;
    public static int pointGainIntervalHours = 24;

    public static double defaultItemValue = 0.1;

    public static final Map<Integer, Double> tierValues = new HashMap<>();

    public static int recentClueMax = 6;
    public static int recentClueCooldownHours = 72;
    public static int criticalCooldownHours = 24;
    public static int minCluesDoor = 0;
    public static int minCluesContainer = 1;

    public static double riskGainDoorMin = 0.05;
    public static double riskGainDoorMax = 0.15;
    public static double riskGainChestMin = 0.025;
    public static double riskGainChestMax = 0.075;
    public static double riskGainPickpocketMin = 0.04;
    public static double riskGainPickpocketMax = 0.12;
    public static double riskPickReduction = 0.5;
    public static double riskDecayPerHour = 0.08;

    public static double criticalBase = 0.0;
    public static double criticalRiskWeight = 0.5;
    public static double criticalDexReduction = 0.15;
    public static double criticalStrengthReduction = 0.2;
    public static double takeValueScale = 12.0;
    public static double takeValueMaxBonus = 0.35;
    public static double takeClueDivisor = 10.0;
    public static String criticalClue = "§7This seems to be the work of #d6cf69{character_name}";

    public static double gravesBudget = 10;

    public static boolean interactibleFurniture = false;

    public static boolean requireOwnerOnline = false;
    public static boolean debugAllowOwnChest = false;
    public static boolean debugCluePreview = false;
}
