package com.shank.AlbumsAPI.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.shank.AlbumsAPI.model.Account;
import com.shank.AlbumsAPI.repository.AccountRepository;
import com.shank.AlbumsAPI.util.constants.Authority;

@Service
public class AccountService implements UserDetailsService{
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Account save(Account account) {
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        if (account.getAuthorities() == null) {
            account.setAuthorities(Authority.USER.toString());
        }
        return accountRepository.save(account);
    }

    public List<Account> findallAccounts() {
        return accountRepository.findAll();
    }

    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    public Optional<Account> findByID(long id) {
        return accountRepository.findById(id);
    }

    public void deleteByID(long id) {
        accountRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<Account> optionalAccount = accountRepository.findByEmail(email);
        if(!optionalAccount.isPresent()) {
            throw new UsernameNotFoundException("Account not found");
        }
        Account account = optionalAccount.get();

        List<GrantedAuthority> grantedAuthority = new ArrayList<>();
        grantedAuthority.add(new SimpleGrantedAuthority(account.getAuthorities()));
        return new User(account.getEmail(), account.getPassword(), grantedAuthority);
    }
}
