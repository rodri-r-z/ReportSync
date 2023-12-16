package dev.rodrigo.reportsync.command;

import dev.rodrigo.FancyYAML;
import dev.rodrigo.reportsync.ReportSync;
import dev.rodrigo.reportsync.discord.DiscordBridge;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Bungee extends Command implements TabExecutor {
    private final ReportSync plugin;
    private final DiscordBridge discordBridge;

    public Bungee(ReportSync plugin, DiscordBridge discordBridge) {
        super("report");
        this.plugin = plugin;
        this.discordBridge = discordBridge;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            if (!(commandSender instanceof ProxiedPlayer)) {
                commandSender.sendMessage(
                        TextComponent.fromLegacyText(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_console"))
                        )
                );
                return;
            }
            if (args.length < 2) {
                commandSender.sendMessage(
                        TextComponent.fromLegacyText(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.usage"))
                        )
                );
                return;
            }
            final String nick = args[0];
            final String reason = String.join(" ", args).substring(nick.length() + 1);
            final ProxiedPlayer sender = (ProxiedPlayer) commandSender;
            final ProxiedPlayer player = plugin.getProxy().getPlayer(nick);
            if  (player != null && player.isConnected() && player.getName().equals(sender.getName())) {
                commandSender.sendMessage(
                        TextComponent.fromLegacyText(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_report_self"))
                        )
                );
                return;
            }
            if (nick.equalsIgnoreCase("reload") && (player == null || !player.isConnected())) {
                if (sender.hasPermission("reportsync.reload")) {
                    try {
                        plugin.config = new FancyYAML(plugin.getDataFolder().toPath().resolve("config.yml"));
                    } catch (FileNotFoundException exception) {
                        plugin.getLogger().severe("Could not reload config.yml because: " + exception.getMessage());
                        return;
                    }
                    commandSender.sendMessage(
                            TextComponent.fromLegacyText(
                                    ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.config_reloaded"))
                            )
                    );
                    discordBridge.StartAgain(plugin.config);
                    return;
                } else {
                    if (!plugin.config.AsBoolean("overrides.hide_reload")) {
                        commandSender.sendMessage(
                                TextComponent.fromLegacyText(
                                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_no_permission"))
                                )
                        );
                        return;
                    }
                }

            }
            if (player == null || !player.isConnected()) {
                commandSender.sendMessage(
                        TextComponent.fromLegacyText(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_not_online"))
                        )
                );
                return;
            }
            if (player.hasPermission("reportsync.staff") && !plugin.config.AsBoolean("overrides.report_staff")) {
                commandSender.sendMessage(
                        TextComponent.fromLegacyText(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_staff"))
                        )
                );
                return;
            }
            if (sender.hasPermission("reportsync.staff") && !plugin.config.AsBoolean("overrides.staff_report")) {
                commandSender.sendMessage(
                        TextComponent.fromLegacyText(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.staff_report"))
                        )
                );
                return;
            }
            commandSender.sendMessage(
                    TextComponent.fromLegacyText(
                            ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.report_sent"))
                    )
            );
            for (ProxiedPlayer plr : plugin.getProxy().getPlayers().stream().filter(a -> a.hasPermission("reportsync.staff")).collect(Collectors.toList())) {
                plr.sendMessage(
                        TextComponent.fromLegacyText(
                                String.join("\n", plugin.config.AsStringList("report_recieved.report"))
                                        .replaceAll("(?i)%executor%", sender.getName())
                                        .replaceAll("(?i)%reason%", reason)
                                        .replaceAll("(?i)%target%", player.getName())
                                        .replaceAll("(?i)%server%", sender.getServer().getInfo().getName())
                                        .replaceAll("&", "§")
                        )
                );
            }
            discordBridge.SendReport(sender.getName(), reason, player.getName(), sender.getServer().getInfo().getName());
        });
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] args) {
        if (args.length < 2) {
            return plugin.getProxy().getPlayers().stream().map(ProxiedPlayer::getName).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
