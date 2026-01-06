package com.shank.AlbumsAPI.controller;

import org.springframework.web.bind.annotation.RestController;

import com.shank.AlbumsAPI.model.Account;
import com.shank.AlbumsAPI.payload.auth.AccountDTO;
import com.shank.AlbumsAPI.payload.auth.AccountViewDTO;
import com.shank.AlbumsAPI.payload.auth.AuthoritiesDTO;
import com.shank.AlbumsAPI.payload.auth.PasswordDTO;
import com.shank.AlbumsAPI.payload.auth.ProfileDTO;
import com.shank.AlbumsAPI.payload.auth.TokenDTO;
import com.shank.AlbumsAPI.payload.auth.UserLoginDTO;
import com.shank.AlbumsAPI.service.AccountService;
import com.shank.AlbumsAPI.service.TokenService;
import com.shank.AlbumsAPI.util.constants.AccountError;
import com.shank.AlbumsAPI.util.constants.AccountSuccess;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/v2/auth")
@Tag(name = "Auth Controller" , description = "Controller for account management")
@Slf4j
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenService tokenService;

    @Autowired
    private AccountService accountService;

    public AuthController(TokenService tokenService, AuthenticationManager authenticationManager) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<TokenDTO> token(@Valid @RequestBody UserLoginDTO userLogin) throws AuthenticationException{

        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(userLogin.getEmail(), userLogin.getPassword()));
            return ResponseEntity.ok(new TokenDTO(tokenService.generateToken(authentication)));

        } catch (Exception e) {
            log.debug(AccountError.TOKEN_GENERATION_ERROR.toString() + ": " + e.getMessage());
            return new ResponseEntity<>(new TokenDTO(null) , HttpStatus.BAD_REQUEST);
        }
        
    }
    
    @PostMapping(value = "/users/add" , produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "401" , description = "Please enter a valid email and password length between 6 to 20 characters")
    @ApiResponse(responseCode = "200" , description = "Account added")
    @Operation(summary = "Add a new user")
    public ResponseEntity<String> addUser(@Valid @RequestBody AccountDTO accountDTO) {

        try {
            Account account = new Account();
            account.setEmail(accountDTO.getEmail());
            account.setPassword(accountDTO.getPassword());
            accountService.save(account);
            return ResponseEntity.ok(AccountSuccess.ACCOUNT_ADDED.toString());

        } catch (Exception e) {
            log.debug(AccountError.ADD_ACCOUNT_ERROR.toString()+": "+e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping(value = "/users" , produces = "application/json")
    @ApiResponse(responseCode = "200" , description = "List of users")
    @ApiResponse(responseCode = "401" , description = "Token missing")
    @ApiResponse(responseCode = "403" , description = "Token error")
    @Operation(summary = "List user api")
    @SecurityRequirement(name = "demo-api")
    public List<AccountViewDTO> Users() {
        List<AccountViewDTO> accounts = new ArrayList<>();

        for(Account account : accountService.findallAccounts()) {
            accounts.add(new AccountViewDTO(account.getId(), account.getEmail(), account.getAuthorities()));
        }
        return accounts;
    }

    @PutMapping(value = "/users/{user_id}/update-authorities" , produces = "application/json" , consumes = "application/json")
    @ApiResponse(responseCode = "200" , description = "Update authorities")
    @ApiResponse(responseCode = "401" , description = "Token missing")
    @ApiResponse(responseCode = "400" , description = "Invalid User")
    @ApiResponse(responseCode = "403" , description = "Token error")
    @Operation(summary = "Update authorities")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<AccountViewDTO> update_auth(@Valid @RequestBody AuthoritiesDTO authoritiesDTO, @PathVariable long user_id) {
        Optional<Account> optionalAccount = accountService.findByID(user_id);

        if(optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            account.setAuthorities(authoritiesDTO.getAuthorities());
            accountService.save(account);
            AccountViewDTO accountViewDTO = new AccountViewDTO(account.getId(), account.getEmail(), account.getAuthorities());
            return ResponseEntity.ok(accountViewDTO);
        }
        return new ResponseEntity<AccountViewDTO>(new AccountViewDTO() , HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value = "/profile" , produces = "application/json")
    @ApiResponse(responseCode = "200" , description = "View profile")
    @ApiResponse(responseCode = "401" , description = "Token missing")
    @ApiResponse(responseCode = "403" , description = "Token error")
    @Operation(summary = "View profile")
    @SecurityRequirement(name = "demo-api")
    public ProfileDTO profile(Authentication authentication) {

        String email = authentication.getName();
        Optional<Account> optionalAccount = accountService.findByEmail(email);
        Account account = optionalAccount.get();
        ProfileDTO profileDTO = new ProfileDTO(account.getId(), account.getEmail(), account.getAuthorities());
        return profileDTO;

    }

    @PutMapping(value = "/profile/update-password" , produces = "application/json" , consumes = "application/json")
    @ApiResponse(responseCode = "200" , description = "Update password")
    @ApiResponse(responseCode = "401" , description = "Token missing")
    @ApiResponse(responseCode = "403" , description = "Token error")
    @Operation(summary = "Update password")
    @SecurityRequirement(name = "demo-api")
    public AccountViewDTO update_password(@Valid @RequestBody PasswordDTO passwordDTO , Authentication authentication) {
        String email = authentication.getName();
        Optional<Account> optionalAccount = accountService.findByEmail(email);
        Account account = optionalAccount.get();
        account.setPassword(passwordDTO.getPassword());
        accountService.save(account);

        AccountViewDTO accountViewDTO = new AccountViewDTO(account.getId(), account.getEmail(), account.getAuthorities());
        return accountViewDTO;
    }

    @DeleteMapping(value = "/profile/delete")
    @ApiResponse(responseCode = "200" , description = "Profile successfully deleted.")
    @ApiResponse(responseCode = "401" , description = "Token missing")
    @ApiResponse(responseCode = "403" , description = "Token error")
    @Operation(summary = "Delete Profile")
    @SecurityRequirement(name = "demo-api")
    public ResponseEntity<String> delete_profile(Authentication authentication) {
        String email = authentication.getName();
        Optional<Account> optionalAccount = accountService.findByEmail(email);

        if(optionalAccount.isPresent()) {
            accountService.deleteByID(optionalAccount.get().getId());
            return ResponseEntity.ok("User deleted");
        }
        return new ResponseEntity<String>("Bad Request" , HttpStatus.BAD_REQUEST);
    }
}
