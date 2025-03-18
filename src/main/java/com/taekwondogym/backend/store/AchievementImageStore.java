package com.taekwondogym.backend.store;

import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class AchievementImageStore {

    private static final String IMAGE_DIRECTORY = "src/main/resources/uploads/images/achievements/";

    public AchievementImageStore() {
        // Ensure the directory exists
        Path path = Paths.get(IMAGE_DIRECTORY);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();  // Handle exception or log error properly
            }
        }
    }
    
    public void setContent(String filename, InputStream inputStream) throws IOException {
        Path path = Paths.get(IMAGE_DIRECTORY + filename);
        Files.copy(inputStream, path);
    }

    public void delete(String filename) throws IOException {
        Path path = Paths.get(IMAGE_DIRECTORY + filename);
        Files.deleteIfExists(path);
    }
}
