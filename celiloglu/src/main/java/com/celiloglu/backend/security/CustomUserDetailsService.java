package com.celiloglu.backend.security;

import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new UsernameNotFoundException("İstifadəçi adı və ya e-poçt boş ola bilməz!");
        }

        String identifier = usernameOrEmail.trim();

        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(identifier);
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(identifier);

        User user;
        if (byUsername.isPresent() && byEmail.isPresent()) {
            User u1 = byUsername.get();
            User u2 = byEmail.get();
            if (!u1.getId().equals(u2.getId())) {
                throw new UsernameNotFoundException("İdentifikator ziddiyyətlidir: fərqli istifadəçilər tapıldı!");
            }
            user = u1;
        } else if (byUsername.isPresent()) {
            user = byUsername.get();
        } else if (byEmail.isPresent()) {
            user = byEmail.get();
        } else {
            throw new UsernameNotFoundException("İstifadəçi tapılmadı: " + identifier);
        }

        boolean enabled = Boolean.TRUE.equals(user.getIsActive());

        List<GrantedAuthority> authorities;
        if (user.getRole() != null) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        } else {
            authorities = Collections.emptyList();
        }

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                enabled,
                authorities
        );
    }
}
