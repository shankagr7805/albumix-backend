package com.shank.AlbumsAPI.payload.albums;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private String description;
    private String download_link;
}
