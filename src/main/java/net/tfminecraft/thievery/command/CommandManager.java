package net.tfminecraft.thievery.command;

import net.tfminecraft.thievery.Thievery;
import net.tfminecraft.thievery.manager.ContainerManager;
import net.tfminecraft.thievery.manager.InventoryManager;
import net.tfminecraft.thievery.util.TraitChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final ContainerManager containerManager;
    private final InventoryManager inventoryManager;

    public CommandManager(ContainerManager containerManager, InventoryManager inventoryManager) {
        this.containerManager = containerManager;
        this.inventoryManager = inventoryManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("thievery.admin")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            if (sender instanceof Player player) {
                Thievery.getInstance().reloadMessage(player);
            } else {
                Thievery.getInstance().reload();
                sender.sendMessage("§a[Thievery] §eReloaded.");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            if (player.hasPermission("thievery.admin")) {
                player.sendMessage("§e/thievery feedback §7- Toggle feedback alerts");
                player.sendMessage("§e/thievery reload §7- Reload plugin config");
            }
            player.sendMessage("§e/thievery loadout §7- Open thievery category loadout");
            return true;
        }

        if (args[0].equalsIgnoreCase("loadout")) {
            if (!TraitChecker.hasRequiredTraits(player)) {
                TraitChecker.sendMissingTraitMessage(player);
                return true;
            }
            inventoryManager.openLoadout(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("feedback")) {
            if (!player.hasPermission("thievery.admin")) {
                player.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            UUID uuid = player.getUniqueId();
            boolean current = containerManager.getFeedbackState(uuid);
            containerManager.setFeedbackState(uuid, !current);
            player.sendMessage("§e[Thievery] Feedback " + (!current ? "§aenabled" : "§cdisabled"));
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Try §e/thievery loadout");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("loadout");
            if (sender.hasPermission("thievery.admin")) {
                options.add("feedback");
                options.add("reload");
            }
            return options;
        }

        return Collections.emptyList();
    }
}
