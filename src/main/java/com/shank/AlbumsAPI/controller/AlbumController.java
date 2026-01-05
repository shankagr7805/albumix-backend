package com.shank.AlbumsAPI.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
@CrossOrigin(origins = "http://localhost:3000/" , maxAge = 3600) 
@Tag(name = "Album Controller", description = "Controller for album and photo management")
@Slf4j
public class AlbumController {

    static final String PHOTOS_FOLDER_NAME = "photos";
    static final String THUMBNAIL_FOLDER_NAME = "thumbnails";
    static final int THUMBNAIL_WIDTH = 300;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private PhotoService photoService;

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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            
            album.setName(albumPayloadDTO.getName());
            album.setDescription(albumPayloadDTO.getDescription());
            album = albumService.save(album);
            List<PhotoDTO> photos = new ArrayList<>();
            for(Photo photo : photoService.findByAlbumId(album.getId())) {
                String link = "/albums/"+album.getId()+"/photos/"+photo.getId()+"/download-photo"; 
                photos.add(new PhotoDTO(photo.getId(), photo.getName(), photo.getDescription(), photo.getFileName(), link));
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to delete photo");
            }
            Optional<Photo> optionalPhoto = photoService.findById(photo_id);
            if(optionalPhoto.isPresent()) {
                Photo photo = optionalPhoto.get();
                if(photo.getAlbum().getId() != album_id) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("An error occurred while deleting the photo.");
                }

                AppUtil.delete_photo_from_path(photo.getFileName(), PHOTOS_FOLDER_NAME, album_id);
                AppUtil.delete_photo_from_path(photo.getFileName(), THUMBNAIL_FOLDER_NAME, album_id);
                photoService.delete(photo);  

                return ResponseEntity.status(HttpStatus.ACCEPTED).body("Photo deleted successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to delete photo");
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
                String link = "/albums/"+album.getId()+"/photos/"+photo.getId()+"/download-photo"; 
                photos.add(new PhotoDTO(photo.getId(), photo.getName(), photo.getDescription(), photo.getFileName(), link));
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        if(account.getId() != album.getAccount().getId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        List<PhotoDTO> photos = new ArrayList<>();
        for (Photo photo : photoService.findByAlbumId(album.getId())) {
            String link = "/albums/"+album.getId()+"/photos/"+photo.getId()+"/download-photo"; 
            photos.add(new PhotoDTO(photo.getId(), photo.getName(), photo.getDescription(), photo.getFileName(), link));
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            for(Photo photo : photoService.findByAlbumId(album.getId())) {
                if(photo == null) continue;
                photoService.delete(photo);  
                AppUtil.delete_photo_from_path(photo.getFileName(), PHOTOS_FOLDER_NAME, album_id);
                AppUtil.delete_photo_from_path(photo.getFileName(), THUMBNAIL_FOLDER_NAME, album_id);
            }
            albumService.deleteAlbum(album);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Album deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while deleting the album.");
        }
    }

    @SuppressWarnings("null")
    @PostMapping(value = "/albums/{album_id}/upload-photos", consumes = { "multipart/form-data" })
    @Operation(summary = "Upload photo in album")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<List<HashMap<String, List<?>>>> photos(@RequestPart(required = true) MultipartFile[] files, @PathVariable long album_id, Authentication authentication) {
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        List<PhotoViewDTO> fileNamesWithSuccess = new ArrayList<>();
        List<String> fileNamesWithError = new ArrayList<>();

        Arrays.asList(files).stream().forEach(file -> {
            String contentType = file.getContentType();
            if (contentType.equals("image/png") ||
                    contentType.equals("image/jpg") ||
                    contentType.equals("image/jpeg")) {

                int length = 10;
                boolean useLetters = true;
                boolean useNumbers = true;

                try {
                    String fileName = file.getOriginalFilename();
                    String generatedString = RandomStringUtils.secure().next(length, useLetters, useNumbers);
                    String final_photo_name = generatedString + fileName;
                    String absolute_fileLocation = AppUtil.get_photo_upload_path(final_photo_name, PHOTOS_FOLDER_NAME, album_id);
                    Path path = Paths.get(absolute_fileLocation);
                    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                    Photo photo = new Photo();
                    photo.setName(fileName);
                    photo.setFileName(final_photo_name);
                    photo.setOriginalFileName(fileName);
                    photo.setAlbum(album);
                    photoService.save(photo);

                    PhotoViewDTO photoViewDTO = new PhotoViewDTO(photo.getId(), photo.getName(), photo.getDescription());
                    fileNamesWithSuccess.add(photoViewDTO);
                    BufferedImage thumbImg = AppUtil.getThumbnail(file, THUMBNAIL_WIDTH);
                    File thumbnail_location = new File(AppUtil.get_photo_upload_path(final_photo_name, THUMBNAIL_FOLDER_NAME, album_id));
                    ImageIO.write(thumbImg, file.getContentType().split("/")[1], thumbnail_location);

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

        List<HashMap<String, List<?>>> response = new ArrayList<>();
        response.add(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/albums/{album_id}/photos/{photo_id}/download-photo")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> downloadPhoto(@PathVariable("album_id") long album_id, @PathVariable("photo_id") long photo_id, Authentication authentication) {
        return downloadFile(album_id, photo_id, PHOTOS_FOLDER_NAME, authentication);
    }

    @GetMapping("/albums/{album_id}/photos/{photo_id}/download-thumbnail")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<?> downloadThumbnail(@PathVariable("album_id") long album_id, @PathVariable("photo_id") long photo_id, Authentication authentication) {
        return downloadFile(album_id, photo_id, THUMBNAIL_FOLDER_NAME, authentication);
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
