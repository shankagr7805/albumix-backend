package com.shank.AlbumsAPI.util.apputils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AppUtil {

    private static String UPLOAD_DIR;

    // ✅ Safe static injection
    @Value("${file.upload-dir}")
    public void setUploadDir(String uploadDir) {
        AppUtil.UPLOAD_DIR = uploadDir;
        log.info("📂 File upload directory set to: {}", uploadDir);
    }

    // ===============================
    // 📂 Create upload path
    // ===============================
    public static String get_photo_upload_path(String fileName, String folderName, long albumId) throws IOException {
        Path albumPath = Paths.get(
                UPLOAD_DIR,
                String.valueOf(albumId),
                folderName);

        Files.createDirectories(albumPath);

        return albumPath
                .resolve(fileName)
                .toAbsolutePath()
                .toString();
    }

    // ===============================
    // 🗑 Delete photo
    // ===============================
    public static boolean delete_photo_from_path(String fileName, String folderName, long albumId) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, String.valueOf(albumId), folderName, fileName);
            return Files.deleteIfExists(filePath);

        } catch (Exception e) {
            log.error("❌ Failed to delete file: {}", fileName, e);
            return false;
        }
    }

    // ===============================
    // 🖼 Generate thumbnail
    // ===============================
    public static BufferedImage getThumbnail(MultipartFile originalFile, Integer width) throws IOException {
        BufferedImage image = ImageIO.read(originalFile.getInputStream());

        return Scalr.resize(
                image,
                Scalr.Method.AUTOMATIC,
                Scalr.Mode.AUTOMATIC,
                width,
                Scalr.OP_ANTIALIAS);
    }

    // ===============================
    // 📥 Download file
    // ===============================
    public static Resource getFileAsResource(
            long albumId,
            String folderName,
            String fileName) {
        try {
            Path filePath = Paths.get(
                    UPLOAD_DIR,
                    String.valueOf(albumId),
                    folderName,
                    fileName);

            File file = filePath.toFile();
            return file.exists() ? new FileSystemResource(file) : null;

        } catch (Exception e) {
            log.error("❌ Failed to load file as resource: {}", fileName, e);
            return null;
        }
    }
}