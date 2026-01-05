package com.shank.AlbumsAPI.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shank.AlbumsAPI.model.Photo;

public interface PhotoRepository extends JpaRepository<Photo , Long>{
    List<Photo> findByAlbum_id(long id) ;
}
