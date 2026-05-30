package com.shank.AlbumsAPI.service;

import org.springframework.stereotype.Service;
import org.imgscalr.Scalr;
import org.springframework.scheduling.annotation.Async;

import com.shank.AlbumsAPI.util.apputils.AppUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

@Service
public class ThumbnailService {

    @Async
    public void generateThumbnail(
            byte[] file,
            String contentType,
            String final_photo_name,
            long album_id,
            int thumbnailWidth) {

        try {

            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(file));
            BufferedImage thumbImg = Scalr.resize(image,
                    Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC,
                    thumbnailWidth, Scalr.OP_ANTIALIAS);

            File thumbnail_location = new File(
                    AppUtil.get_photo_upload_path(final_photo_name, "thumbnails", album_id));

            ImageIO.write(thumbImg, contentType.split("/")[1], thumbnail_location);

            System.out.println("Thumbnail generated asynchronously");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
