package com.yowyob.payment.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.yowyob.payment.domain.user.User;
import com.yowyob.payment.domain.user.UserRole;

import lombok.Getter;

/**
 * Principal Spring Security encapsulant un utilisateur domaine.
 */
@Getter
public class AuthPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final UserRole role;

    /**
     * @param user utilisateur domaine
     */
    public AuthPrincipal(User user) {
        this.id = user.id();
        this.email = user.email();
        this.password = user.password();
        this.role = user.role();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
