package com.shank.AlbumsAPI.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.shank.AlbumsAPI.model.Photo;
import com.shank.AlbumsAPI.repository.PhotoRepository;

@Service
public class PhotoService {
    
    @Autowired
    private PhotoRepository photoRepository;

    public Photo save(Photo photo) {
        if (photo == null) {
            throw new IllegalArgumentException("Photo cannot be null");
        }
        return photoRepository.save(photo);
    }

    public Optional<Photo> findById(long id) {
        return photoRepository.findById(id);
    }

    public List<Photo> findByAlbumId(long id) {
        return photoRepository.findByAlbum_id(id);
    }

    public void delete(@NonNull Photo photo) {
        photoRepository.delete(photo);
    }
}
