package com.shank.AlbumsAPI.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v2/cloudinary")
@SecurityRequirement(name = "demo-api") // 🔐 JWT required in Swagger
public class CloudinaryController {

    @Autowired
    private Cloudinary cloudinary;

    // ===============================
    // Upload Image
    // ===============================
    @PostMapping(
        value = "/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
        summary = "Upload image to Cloudinary",
        description = "Uploads an image file to Cloudinary (JWT protected)"
    )
    public ResponseEntity<?> uploadImage(
            @Parameter(
                description = "Image file to upload",
                required = true,
                content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")
                )
            )
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "albumix_poc",
                        "resource_type", "image",
                        "uploaded_by", authentication.getName()
                )
        );

        return ResponseEntity.ok(uploadResult);
    }

    // ===============================
    // Delete Image
    // ===============================
    @DeleteMapping("/delete")
    @Operation(
        summary = "Delete image from Cloudinary",
        description = "Deletes image using Cloudinary publicId (JWT protected)"
    )
    public ResponseEntity<?> deleteImage(
            @RequestParam("publicId") String publicId,
            Authentication authentication
    ) throws IOException {

        if (publicId == null || publicId.isBlank()) {
            return ResponseEntity.badRequest().body("publicId is required");
        }

        Map<?, ?> result = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<?> listImages() throws Exception {

        Map<?, ?> result = cloudinary.api().resources(
            ObjectUtils.asMap(
                "type", "upload",
                "prefix", "albumix_poc/",
                "max_results", 50
            )
        );

        return ResponseEntity.ok(result.get("resources"));
    }
}