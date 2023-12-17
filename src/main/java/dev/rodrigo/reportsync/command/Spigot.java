package dev.rodrigo.reportsync.command;

import dev.rodrigo.reportsync.discord.DiscordBridge;
import dev.rodrigo.reportsync.lib.FancyYAML;
import dev.rodrigo.reportsync.spigot.SpigotPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

public class Spigot implements CommandExecutor, TabExecutor {
    private final SpigotPlugin plugin;
    private final DiscordBridge discordBridge;

    public Spigot(SpigotPlugin plugin, DiscordBridge discordBridge) {
        this.plugin = plugin;
        this.discordBridge = discordBridge;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, org.bukkit.command.Command command, String label, String[] args) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!commandSender.hasPermission("reportsync.report")) {
                commandSender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_no_permission"))

                );
                return;
            }
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_console"))
                );
                return;
            }
            if (args.length < 2) {
                commandSender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.usage"))
                );
                return;
            }
            final String nick = args[0];
            final String reason = String.join(" ", args).substring(nick.length() + 1);
            final Player sender = (Player) commandSender;
            final Player player = plugin.getServer().getPlayer(nick);
            if  (player != null && player.getName().equals(sender.getName())) {
                commandSender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_report_self"))
                );
                return;
            }
            if (nick.equalsIgnoreCase("reload") && (player == null)) {
                if (sender.hasPermission("reportsync.reload")) {
                    try {
                        plugin.config = new FancyYAML(plugin.getDataFolder().toPath().resolve("config.yml"));
                    } catch (FileNotFoundException exception) {
                        plugin.getLogger().severe("Could not reload config.yml because: " + exception.getMessage());
                        return;
                    }
                    commandSender.sendMessage(
                            ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.config_reloaded"))
                    );
                    discordBridge.StartAgain(plugin.config);
                    return;
                } else {
                    if (!plugin.config.AsBoolean("overrides.hide_reload")) {
                        commandSender.sendMessage(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_no_permission"))
                        );
                        return;
                    }
                }

            }
            if (player == null) {
                commandSender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_not_online"))
                );
                return;
            }
            if (player.hasPermission("reportsync.staff") && !plugin.config.AsBoolean("overrides.report_staff")) {
                commandSender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_staff"))
                );
                return;
            }
            if (sender.hasPermission("reportsync.staff") && !plugin.config.AsBoolean("overrides.staff_report")) {
                commandSender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.staff_report"))
                );
                return;
            }
            commandSender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.report_sent"))
            );
            for (Player plr : plugin.getServer().getOnlinePlayers().stream().filter(a -> a.hasPermission("reportsync.staff")).collect(Collectors.toList())) {
                plr.sendMessage(
                        String.join("\n", plugin.config.AsStringList("report_recieved.report"))
                                .replaceAll("(?i)%executor%", sender.getName())
                                .replaceAll("(?i)%reason%", reason)
                                .replaceAll("(?i)%target%", player.getName())
                                .replaceAll("(?i)%server%", sender.getWorld().getName())
                                .replaceAll("&", "§")
                );
            }
            discordBridge.SendReport(sender.getName(), reason, player.getName(), sender.getWorld().getName());
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length < 1) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return null;
    }
}
