package dev.rodrigo.reportsync.spigot;

import dev.rodrigo.FancyYAML;
import dev.rodrigo.reportsync.command.Spigot;
import dev.rodrigo.reportsync.network.Http;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class SpigotPlugin extends JavaPlugin {
    public FancyYAML config;

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
                saveResource("config.yml", false);
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
            DiscordBridgetSpigot discordBridge = new DiscordBridgetSpigot(dataFolder.toPath().resolve("libs").resolve("JDA-jar.jar"), config, this);
            getCommand("report").setExecutor(new Spigot(this, discordBridge));
            logger.info("ReportSync has enabled successfully.");
        } catch (IOException e) {
            logger.severe("Could not enable ReportSync because: " + e.getMessage());
        }
    }
}
