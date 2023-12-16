package dev.rodrigo.reportsync;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.rodrigo.reportsync.command.Velocity;
import dev.rodrigo.reportsync.discord.DiscordBridge;
import dev.rodrigo.reportsync.lib.FancyYAML;
import dev.rodrigo.reportsync.network.Http;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(
        id = "reportsync",
        name = "ReportSync",
        version = "1.2"
)
public class ReportSync {
    public final Logger logger;
    public final ProxyServer proxyServer;
    public final Path dataFolder;
    public FancyYAML config;
    
    @Inject
    public ReportSync(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataFolder) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.dataFolder = dataFolder;
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Attempting to start ReportSync.");
        if (!dataFolder.toFile().exists() && !dataFolder.toFile().mkdirs()) {
            logger.error("Could not create ReportSync data folder.");
            return;
        }
        try {
            if (!dataFolder.resolve("config.yml").toFile().exists()) {
                logger.info("Default config file not found. Creating one...");
                final InputStream stream = getClass().getClassLoader().getResourceAsStream("config.yml");
                if (stream == null) {
                    logger.error("Could not find ReportSync config.yml.");
                    return;
                }
                Files.copy(stream, dataFolder.resolve("config.yml"));
                logger.info("Default config file created.");
            }

            if (!dataFolder.resolve("libs").toFile().exists() && !dataFolder.resolve("libs").toFile().mkdirs()) {
                logger.error("Could not create ReportSync libs folder.");
                return;
            }

            if (!dataFolder.resolve("libs").resolve("JDA-jar.jar").toFile().exists()) {
                logger.info("Java Discord API not found. Attempting to download...");
                Http.Download("https://github.com/discord-jda/JDA/releases/download/v5.0.0-beta.18/JDA-5.0.0-beta.18-withDependencies.jar",
                        dataFolder.resolve("libs").resolve("JDA-jar.jar").toString());
                logger.info("JDA JAR saved.");
            }

            logger.info("Attempting to read ReportSync config.");
            config = new FancyYAML(dataFolder.resolve("config.yml"));
            logger.info("Successfully read ReportSync config.");
            logger.info("Looking for discord configuration and enabling.");
            DiscordBridge discordBridge = new DiscordBridge(dataFolder.resolve("libs").resolve("JDA-jar.jar"), config, this);
            proxyServer.getCommandManager().register("report", new Velocity(this, discordBridge));
            logger.info("ReportSync has enabled successfully.");
        } catch (IOException e) {
            logger.error("Could not enable ReportSync because: " + e.getMessage());
        }
    }
}
