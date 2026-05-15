package com.shank.AlbumsAPI.service;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import com.shank.AlbumsAPI.util.apputils.AppUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

@Service
public class ThumbnailService {

    @Async
    public void generateThumbnail(
            MultipartFile file,
            String final_photo_name,
            long album_id,
            int thumbnailWidth) {

        try {

            BufferedImage thumbImg = AppUtil.getThumbnail(file, thumbnailWidth);

            File thumbnail_location = new File(
                    AppUtil.get_photo_upload_path(
                            final_photo_name,
                            "thumbnails",
                            album_id));

            ImageIO.write(
                    thumbImg,
                    file.getContentType().split("/")[1],
                    thumbnail_location);

            System.out.println("Thumbnail generated asynchronously");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
