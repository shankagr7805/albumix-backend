package com.shank.AlbumsAPI.controller;

import java.nio.file.*;
import java.util.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.shank.AlbumsAPI.model.*;
import com.shank.AlbumsAPI.payload.albums.*;
import com.shank.AlbumsAPI.service.AccountService;
import com.shank.AlbumsAPI.service.AlbumService;
import com.shank.AlbumsAPI.service.PhotoService;
import com.shank.AlbumsAPI.util.apputils.AppUtil;
import com.shank.AlbumsAPI.util.constants.AlbumError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v2")
@Tag(name = "Album Controller", description = "Controller for album and photo management")
@Slf4j
public class AlbumController {

    static final String PHOTOS_FOLDER_NAME = "photos";

    @Autowired
    private AccountService accountService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private Cloudinary cloudinary;

    // ===============================
    // Add Album
    // ===============================
    @PostMapping(value = "/albums/add", consumes = "application/json")
    @Operation(summary = "Add an album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<AlbumViewDTO> addAlbum(
            @Valid @RequestBody AlbumPayloadDTO albumPayloadDTO,
            Authentication authentication
    ) {
        Account account = accountService.findByEmail(authentication.getName()).orElseThrow();

        Album album = new Album();
        album.setName(albumPayloadDTO.getName());
        album.setDescription(albumPayloadDTO.getDescription());
        album.setAccount(account);

        album = albumService.save(album);
        return ResponseEntity.ok(
                new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), new ArrayList<>())
        );
    }

    // ===============================
    // Upload Photos (HYBRID)
    // ===============================
    @PostMapping(value = "/albums/{album_id}/upload-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload photos to album (Hybrid)")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> uploadPhotos(
            @RequestPart MultipartFile[] files,
            @PathVariable long album_id,
            Authentication authentication
    ) {

        Account account = accountService.findByEmail(authentication.getName()).orElseThrow();
        Album album = albumService.findById(album_id).orElseThrow();

        if (account.getId() != album.getAccount().getId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PhotoViewDTO> success = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String originalName = Objects.requireNonNull(file.getOriginalFilename());
                String random = RandomStringUtils.secure().nextAlphanumeric(10);
                String finalName = random + "_" + originalName;

                // 1️⃣ Save original locally
                String path = AppUtil.get_photo_upload_path(finalName, PHOTOS_FOLDER_NAME, album_id);
                Files.copy(file.getInputStream(), Paths.get(path), StandardCopyOption.REPLACE_EXISTING);

                // 2️⃣ Upload thumbnail to Cloudinary
                Map<?, ?> cloudinaryResult = cloudinary.uploader().upload(
                        file.getInputStream(),
                        ObjectUtils.asMap(
                                "folder", "albumix/thumbnails",
                                "transformation", ObjectUtils.asMap(
                                        "width", 300,
                                        "crop", "scale"
                                )
                        )
                );

                // 3️⃣ Save DB
                Photo photo = new Photo();
                photo.setName(originalName);
                photo.setOriginalFileName(originalName);
                photo.setFileName(finalName);
                photo.setCloudinaryPublicId(cloudinaryResult.get("public_id").toString());
                photo.setCloudinaryThumbnailUrl(cloudinaryResult.get("secure_url").toString());
                photo.setAlbum(album);

                photoService.save(photo);
                success.add(new PhotoViewDTO(photo.getId(), photo.getName(), photo.getDescription()));

            } catch (Exception e) {
                log.error(AlbumError.PHOTO_UPLOAD_ERROR.toString(), e);
                errors.add(file.getOriginalFilename());
            }
        }

        return ResponseEntity.ok(Map.of(
                "SUCCESS", success,
                "ERRORS", errors
        ));
    }

    // ===============================
    // Get Albums
    // ===============================
    @GetMapping("/albums")
    @Operation(summary = "List albums")
    @SecurityRequirement(name = "demo-api")
    public List<AlbumViewDTO> getAlbums(Authentication authentication) {

        Account account = accountService.findByEmail(authentication.getName()).orElseThrow();
        List<AlbumViewDTO> response = new ArrayList<>();

        for (Album album : albumService.findByAccount_id(account.getId())) {
            List<PhotoDTO> photos = new ArrayList<>();

            for (Photo photo : photoService.findByAlbumId(album.getId())) {
                photos.add(new PhotoDTO(
                        photo.getId(),
                        photo.getName(),
                        photo.getDescription(),
                        photo.getCloudinaryThumbnailUrl()
                ));
            }

            response.add(new AlbumViewDTO(
                    album.getId(),
                    album.getName(),
                    album.getDescription(),
                    photos
            ));
        }
        return response;
    }

    @GetMapping(value = "/albums/{album_id}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "List of albums")
    @ApiResponse(responseCode = "401", description = "Token missing")
    @ApiResponse(responseCode = "403", description = "Token error")
    @Operation(summary = "List album by album id")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<AlbumViewDTO> albums_by_id(@PathVariable long album_id, Authentication authentication) {
        String email = authentication.getName();
        Optional<Account> optionalAccount = accountService.findByEmail(email);
        Account account = optionalAccount.get();
        Optional<Album> optionalAlbum = albumService.findById(album_id);
        Album album;
        if(optionalAlbum.isPresent()) {
            album = optionalAlbum.get();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        if(account.getId() != album.getAccount().getId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        List<PhotoDTO> photos = new ArrayList<>();
        for (Photo photo : photoService.findByAlbumId(album.getId())) {
            String link = "/albums/"+album.getId()+"/photos/"+photo.getId()+"/download-photo"; 
            photos.add(new PhotoDTO(photo.getId(), photo.getName(), photo.getDescription(), link));
        }
        AlbumViewDTO albumViewDTO = new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), photos);
        
        return ResponseEntity.ok(albumViewDTO);
    }


    // ===============================
    // Download Original Photo
    // ===============================
    @SuppressWarnings("null")
    @GetMapping("/albums/{album_id}/photos/{photo_id}/download-photo")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> downloadPhoto(
            @PathVariable long album_id,
            @PathVariable long photo_id,
            Authentication authentication
    ) {

        Account account = accountService.findByEmail(authentication.getName()).orElseThrow();
        Album album = albumService.findById(album_id).orElseThrow();
        Photo photo = photoService.findById(photo_id).orElseThrow();

        if (account.getId() != album.getAccount().getId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Resource resource = AppUtil.getFileAsResource(album_id, PHOTOS_FOLDER_NAME, photo.getFileName());
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + photo.getOriginalFileName() + "\"")
                .body(resource);
    }

    // ===============================
    // Delete Photo (Local + Cloudinary)
    // ===============================
    @DeleteMapping("/albums/{album_id}/photo/{photo_id}/delete")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> deletePhoto(
            @PathVariable long album_id,
            @PathVariable long photo_id,
            Authentication authentication
    ) throws Exception {

        Account account = accountService.findByEmail(authentication.getName()).orElseThrow();
        Album album = albumService.findById(album_id).orElseThrow();
        Photo photo = photoService.findById(photo_id).orElseThrow();

        if (account.getId() != album.getAccount().getId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AppUtil.delete_photo_from_path(photo.getFileName(), PHOTOS_FOLDER_NAME, album_id);
        cloudinary.uploader().destroy(photo.getCloudinaryPublicId(), ObjectUtils.emptyMap());
        photoService.delete(photo);

        return ResponseEntity.ok("Photo deleted");
    }
}