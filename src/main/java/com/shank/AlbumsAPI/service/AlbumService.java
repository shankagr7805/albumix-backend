package com.shank.AlbumsAPI.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.shank.AlbumsAPI.model.Album;
import com.shank.AlbumsAPI.repository.AlbumRepository;

@Service
public class AlbumService {
    @Autowired
    private AlbumRepository albumRepository;

    public Album save(Album album) {
        if (album == null) {
            throw new IllegalArgumentException("Album cannot be null");
        }
        return albumRepository.save(album);
    }

    public List<Album> findByAccount_id(long id) {
        return albumRepository.findByAccount_id(id);
    }

    public Optional<Album> findById(long id) {
        return albumRepository.findById(id);
    }

    public void deleteAlbum(@NonNull Album album) {
        albumRepository.delete(album);
    }
}
