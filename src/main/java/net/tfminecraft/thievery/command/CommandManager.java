package net.tfminecraft.thievery.command;

import net.tfminecraft.thievery.manager.ContainerManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final ContainerManager containerManager;

    public CommandManager(ContainerManager containerManager) {
        this.containerManager = containerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("thievery.admin")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e/thievery feedback §7- Toggle feedback alerts");
            return true;
        }

        if (args[0].equalsIgnoreCase("feedback")) {
            UUID uuid = player.getUniqueId();
            boolean current = containerManager.getFeedbackState(uuid);
            containerManager.setFeedbackState(uuid, !current);
            player.sendMessage("§e[Thievery] Feedback " + (!current ? "§aenabled" : "§cdisabled"));
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Try §e/thievery feedback");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (!player.hasPermission("thievery.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return Collections.singletonList("feedback");
        }

        return Collections.emptyList();
    }
}
