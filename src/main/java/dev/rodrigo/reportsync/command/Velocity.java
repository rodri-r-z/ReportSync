package dev.rodrigo.reportsync.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.rodrigo.reportsync.ReportSync;
import dev.rodrigo.reportsync.discord.DiscordBridge;
import dev.rodrigo.reportsync.lib.FancyYAML;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Velocity implements SimpleCommand {

    private final ReportSync plugin;
    private final DiscordBridge discordBridge;

    public Velocity(ReportSync plugin, DiscordBridge discordBridge) {
        this.plugin = plugin;
        this.discordBridge = discordBridge;
    }
    
    @Override
    public void execute(Invocation invocation) {
        plugin.proxyServer.getScheduler().buildTask(plugin, () -> {
            final CommandSource commandSender = invocation.source();
            final String[] args = invocation.arguments();
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(
                        Component.text(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_console"))
                        )
                );
                return;
            }
            if (args.length < 2) {
                commandSender.sendMessage(
                        Component.text(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.usage"))
                        )
                );
                return;
            }
            final String nick = args[0];
            final String reason = String.join(" ", args).substring(nick.length() + 1);
            final Player sender = (Player) commandSender;
            final Optional<Player> player = plugin.proxyServer.getPlayer(nick);
            if  (player.isPresent() && player.get().isActive() && player.get().getUsername().equals(sender.getUsername())) {
                commandSender.sendMessage(
                        Component.text(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_report_self"))
                        )
                );
                return;
            }
            if (nick.equalsIgnoreCase("reload") && (!player.isPresent() || !player.get().isActive())) {
                if (sender.hasPermission("reportsync.reload")) {
                    try {
                        plugin.config = new FancyYAML(plugin.dataFolder.resolve("config.yml"));
                    } catch (FileNotFoundException exception) {
                        plugin.logger.error("Could not reload config.yml because: " + exception.getMessage());
                        return;
                    }
                    commandSender.sendMessage(
                            Component.text(
                                    ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.config_reloaded"))
                            )
                    );
                    discordBridge.StartAgain(plugin.config);
                    return;
                } else {
                    if (!plugin.config.AsBoolean("overrides.hide_reload")) {
                        commandSender.sendMessage(
                                Component.text(
                                        ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_no_permission"))
                                )
                        );
                        return;
                    }
                }

            }
            if (!player.isPresent() || !player.get().isActive()) {
                commandSender.sendMessage(
                        Component.text(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_not_online"))
                        )
                );
                return;
            }
            if (player.get().hasPermission("reportsync.staff") && !plugin.config.AsBoolean("overrides.report_staff")) {
                commandSender.sendMessage(
                        Component.text(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.error_staff"))
                        )
                );
                return;
            }
            if (sender.hasPermission("reportsync.staff") && !plugin.config.AsBoolean("overrides.staff_report")) {
                commandSender.sendMessage(
                        Component.text(
                                ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.staff_report"))
                        )
                );
                return;
            }
            commandSender.sendMessage(
                    Component.text(
                            ChatColor.translateAlternateColorCodes('&', plugin.config.AsString("messages.report_sent"))
                    )
            );
            if (!sender.getCurrentServer().isPresent()) {
                plugin.logger.error("The player " + sender.getUsername() + " is not connected to a server");
                return;
            }
            for (Player plr : plugin.proxyServer.getAllPlayers().stream().filter(a -> a.hasPermission("reportsync.staff")).collect(Collectors.toList())) {
                plr.sendMessage(
                        Component.text(
                                String.join("\n", plugin.config.AsStringList("report_recieved.report"))
                                        .replaceAll("(?i)%executor%", sender.getUsername())
                                        .replaceAll("(?i)%reason%", reason)
                                        .replaceAll("(?i)%target%", player.get().getUsername())
                                        .replaceAll("(?i)%server%", sender.getCurrentServer().get().getServerInfo().getName())
                                        .replaceAll("&", "§")
                        )
                );
            }
            discordBridge.SendReport(sender.getUsername(), reason, player.get().getUsername(), sender.getCurrentServer().get().getServerInfo().getName());
        }).schedule();
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return plugin.proxyServer.getAllPlayers().stream().map(Player::getUsername).collect(Collectors.toList());
    }
}
