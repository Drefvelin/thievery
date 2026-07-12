package net.tfminecraft.thievery.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.thievery.mask.MaskChannelOverride;
import net.tfminecraft.thievery.mask.MaskDefinition;

public final class MaskLoader {

    private static final Map<String, MaskDefinition> masks = new HashMap<>();
    private static final Map<String, MaskChannelOverride> channels = new HashMap<>();
    private static final Map<String, String> commandToChannel = new HashMap<>();

    public static final String DEFAULT_MASKED_COMMAND = "rp";

    private MaskLoader() {}

    public static void load(FileConfiguration config) {
        masks.clear();
        channels.clear();
        commandToChannel.clear();

        loadMasks(config);
        loadChannels(config);
    }

    private static void loadMasks(FileConfiguration config) {
        if (config.isList("masks")) {
            List<String> paths = config.getStringList("masks");
            int index = 0;
            for (String path : paths) {
                if (path == null || path.isBlank()) continue;
                String id = "mask_" + index++;
                masks.put(id.toLowerCase(Locale.ROOT), MaskDefinition.fromItemPath(id, path.trim()));
            }
            return;
        }

        if (!config.isConfigurationSection("masks")) {
            return;
        }

        ConfigurationSection section = config.getConfigurationSection("masks");
        for (String key : section.getKeys(false)) {
            ConfigurationSection maskSection = section.getConfigurationSection(key);
            if (maskSection == null) continue;
            masks.put(key.toLowerCase(Locale.ROOT), new MaskDefinition(key, maskSection));
        }
    }

    private static void loadChannels(FileConfiguration config) {
        if (!config.isConfigurationSection("masked-channels")) {
            return;
        }

        ConfigurationSection section = config.getConfigurationSection("masked-channels");
        for (String key : section.getKeys(false)) {
            ConfigurationSection channelSection = section.getConfigurationSection(key);
            if (channelSection == null) continue;

            MaskChannelOverride channel = new MaskChannelOverride(key, channelSection);
            channels.put(key.toLowerCase(Locale.ROOT), channel);

            for (String command : channel.getCommands()) {
                commandToChannel.put(command.toLowerCase(Locale.ROOT), key.toLowerCase(Locale.ROOT));
            }
        }
    }

    public static MaskDefinition resolveMask(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        for (MaskDefinition mask : masks.values()) {
            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, mask.getItem())) {
                return mask;
            }
        }
        return null;
    }

    public static List<String> getMaskItemPaths() {
        List<String> paths = new ArrayList<>();
        for (MaskDefinition mask : masks.values()) {
            paths.add(mask.getItem());
        }
        return Collections.unmodifiableList(paths);
    }

    public static MaskChannelOverride getChannel(String channelId) {
        if (channelId == null) return null;
        return channels.get(channelId.toLowerCase(Locale.ROOT));
    }

    public static String resolveChannelFromCommand(String commandLabel) {
        if (commandLabel == null) return null;
        return commandToChannel.get(commandLabel.toLowerCase(Locale.ROOT));
    }

    public static List<String> getMaskedChannelCommands() {
        List<String> commands = new ArrayList<>();
        for (MaskChannelOverride channel : channels.values()) {
            if ("none".equalsIgnoreCase(channel.getId())) {
                continue;
            }
            for (String command : channel.getCommands()) {
                if (command == null || command.isBlank()) {
                    continue;
                }
                String normalized = command.toLowerCase(Locale.ROOT);
                if (!commands.contains(normalized)) {
                    commands.add(normalized);
                }
            }
        }
        commands.sort((a, b) -> {
            if (a.equals(DEFAULT_MASKED_COMMAND)) return -1;
            if (b.equals(DEFAULT_MASKED_COMMAND)) return 1;
            return a.compareTo(b);
        });
        return Collections.unmodifiableList(commands);
    }
}
