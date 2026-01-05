package com.shank.AlbumsAPI.util.constants;

public enum Authority {
    READ,
    WRITE,
    UPDATE,
    USER,    //& Can delete & update self object. Reads anything
    ADMIN   //& Can read , delete and update any object.
}
