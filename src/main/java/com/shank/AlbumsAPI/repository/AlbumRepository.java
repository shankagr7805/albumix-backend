package com.shank.AlbumsAPI.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shank.AlbumsAPI.model.Album;

public interface AlbumRepository extends JpaRepository<Album , Long>{
    List<Album> findByAccount_id(long id);
}
