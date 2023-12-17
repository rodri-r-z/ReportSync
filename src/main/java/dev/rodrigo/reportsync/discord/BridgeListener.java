package dev.rodrigo.reportsync.discord;

import dev.rodrigo.reportsync.lib.FancyYAML;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BridgeListener {
    private FancyYAML config;
    private final URLClassLoader loader;
    private final DiscordBridge superClass;

    public BridgeListener(FancyYAML config, URLClassLoader classLoader, DiscordBridge superClass) {
        this.config = config;
        this.loader = classLoader;
        this.superClass = superClass;
    }

    public void setConfig(FancyYAML config) {
        this.config = config;
    }

    public void onSlashCommandInteraction(Object event, Method discordReportBroadcaster, Object parent) {
        try {
            if (!config.AsBoolean("discord.command.enabled")) return;
            final Object member = event.getClass().getMethod("getMember").invoke(event);
            if (member == null) return;
            final Object user = event.getClass().getMethod("getUser").invoke(event);
            if (user == null) return;
            final String executorMention = member.getClass().getMethod("getAsMention").invoke(member).toString();
            final String executorDisplayName = user.getClass().getMethod("getName").invoke(user).toString();
            final Object reason_option = event.getClass().getMethod("getOption", String.class).invoke(event, config.AsString("discord.command.options.reason.name"));
            final String reason = reason_option.getClass().getMethod("getAsString").invoke(reason_option).toString();
            String targetMention = "";
            String targetDisplayName = "";
            Object targetUser = null;
            if (config.AsString("discord.command.options.target.type").equalsIgnoreCase("user")) {
                final Object target_option = event.getClass().getMethod("getOption", String.class).invoke(event, config.AsString("discord.command.options.target.name"));
                targetUser = target_option.getClass().getMethod("getAsMember").invoke(target_option);
                targetMention = targetUser.getClass().getMethod("getAsMention").invoke(targetUser).toString();
                final Object targetDiscordUser = targetUser.getClass().getMethod("getUser").invoke(targetUser);
                targetDisplayName = targetDiscordUser.getClass().getMethod("getName").invoke(targetDiscordUser).toString();
            } else if (config.AsString("discord.command.options.target.type").equalsIgnoreCase("string")) {
                final Object target_option = event.getClass().getMethod("getOption", String.class).invoke(event, config.AsString("discord.command.options.target.name"));
                targetMention = target_option.getClass().getMethod("getAsString").invoke(target_option).toString();
                targetDisplayName = targetMention;
            }
            final Object interaction_channel = event.getClass().getMethod("getChannel").invoke(event);
            if (interaction_channel == null) return;
            final String channel = interaction_channel.getClass().getMethod("getAsMention").invoke(interaction_channel).toString();
            final String channelName = interaction_channel.getClass().getMethod("getName").invoke(interaction_channel).toString();
            final List<Object> embeds = new ArrayList<>();
            final String[] roles = Arrays.stream(((Collection<Object>) member.getClass().getMethod("getRoles").invoke(member)).toArray()).map(a -> {
                try {
                    return a.getClass().getMethod("getId").invoke(a).toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toArray(String[]::new);
            if (Arrays.stream(roles).anyMatch(a -> a.equals(config.AsString("discord.staff_role_id"))) && !config.AsBoolean("overrides.staff_report")) {
                embeds.add(new EmbedBuilder(config, loader).Build(
                        "no_staff_report",
                        executorMention,
                        reason,
                        targetMention,
                        channel
                ));
                final Object reply = event.getClass().getMethod("replyEmbeds", Collection.class).invoke(event, embeds);
                reply.getClass().getMethod("setEphemeral", boolean.class).invoke(reply, true);
                reply.getClass().getMethod("queue").invoke(reply);
                return;
            }
            if (targetUser != null) {
                final String[] staffRoles = Arrays.stream(((Collection<Object>) targetUser.getClass().getMethod("getRoles").invoke(targetUser)).toArray()).map(a -> {
                    try {
                        return a.getClass().getMethod("getId").invoke(a).toString();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toArray(String[]::new);
                if (Arrays.stream(staffRoles).anyMatch(a -> a.equals(config.AsString("discord.staff_role_id"))) && !config.AsBoolean("overrides.report_staff")) {
                    embeds.add(new EmbedBuilder(config, loader).Build(
                            "no_report_staff",
                            executorMention,
                            reason,
                            targetMention,
                            channel
                    ));
                    final Object reply = event.getClass().getMethod("replyEmbeds", Collection.class).invoke(event, embeds);
                    reply.getClass().getMethod("setEphemeral", boolean.class).invoke(reply, true);
                    reply.getClass().getMethod("queue").invoke(reply);
                    return;
                }
                final Object targetRequest = targetUser.getClass().getMethod("getUser").invoke(targetUser);
                if (targetRequest == null) return;
                if (targetMention.equals(executorMention)) {
                    embeds.add(new EmbedBuilder(config, loader).Build(
                            "no_report_self",
                            executorMention,
                            reason,
                            targetMention,
                            channel
                    ));
                    final Object reply = event.getClass().getMethod("replyEmbeds", Collection.class).invoke(event, embeds);
                    reply.getClass().getMethod("setEphemeral", boolean.class).invoke(reply, true);
                    reply.getClass().getMethod("queue").invoke(reply);
                    return;
                }
                final boolean isBot = (boolean) targetRequest.getClass().getMethod("isBot").invoke(targetRequest);
                if (isBot) {
                    embeds.add(new EmbedBuilder(config, loader).Build(
                            "no_report_bots",
                            executorMention,
                            reason,
                            targetMention,
                            channel
                    ));
                    final Object reply = event.getClass().getMethod("replyEmbeds", Collection.class).invoke(event, embeds);
                    reply.getClass().getMethod("setEphemeral", boolean.class).invoke(reply, true);
                    reply.getClass().getMethod("queue").invoke(reply);
                    return;
                }
            }

            embeds.add(new EmbedBuilder(config, loader).Build(
                    "report_sent",
                    executorMention,
                    reason,
                    targetMention,
                    channel
            ));
            superClass.SendReport(
                    executorMention,
                    reason,
                    targetMention,
                    channel
            );
            final Object reply = event.getClass().getMethod("replyEmbeds", Collection.class).invoke(event, embeds);
            reply.getClass().getMethod("setEphemeral", boolean.class).invoke(reply, true);
            reply.getClass().getMethod("queue").invoke(reply);
            discordReportBroadcaster.invoke(
                    parent,
                    EmbedBuilder.replacePlaceholders(
                            String.join(
                            "\n",
                            config.AsStringList("messages.discord_report")
                        ),
                        executorDisplayName,
                        reason,
                        targetDisplayName,
                        channelName
                    ).replaceAll("&", "§")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
