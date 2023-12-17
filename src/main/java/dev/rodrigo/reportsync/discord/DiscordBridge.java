package dev.rodrigo.reportsync.discord;

import dev.rodrigo.reportsync.lib.FancyYAML;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DiscordBridge {
    private URLClassLoader loader;
    private Object bot;
    private Object MessageChannel;
    private FancyYAML config;
    private Object parent;
    private BridgeListener listener;
    private Method discordReportBroadcaster;
    public DiscordBridge(Path libsFolder, FancyYAML config, ClassLoader classLoader, Object parent) {
        try {
            if (config.AsBoolean("discord.enabled")) {
                this.config = config;
                this.parent = parent;
                this.discordReportBroadcaster = parent.getClass().getMethod("discordReportBroadcast", String.class);
                this.loader = new URLClassLoader(new URL[] { libsFolder.toUri().toURL() }, classLoader);
                this.listener = new BridgeListener(config, loader, this);
                final Class<?> JDA = loader.loadClass("net.dv8tion.jda.api.JDABuilder");
                final Class<?> EventListenerClass = loader.loadClass("net.dv8tion.jda.api.hooks.EventListener");
                final Class<?> SlashCommandInteractionEvent = loader.loadClass("net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent");
                Object preBuild = JDA.getMethod("createDefault", String.class).invoke(parent, config.AsString("discord.bot_token"));
                Object build = preBuild.getClass().getMethod("build").invoke(preBuild);
                build.getClass().getMethod("awaitReady").invoke(build);


                bot = build;
                final Object eventListener = Proxy.newProxyInstance(
                        loader,
                        new Class<?>[] { EventListenerClass },
                        (proxy, method, args) -> {
                            if (method.getName().equals("onEvent")) {
                                if (args[0].getClass().getName().equals(SlashCommandInteractionEvent.getName())) {
                                    if (!config.AsBoolean("discord.command.enabled")) return null;
                                    String command = args[0].getClass().getMethod("getName").invoke(args[0]).toString();
                                    if (!command.equalsIgnoreCase(config.AsString("discord.command.name"))) return null;
                                    listener.onSlashCommandInteraction(args[0], discordReportBroadcaster, parent);
                                }
                                return null;
                            }
                            return proxy;
                        }
                );
                build.getClass().getMethod("addEventListener", Object[].class).invoke(build,
                        (Object) new Object[] {
                                eventListener
                        });

                queueCommands();
                MessageChannel = build.getClass().getMethod("getTextChannelById", String.class).invoke(build, config.AsString("discord.channel_id"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void queueCommands() throws Exception {
        final Class<?> CommandsClass = loader.loadClass("net.dv8tion.jda.api.interactions.commands.build.Commands");
        final Class<?> OptionTypeClass = loader.loadClass("net.dv8tion.jda.api.interactions.commands.OptionType");
        final List<Object> commands = new ArrayList<>();
        final Object slashCommand = CommandsClass.getMethod("slash", String.class, String.class).invoke(parent, "report", "Report a player");
        slashCommand.getClass().getMethod("setGuildOnly", boolean.class).invoke(slashCommand, true);

        if (!config.AsString("discord.command.options.target.type").equalsIgnoreCase("string") && !config.AsString("discord.command.options.target.type").equalsIgnoreCase("user")) {
            throw new RuntimeException("discord.command.options.target.type must be either 'string' or 'user'");
        }

        slashCommand.getClass().getMethod("addOption", OptionTypeClass, String.class, String.class, boolean.class).invoke(slashCommand,
                OptionTypeClass.getField(config.AsString("discord.command.options.target.type").toUpperCase()).get(null),
                config.AsString("discord.command.options.target.name"),
                config.AsString("discord.command.options.target.description"),
                config.AsBoolean("discord.command.options.target.required"));

        slashCommand.getClass().getMethod("addOption", OptionTypeClass, String.class, String.class, boolean.class).invoke(slashCommand,
                OptionTypeClass.getField("STRING").get(null),
                config.AsString("discord.command.options.reason.name"),
                config.AsString("discord.command.options.reason.description"),
                config.AsBoolean("discord.command.options.reason.required"));

        if (config.AsBoolean("discord.command.enabled")) {
            commands.add(slashCommand);
        }
        final Object requestUpdateCommands = bot.getClass().getMethod("updateCommands").invoke(bot);
        final Object formedRequestUpdateCommands = requestUpdateCommands.getClass().getMethod("addCommands", Collection.class)
                .invoke(
                        requestUpdateCommands,
                        commands
                );

        formedRequestUpdateCommands.getClass().getMethod("queue").invoke(formedRequestUpdateCommands);
    }

    public void StartAgain(FancyYAML config) {
        this.config = config;
        listener.setConfig(config);
        try {
            queueCommands();
            MessageChannel = bot.getClass().getMethod("getTextChannelById", String.class).invoke(bot, config.AsString("discord.channel_id"));
        } catch (Exception e) {
            throw new RuntimeException("Could not reload ReportSync because: " + e.getMessage());
        }
    }

    public void SendReport(String executor, String reason, String target, String server) {
        try {
            if (!config.AsBoolean("discord.enabled")) return;
            final List<Object> embeds = new ArrayList<>();
            embeds.add(new EmbedBuilder(config, loader).Build("received", executor, reason, target, server));
            final Object message = MessageChannel.getClass().getMethod("sendMessageEmbeds", Collection.class).invoke(MessageChannel, embeds);
            message.getClass().getMethod("queue").invoke(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
