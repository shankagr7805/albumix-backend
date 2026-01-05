package com.shank.AlbumsAPI.security;

import java.security.KeyPair;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import com.nimbusds.jose.jwk.RSAKey;

public class Jwks {
    private Jwks() {}

    public static RSAKey generatRsa() {
        KeyPair keyPair = KeyGeneratorUtils.generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey) 
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}
