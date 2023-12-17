package dev.rodrigo.reportsync.discord;

import dev.rodrigo.reportsync.lib.FancyYAML;

import java.net.URLClassLoader;

public class EmbedBuilder {
    private final FancyYAML config;
    private final URLClassLoader loader;
    public EmbedBuilder(FancyYAML config, URLClassLoader classLoader) {
        this.config = config;
        this.loader = classLoader;
    }
    
    public static String replacePlaceholders(String input, String executor, String reason, String target, String server) {
        if (input == null || input.isEmpty() || executor == null || executor.isEmpty() || server == null || server.isEmpty() || target == null || target.isEmpty() || reason == null || reason.isEmpty()) return input;
        return input
                .replaceAll("(?i)%executor%", executor)
                .replaceAll("(?i)%reason%", reason)
                .replaceAll("(?i)%target%", target)
                .replaceAll("(?i)%channel%", server)
                .replaceAll("(?i)%server%", server);
        
    }

    public Object Build(String configPath, String executor, String reason, String target, String server) {
        try {
            if (!config.AsBoolean("discord.enabled")) return null;
            final Object EmbedBuilder = loader.loadClass("net.dv8tion.jda.api.EmbedBuilder").getConstructor().newInstance();
            EmbedBuilder.getClass().getMethod("setTitle", String.class).invoke(EmbedBuilder,
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".title"),
                            executor,
                            reason,
                            target,
                            server
                    )
            );
            EmbedBuilder.getClass().getMethod("setDescription", CharSequence.class).invoke(EmbedBuilder,
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".description"),
                            executor,
                            reason,
                            target,
                            server
                    )
            );
            EmbedBuilder.getClass().getMethod("setUrl", String.class).invoke(EmbedBuilder,
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".url"),
                            executor,
                            reason,
                            target,
                            server
                    )
            );
            EmbedBuilder.getClass().getMethod("setImage", String.class).invoke(EmbedBuilder,
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".image"),
                            executor,
                            reason,
                            target,
                            server
                    )
            );
            EmbedBuilder.getClass().getMethod("setThumbnail", String.class).invoke(EmbedBuilder,
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".thumbnail"),
                            executor,
                            reason,
                            target,
                            server
                    )
            );
            EmbedBuilder.getClass().getMethod("setAuthor", String.class, String.class).invoke(EmbedBuilder,
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".author.name"),
                            executor,
                            reason,
                            target,
                            server
                    ),
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".author.icon_url"),
                            executor,
                            reason,
                            target,
                            server
                    )
            );
            EmbedBuilder.getClass().getMethod("setFooter", String.class, String.class).invoke(EmbedBuilder,
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".footer.text"),
                            executor,
                            reason,
                            target,
                            server
                    ),
                    replacePlaceholders(
                            config.AsString("discord.embeds."+configPath+".footer.icon_url"),
                            executor,
                            reason,
                            target,
                            server
                    )
            );
            EmbedBuilder.getClass().getMethod("setColor", int.class).invoke(EmbedBuilder, config.AsInt("discord.embeds."+configPath+".color"));
            return EmbedBuilder.getClass().getMethod("build").invoke(EmbedBuilder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
