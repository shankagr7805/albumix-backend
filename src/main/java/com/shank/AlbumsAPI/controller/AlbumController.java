package com.shank.AlbumsAPI.controller;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.shank.AlbumsAPI.model.*;
import com.shank.AlbumsAPI.payload.albums.*;
import com.shank.AlbumsAPI.service.AccountService;
import com.shank.AlbumsAPI.service.AlbumService;
import com.shank.AlbumsAPI.service.PhotoService;
import com.shank.AlbumsAPI.service.ThumbnailService;
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
    static final String THUMBNAIL_FOLDER_NAME = "thumbnails";
    static final int THUMBNAIL_WIDTH = 300;

    @Autowired private ThumbnailService thumbnailService;
    @Autowired private AccountService accountService;
    @Autowired private AlbumService albumService;
    @Autowired private PhotoService photoService;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    // ── helpers ──────────────────────────────────────────────
    private String albumsCacheKey(long accountId) {
        return "albums::" + accountId;
    }

    private List<AlbumViewDTO> buildAlbumList(long accountId) {
        List<AlbumViewDTO> albums = new ArrayList<>();
        for (Album album : albumService.findByAccount_id(accountId)) {
            List<PhotoDTO> photos = new ArrayList<>();
            for (Photo photo : photoService.findByAlbumId(album.getId())) {
                String link = "/albums/" + album.getId() + "/photos/" + photo.getId() + "/download-photo";
                photos.add(new PhotoDTO(photo.getId(), photo.getName(), photo.getDescription(), link));
            }
            albums.add(new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), photos));
        }
        return albums;
    }

    private void evictAlbumsCache(long accountId) {
        redisTemplate.delete(albumsCacheKey(accountId));
    }

    // ── GET /albums  (with Redis caching) ────────────────────
    @GetMapping(value = "/albums", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "List of albums")
    @ApiResponse(responseCode = "401", description = "Token missing")
    @ApiResponse(responseCode = "403", description = "Token error")
    @Operation(summary = "List album api")
    @SecurityRequirement(name = "demo-api")
    @SuppressWarnings("unchecked")
    public List<AlbumViewDTO> albums(Authentication authentication) {
        String email = authentication.getName();
        Account account = accountService.findByEmail(email).orElseThrow();
        String cacheKey = albumsCacheKey(account.getId());

        // 1. Try Redis cache
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("CACHE HIT  — key={}", cacheKey);
            return (List<AlbumViewDTO>) cached;
        }

        // 2. Cache miss — hit DB
        log.info("CACHE MISS — key={}", cacheKey);
        List<AlbumViewDTO> albums = buildAlbumList(account.getId());

        // 3. Write to Redis (TTL 10 min)
        redisTemplate.opsForValue().set(cacheKey, albums, 10, TimeUnit.MINUTES);
        return albums;
    }

    // ── GET /albums/{id} ─────────────────────────────────────
    @GetMapping(value = "/albums/{album_id}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Album by id")
    @ApiResponse(responseCode = "401", description = "Token missing")
    @ApiResponse(responseCode = "403", description = "Token error")
    @Operation(summary = "List album by album id")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<AlbumViewDTO> albums_by_id(@PathVariable long album_id, Authentication authentication) {
        String email = authentication.getName();
        Account account = accountService.findByEmail(email).orElseThrow();
        Album album = albumService.findById(album_id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (account.getId() != album.getAccount().getId())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);

        List<PhotoDTO> photos = new ArrayList<>();
        for (Photo photo : photoService.findByAlbumId(album.getId())) {
            String link = "/albums/" + album.getId() + "/photos/" + photo.getId() + "/download-photo";
            photos.add(new PhotoDTO(photo.getId(), photo.getName(), photo.getDescription(), link));
        }
        return ResponseEntity.ok(new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), photos));
    }

    // ── POST /albums/add ─────────────────────────────────────
    @PostMapping(value = "/albums/add", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "400", description = "Please add a valid name and description")
    @ApiResponse(responseCode = "201", description = "Album added")
    @Operation(summary = "Add an album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<AlbumViewDTO> addAlbum(@Valid @RequestBody AlbumPayloadDTO albumPayloadDTO,
                                                  Authentication authentication) {
        try {
            String email = authentication.getName();
            Account account = accountService.findByEmail(email).orElseThrow();
            Album album = new Album();
            album.setName(albumPayloadDTO.getName());
            album.setDescription(albumPayloadDTO.getDescription());
            album.setAccount(account);
            album = albumService.save(album);
            evictAlbumsCache(account.getId());  // invalidate cache on write
            return ResponseEntity.ok(new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), null));
        } catch (Exception e) {
            log.debug(AlbumError.ADD_ALBUM_ERROR.toString() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // ── PUT /albums/{id}/update ───────────────────────────────
    @PutMapping(value = "/albums/{album_id}/update", consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Update an album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<AlbumViewDTO> update_Album(@Valid @RequestBody AlbumPayloadDTO albumPayloadDTO,
                                                      @PathVariable long album_id,
                                                      Authentication authentication) {
        try {
            String email = authentication.getName();
            Account account = accountService.findByEmail(email).orElseThrow();
            Album album = albumService.findById(album_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
            if (account.getId() != album.getAccount().getId())
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);

            album.setName(albumPayloadDTO.getName());
            album.setDescription(albumPayloadDTO.getDescription());
            albumService.save(album);
            evictAlbumsCache(account.getId());

            List<PhotoDTO> photos = new ArrayList<>();
            for (Photo photo : photoService.findByAlbumId(album.getId())) {
                String link = "/albums/" + album.getId() + "/photos/" + photo.getId() + "/download-photo";
                photos.add(new PhotoDTO(photo.getId(), photo.getName(), photo.getDescription(), link));
            }
            return ResponseEntity.ok(new AlbumViewDTO(album.getId(), album.getName(), album.getDescription(), photos));
        } catch (Exception e) {
            log.debug(AlbumError.ADD_ALBUM_ERROR.toString() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // ── PUT /albums/{album_id}/photo/{photo_id}/update ────────
    @PutMapping(value = "/albums/{album_id}/photo/{photo_id}/update", consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Update a photo")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<PhotoViewDTO> update_photo(@Valid @RequestBody PhotoPayloadDTO photoPayloadDTO,
                                                      @PathVariable long album_id,
                                                      @PathVariable long photo_id,
                                                      Authentication authentication) {
        try {
            String email = authentication.getName();
            Account account = accountService.findByEmail(email).orElseThrow();
            Album album = albumService.findById(album_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
            if (account.getId() != album.getAccount().getId())
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);

            Photo photo = photoService.findById(photo_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
            if (photo.getAlbum().getId() != album_id)
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);

            photo.setName(photoPayloadDTO.getName());
            photo.setDescription(photoPayloadDTO.getDescription());
            photoService.save(photo);
            evictAlbumsCache(account.getId());
            return ResponseEntity.ok(new PhotoViewDTO(photo.getId(), photoPayloadDTO.getName(), photoPayloadDTO.getDescription()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // ── DELETE /albums/{id}/photo/{photo_id}/delete ───────────
    @DeleteMapping(value = "/albums/{album_id}/photo/{photo_id}/delete")
    @ApiResponse(responseCode = "202", description = "Photo deleted")
    @Operation(summary = "Delete a photo")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<String> delete_photo(@PathVariable long album_id, @PathVariable long photo_id,
                                                Authentication authentication) {
        try {
            String email = authentication.getName();
            Account account = accountService.findByEmail(email).orElseThrow();
            Album album = albumService.findById(album_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
            if (account.getId() != album.getAccount().getId())
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

            Photo photo = photoService.findById(photo_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
            if (photo.getAlbum().getId() != album_id)
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

            AppUtil.delete_photo_from_path(photo.getFileName(), PHOTOS_FOLDER_NAME, album_id);
            AppUtil.delete_photo_from_path(photo.getFileName(), THUMBNAIL_FOLDER_NAME, album_id);
            photoService.delete(photo);
            evictAlbumsCache(account.getId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Photo deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while deleting the photo.");
        }
    }

    // ── DELETE /albums/{id}/delete ────────────────────────────
    @DeleteMapping(value = "/albums/{album_id}/delete")
    @ApiResponse(responseCode = "202", description = "Album deleted")
    @Operation(summary = "Delete an album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<String> delete_album(@PathVariable long album_id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Account account = accountService.findByEmail(email).orElseThrow();
            Album album = albumService.findById(album_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
            if (account.getId() != album.getAccount().getId())
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);

            for (Photo photo : photoService.findByAlbumId(album.getId())) {
                if (photo == null) continue;
                photoService.delete(photo);
                AppUtil.delete_photo_from_path(photo.getFileName(), PHOTOS_FOLDER_NAME, album_id);
                AppUtil.delete_photo_from_path(photo.getFileName(), THUMBNAIL_FOLDER_NAME, album_id);
            }
            albumService.deleteAlbum(album);
            evictAlbumsCache(account.getId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Album deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while deleting the album.");
        }
    }

    // ── POST /albums/{id}/upload-photos ───────────────────────
    @SuppressWarnings("null")
    @PostMapping(value = "/albums/{album_id}/upload-photos", consumes = {"multipart/form-data"})
    @Operation(summary = "Upload photo in album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<List<HashMap<String, List<?>>>> photos(@RequestPart(required = true) MultipartFile[] files,
                                                                  @PathVariable long album_id,
                                                                  Authentication authentication) {
        String email = authentication.getName();
        Account account = accountService.findByEmail(email).orElseThrow();
        Album album = albumService.findById(album_id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (account.getId() != album.getAccount().getId())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);

        List<PhotoViewDTO> fileNamesWithSuccess = new ArrayList<>();
        List<String> fileNamesWithError = new ArrayList<>();

        Arrays.asList(files).forEach(file -> {
            String contentType = file.getContentType();
            if ("image/png".equals(contentType) || "image/jpg".equals(contentType) || "image/jpeg".equals(contentType)) {
                try {
                    String fileName = file.getOriginalFilename();
                    String generatedString = RandomStringUtils.secure().next(10, true, true);
                    String final_photo_name = generatedString + fileName;
                    String absolute_fileLocation = AppUtil.get_photo_upload_path(final_photo_name, PHOTOS_FOLDER_NAME, album_id);
                    Files.copy(file.getInputStream(), Paths.get(absolute_fileLocation), StandardCopyOption.REPLACE_EXISTING);

                    Photo photo = new Photo();
                    photo.setName(fileName);
                    photo.setFileName(final_photo_name);
                    photo.setOriginalFileName(fileName);
                    photo.setAlbum(album);
                    photoService.save(photo);
                    fileNamesWithSuccess.add(new PhotoViewDTO(photo.getId(), photo.getName(), photo.getDescription()));
                    thumbnailService.generateThumbnail(file, final_photo_name, album_id, THUMBNAIL_WIDTH);
                    evictAlbumsCache(account.getId());
                } catch (Exception e) {
                    log.debug(AlbumError.PHOTO_UPLOAD_ERROR.toString() + ": " + e.getMessage());
                    fileNamesWithError.add(file.getOriginalFilename());
                }
            } else {
                fileNamesWithError.add(file.getOriginalFilename());
            }
        });

        HashMap<String, List<?>> result = new HashMap<>();
        result.put("SUCCESS", fileNamesWithSuccess);
        result.put("ERRORS", fileNamesWithError);
        return ResponseEntity.ok(List.of(result));
    }

    // ── GET download photo / thumbnail ────────────────────────
    @GetMapping("/albums/{album_id}/photos/{photo_id}/download-photo")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> downloadPhoto(@PathVariable long album_id, @PathVariable long photo_id,
                                            Authentication authentication) {
        return downloadFile(album_id, photo_id, PHOTOS_FOLDER_NAME, authentication);
    }

    @GetMapping("/albums/{album_id}/photos/{photo_id}/download-thumbnail")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> downloadThumbnail(@PathVariable long album_id, @PathVariable long photo_id,
                                                Authentication authentication) {
        return downloadFile(album_id, photo_id, THUMBNAIL_FOLDER_NAME, authentication);
    }

    public ResponseEntity<?> downloadFile(long album_id, long photo_id, String folder_name,
                                           Authentication authentication) {
        String email = authentication.getName();
        Account account = accountService.findByEmail(email).orElseThrow();
        Album album = albumService.findById(album_id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (account.getId() != album.getAccount().getId())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Photo photo = photoService.findById(photo_id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (photo.getAlbum().getId() != album_id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Resource resource;
        try {
            resource = AppUtil.getFileAsResource(album_id, folder_name, photo.getFileName());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        if (resource == null || !resource.exists())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");

        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(Path.of(resource.getFile().getAbsolutePath()));
        } catch (IOException ignored) {}
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + photo.getOriginalFileName() + "\"")
                .body(resource);
    }
}
