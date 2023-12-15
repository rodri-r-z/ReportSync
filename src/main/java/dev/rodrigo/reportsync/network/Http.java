package dev.rodrigo.reportsync.network;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Http {
    public static void Download(String fileUrl, String savePath) {
        try {
            URL url2 = new URL(fileUrl);
            Path targetPath = Paths.get(savePath);
            Files.copy(url2.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
