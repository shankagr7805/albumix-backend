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

    @PostMapping(value = "/albums/add", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "400", description = "Please add a valid name to the description")
    @ApiResponse(responseCode = "201", description = "Album added")
    @Operation(summary = "Add an album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<AlbumViewDTO> addAlbum(@Valid @RequestBody AlbumPayloadDTO albumPayloadDTO, Authentication authentication) {
        try {
            Album album = new Album();
            album.setName(albumPayloadDTO.getName());
            album.setDescription(albumPayloadDTO.getDescription());

            String email = authentication.getName();
            Optional<Account> optionalAccount = accountService.findByEmail(email);
            Account account = optionalAccount.get();
            album.setAccount(account);
            album = albumService.save(album);

            AlbumViewDTO albumViewDTO = new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), null);
            return ResponseEntity.ok(albumViewDTO);

        } catch (Exception e) {
            log.debug(AlbumError.ADD_ALBUM_ERROR.toString() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping(value = "/albums/{album_id}/update", consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "400", description = "Please add a valid name to the description")
    @ApiResponse(responseCode = "201", description = "Album updated")
    @Operation(summary = "Update an album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<AlbumViewDTO> update_Album(@Valid @RequestBody AlbumPayloadDTO albumPayloadDTO, @PathVariable long album_id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Account> optionalAccount = accountService.findByEmail(email);
            Account account = optionalAccount.get();

            Optional<Album> optionalAlbum = albumService.findById(album_id);
            Album album;
            if (optionalAlbum.isPresent()) {
                album = optionalAlbum.get();
                if (account.getId() != album.getAccount().getId()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                } 
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            
            album.setName(albumPayloadDTO.getName());
            album.setDescription(albumPayloadDTO.getDescription());
            album = albumService.save(album);
            List<PhotoDTO> photos = new ArrayList<>();
            for(Photo photo : photoService.findByAlbumId(album.getId())) {
                photos.add(new PhotoDTO(
                    photo.getId(),
                    photo.getName(),
                    photo.getDescription(),
                    photo.getCloudinaryThumbnailUrl()
                ));
            }
            
            AlbumViewDTO albumViewDTO = new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), photos);
            return ResponseEntity.ok(albumViewDTO);

        } catch (Exception e) {
            log.debug(AlbumError.ADD_ALBUM_ERROR.toString() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping(value = "/albums/{album_id}/photo/{photo_id}/update", consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "400", description = "Please add a valid name to the description")
    @ApiResponse(responseCode = "201", description = "Album updated")
    @Operation(summary = "Update a photo")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<PhotoViewDTO> update_photo(@Valid @RequestBody PhotoPayloadDTO photoPayloadDTO, @PathVariable long album_id, @PathVariable long photo_id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Account> optionalAccount = accountService.findByEmail(email);
            Account account = optionalAccount.get();

            Optional<Album> optionalAlbum = albumService.findById(album_id);
            Album album;
            if (optionalAlbum.isPresent()) {
                album = optionalAlbum.get();
                if (account.getId() != album.getAccount().getId()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                } 
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            Optional<Photo> optionalPhoto = photoService.findById(photo_id);
            if(optionalPhoto.isPresent()) {
                Photo photo = optionalPhoto.get();
                if(photo.getAlbum().getId() != album_id) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                }
                photo.setName(photoPayloadDTO.getName());
                photo.setDescription(photoPayloadDTO.getDescription());
                photoService.save(photo);
                PhotoViewDTO photoViewDTO = new PhotoViewDTO(photo.getId(), photoPayloadDTO.getName(), photoPayloadDTO.getDescription());
                return ResponseEntity.ok(photoViewDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping(value = "/albums/{album_id}/photo/{photo_id}/delete")
    @ApiResponse(responseCode = "202", description = "Photo deleted")
    @Operation(summary = "Delete a photo")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<String> delete_photo(@PathVariable long album_id, @PathVariable long photo_id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Account> optionalAccount = accountService.findByEmail(email);
            Account account = optionalAccount.get();

            Optional<Album> optionalAlbum = albumService.findById(album_id);
            Album album;
            if (optionalAlbum.isPresent()) {
                album = optionalAlbum.get();
                if (account.getId() != album.getAccount().getId()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("An error occurred while deleting the photo.");
                } 
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Failed to delete photo");
            }
            Optional<Photo> optionalPhoto = photoService.findById(photo_id);
            if(optionalPhoto.isPresent()) {
                Photo photo = optionalPhoto.get();
                if(photo.getAlbum().getId() != album_id) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("An error occurred while deleting the photo.");
                }

                AppUtil.delete_photo_from_path(photo.getFileName(), PHOTOS_FOLDER_NAME, album_id);
                if (photo.getCloudinaryPublicId() != null) {
                    cloudinary.uploader().destroy(
                        photo.getCloudinaryPublicId(),
                        ObjectUtils.emptyMap()
                    );
                }

                photoService.delete(photo);  

                return ResponseEntity.status(HttpStatus.ACCEPTED).body("Photo deleted successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Failed to delete photo");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while deleting the photo.");
        }
    }

    @GetMapping(value = "/albums", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "List of users")
    @ApiResponse(responseCode = "401", description = "Token missing")
    @ApiResponse(responseCode = "403", description = "Token error")
    @Operation(summary = "List album api")
    @SecurityRequirement(name = "demo-api")
    public List<AlbumViewDTO> albums(Authentication authentication) {
        String email = authentication.getName();
        Optional<Account> optionalAccount = accountService.findByEmail(email);
        Account account = optionalAccount.get();
        List<AlbumViewDTO> albums = new ArrayList<>();
        for (Album album : albumService.findByAccount_id(account.getId())) {
            List<PhotoDTO> photos = new ArrayList<>();
            for(Photo photo : photoService.findByAlbumId(album.getId())) {
                photos.add(new PhotoDTO(
                    photo.getId(),
                    photo.getName(),
                    photo.getDescription(),
                    photo.getCloudinaryThumbnailUrl()
                ));
            }
            albums.add(new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), photos));
        }
        return albums; 
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if(account.getId() != album.getAccount().getId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        List<PhotoDTO> photos = new ArrayList<>();
        for (Photo photo : photoService.findByAlbumId(album.getId())) {
            photos.add(new PhotoDTO(
                photo.getId(),
                photo.getName(),
                photo.getDescription(),
                photo.getCloudinaryThumbnailUrl()
            ));
        }
        AlbumViewDTO albumViewDTO = new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), photos);
        
        return ResponseEntity.ok(albumViewDTO);
    }

    @DeleteMapping(value = "/albums/{album_id}/delete")
    @ApiResponse(responseCode = "202", description = "Album deleted")
    @Operation(summary = "Delete an album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<String> delete_album(@PathVariable long album_id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Account> optionalAccount = accountService.findByEmail(email);
            Account account = optionalAccount.get();

            Optional<Album> optionalAlbum = albumService.findById(album_id);
            Album album;
            if (optionalAlbum.isPresent()) {
                album = optionalAlbum.get();
                if (account.getId() != album.getAccount().getId()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                } 
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            for(Photo photo : photoService.findByAlbumId(album.getId())) {
                if(photo == null) continue;
                AppUtil.delete_photo_from_path(photo.getFileName(), PHOTOS_FOLDER_NAME, album_id);
                if (photo.getCloudinaryPublicId() != null) {
                    cloudinary.uploader().destroy(
                        photo.getCloudinaryPublicId(),
                        ObjectUtils.emptyMap()
                    );
                }
                photoService.delete(photo);
            }
            albumService.deleteAlbum(album);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Album deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while deleting the album.");
        }
    }

    @SuppressWarnings("null")
    @PostMapping(value = "/albums/{album_id}/upload-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload photo in album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> uploadPhotos(
            @RequestPart MultipartFile[] files,
            @PathVariable long album_id,
            Authentication auth) {

        Account account = accountService.findByEmail(auth.getName()).orElseThrow();
        Album album = albumService.findById(album_id).orElseThrow();

        if (album.getAccount().getId() != account.getId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PhotoViewDTO> success = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                    errors.add(file.getOriginalFilename());
                    continue;
                }

                String originalName = Objects.requireNonNull(file.getOriginalFilename());
                String random = RandomStringUtils.secure().nextAlphanumeric(10);
                String finalName = random + "_" + originalName;

                // ✅ READ FILE ONCE
                byte[] bytes = file.getBytes();

                // 1️⃣ SAVE ORIGINAL LOCALLY
                String path = AppUtil.get_photo_upload_path(finalName, PHOTOS_FOLDER_NAME, album_id);
                Files.write(
                    Paths.get(path),
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                );

                // 2️⃣ UPLOAD THUMBNAIL TO CLOUDINARY
                Map<?, ?> cloudinaryResult = cloudinary.uploader().upload(
                        bytes,
                        ObjectUtils.asMap(
                            "folder", "albumix/thumbnails/" + album_id,
                            "transformation", ObjectUtils.asMap(
                                "width", 300,
                                "crop", "scale"
                            )
                        )
                );

                // 3️⃣ SAVE DB
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

    @GetMapping("/albums/{album_id}/photos/{photo_id}/download-photo")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> downloadPhoto(@PathVariable("album_id") long album_id, @PathVariable("photo_id") long photo_id, Authentication authentication) {
        return downloadFile(album_id, photo_id, PHOTOS_FOLDER_NAME, authentication);
    }

    public ResponseEntity<?> downloadFile(long album_id, long photo_id, String folder_name, Authentication authentication) {
        String email = authentication.getName();
        Optional<Account> optionalAccount = accountService.findByEmail(email);
        Account account = optionalAccount.get();

        Optional<Album> optionalAlbum = albumService.findById(album_id);
        Album album;
        if (optionalAlbum.isPresent()) {
            album = optionalAlbum.get();
            if (account.getId() != album.getAccount().getId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Optional<Photo> optionalPhoto = photoService.findById(photo_id);
        if (optionalPhoto.isPresent()) {
            Photo photo = optionalPhoto.get();

            if(photo.getAlbum().getId() != album_id) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            Resource resource = null;
            try {
                resource = AppUtil.getFileAsResource(album_id, folder_name, photo.getFileName());
            } catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
            if (resource == null) {
                return new ResponseEntity<>("File not found", HttpStatus.NOT_FOUND);
            }
            String contentType = "application/octet-stream";
            String headerValue = "attachment; filename=\"" + photo.getOriginalFileName() + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                    .body(resource);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

}