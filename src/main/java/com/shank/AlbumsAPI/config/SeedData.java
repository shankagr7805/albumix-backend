package com.shank.AlbumsAPI.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.shank.AlbumsAPI.model.Account;
import com.shank.AlbumsAPI.service.AccountService;
import com.shank.AlbumsAPI.util.constants.Authority;

@Component
public class SeedData implements CommandLineRunner {

    @Autowired
    private AccountService accountService;

    @Override
    public void run(String... args) {

        // =========================
        // USER ACCOUNT
        // =========================
        if (accountService.findByEmail("user@user.com").isEmpty()) {
            Account user = new Account();
            user.setEmail("user@user.com");
            user.setPassword("pass987");
            user.setAuthorities(Authority.USER.toString());
            accountService.save(user);
        }

        // =========================
        // ADMIN ACCOUNT
        // =========================
        if (accountService.findByEmail("admin@admin.com").isEmpty()) {
            Account admin = new Account();
            admin.setEmail("admin@admin.com");
            admin.setPassword("pass987");
            admin.setAuthorities(
                Authority.ADMIN.toString() + " " + Authority.USER.toString()
            );
            accountService.save(admin);
        }
    }
}