package dev.rodrigo.reportsync.bungee;

import dev.rodrigo.reportsync.discord.DiscordBridge;
import dev.rodrigo.reportsync.lib.FancyYAML;
import dev.rodrigo.reportsync.command.Bungee;
import dev.rodrigo.reportsync.network.Http;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReportSyncBungee extends Plugin {
    public FancyYAML config;

    public void discordReportBroadcast(String message) {
        getProxy().getScheduler().runAsync(this, () -> {
            for (ProxiedPlayer plr : getProxy().getPlayers().stream().filter(a -> a.hasPermission("reportsync.staff")).collect(Collectors.toList())) {
                plr.sendMessage(
                        TextComponent.fromLegacyText(
                                message
                        )
                );
            }
        });
    }

    @Override
    public void onEnable() {
        final Logger logger = getLogger();
        final File dataFolder = getDataFolder();
        logger.info("Attempting to start ReportSync.");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.severe("Could not create ReportSync data folder.");
            return;
        }
        try {
            if (!dataFolder.toPath().resolve("config.yml").toFile().exists()) {
                logger.info("Default config file not found. Creating one...");
                final InputStream stream = getResourceAsStream("config.yml");
                if (stream == null) {
                    logger.severe("Could not find ReportSync config.yml.");
                    return;
                }
                Files.copy(stream, dataFolder.toPath().resolve("config.yml"));
                logger.info("Default config file created.");
            }

            if (!dataFolder.toPath().resolve("libs").toFile().exists() && !dataFolder.toPath().resolve("libs").toFile().mkdirs()) {
                logger.severe("Could not create ReportSync libs folder.");
                return;
            }

            if (!dataFolder.toPath().resolve("libs").resolve("JDA-jar.jar").toFile().exists()) {
                logger.info("Java Discord API not found. Attempting to download...");
                Http.Download("https://github.com/discord-jda/JDA/releases/download/v5.0.0-beta.18/JDA-5.0.0-beta.18-withDependencies.jar",
                        dataFolder.toPath().resolve("libs").resolve("JDA-jar.jar").toString());
                logger.info("JDA JAR saved.");
            }

            logger.info("Attempting to read ReportSync config.");
            config = new FancyYAML(dataFolder.toPath().resolve("config.yml"));
            logger.info("Successfully read ReportSync config.");
            logger.info("Looking for discord configuration and enabling.");
            DiscordBridge discordBridge = new DiscordBridge(dataFolder.toPath().resolve("libs").resolve("JDA-jar.jar"), config, getClass().getClassLoader(), this);
            getProxy().getPluginManager().registerCommand(this, new Bungee(this, discordBridge));
            logger.info("ReportSync has enabled successfully.");
        } catch (IOException e) {
            logger.severe("Could not enable ReportSync because: " + e.getMessage());
        }
    }
}
