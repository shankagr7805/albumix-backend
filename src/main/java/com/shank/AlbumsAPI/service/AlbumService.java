package com.shank.AlbumsAPI.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.shank.AlbumsAPI.model.Album;
import com.shank.AlbumsAPI.repository.AlbumRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;

@Service
public class AlbumService {
    @Autowired
    private AlbumRepository albumRepository;

    @CacheEvict(value = "albums", allEntries = true)
    public Album save(Album album) {
        if (album == null) {
            throw new IllegalArgumentException("Album cannot be null");
        }
        return albumRepository.save(album);
    }

    @Cacheable(value = "albums", key = "#id")
    public List<Album> findByAccount_id(long id) {
        return albumRepository.findByAccount_id(id);
    }

    public Optional<Album> findById(long id) {
        return albumRepository.findById(id);
    }

    @Caching(evict = {
        @CacheEvict(value = "albums", allEntries = true),
        @CacheEvict(value = "album", allEntries = true)
    })
    public void deleteAlbum(@NonNull Album album) {
        albumRepository.delete(album);
    }
}
