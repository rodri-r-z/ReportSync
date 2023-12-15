package dev.rodrigo.reportsync.discord;

import dev.rodrigo.FancyYAML;
import dev.rodrigo.reportsync.ReportSync;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DiscordBridge {
    private URLClassLoader loader;
    private Object bot;
    private Object MessageChannel;
    private FancyYAML config;
    private ReportSync plugin;
    public DiscordBridge(Path libsFolder, FancyYAML config, ReportSync plugin) {
        try {
            if (config.AsBoolean("discord.enabled")) {;
                this.config = config;
                this.plugin = plugin;
                this.loader = new URLClassLoader(new URL[] { libsFolder.toUri().toURL() }, plugin.getClass().getClassLoader());
                final Class<?> JDA = loader.loadClass("net.dv8tion.jda.api.JDABuilder");
                Object preBuild = JDA.getMethod("createDefault", String.class).invoke(plugin, config.AsString("discord.bot_token"));
                Object build = preBuild.getClass().getMethod("build").invoke(preBuild);
                build.getClass().getMethod("awaitReady").invoke(build);
                bot = build;
                MessageChannel = build.getClass().getMethod("getTextChannelById", String.class).invoke(build, config.AsString("discord.channel_id"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void StartAgain(FancyYAML config) {
        this.config = config;
        try {
            MessageChannel = bot.getClass().getMethod("getTextChannelById", String.class).invoke(bot, config.AsString("discord.channel_id"));
        } catch (Exception e) {
            plugin.getLogger().severe("Could not reload ReportSync because: " + e.getMessage());
        }
    }

    private String replacePlaceholders(String input, String executor, String reason, String target) {
        if (input == null) return null;
        return input
                .replaceAll("(?i)%executor%", executor)
                .replaceAll("(?i)%reason%", reason)
                .replaceAll("(?i)%target%", target);
    }

    public void SendReport(String executor, String reason, String target) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                if (!config.AsBoolean("discord.enabled")) return;
                final Object EmbedBuilder = loader.loadClass("net.dv8tion.jda.api.EmbedBuilder").getConstructor().newInstance();
                EmbedBuilder.getClass().getMethod("setTitle", String.class).invoke(EmbedBuilder,
                        replacePlaceholders(
                                config.AsString("discord.embed.title"),
                                executor,
                                reason,
                                target
                        )
                );
                EmbedBuilder.getClass().getMethod("setDescription", CharSequence.class).invoke(EmbedBuilder,
                        replacePlaceholders(
                                config.AsString("discord.embed.description"),
                                executor,
                                reason,
                                target
                        )
                );
                EmbedBuilder.getClass().getMethod("setUrl", String.class).invoke(EmbedBuilder,
                        replacePlaceholders(
                                config.AsString("discord.embed.url"),
                                executor,
                                reason,
                                target
                        )
                );
                EmbedBuilder.getClass().getMethod("setImage", String.class).invoke(EmbedBuilder,
                        replacePlaceholders(
                                config.AsString("discord.embed.image"),
                                executor,
                                reason,
                                target
                        )
                );
                EmbedBuilder.getClass().getMethod("setThumbnail", String.class).invoke(EmbedBuilder,
                        replacePlaceholders(
                                config.AsString("discord.embed.thumbnail"),
                                executor,
                                reason,
                                target
                        )
                );
                EmbedBuilder.getClass().getMethod("setAuthor", String.class, String.class).invoke(EmbedBuilder,
                        replacePlaceholders(
                                config.AsString("discord.embed.author.name"),
                                executor,
                                reason,
                                target
                        ),
                        replacePlaceholders(
                                config.AsString("discord.embed.author.icon_url"),
                                executor,
                                reason,
                                target
                        )
                );
                EmbedBuilder.getClass().getMethod("setFooter", String.class, String.class).invoke(EmbedBuilder,
                        replacePlaceholders(
                                config.AsString("discord.embed.footer.text"),
                                executor,
                                reason,
                                target
                        ),
                        replacePlaceholders(
                                config.AsString("discord.embed.footer.icon_url"),
                                executor,
                                reason,
                                target
                        )
                );
                EmbedBuilder.getClass().getMethod("setColor", int.class).invoke(EmbedBuilder, config.AsInt("discord.embed.color"));
                final Object embed = EmbedBuilder.getClass().getMethod("build").invoke(EmbedBuilder);
                final List<Object> embeds = new ArrayList<>();
                embeds.add(embed);
                final Object message = MessageChannel.getClass().getMethod("sendMessageEmbeds", Collection.class).invoke(MessageChannel, embeds);
                message.getClass().getMethod("queue").invoke(message);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send report to Discord: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}
