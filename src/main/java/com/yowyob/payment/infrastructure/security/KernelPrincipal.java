package com.yowyob.payment.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.yowyob.payment.infrastructure.config.KernelAuthProperties;

import lombok.Getter;

/**
 * Principal Spring Security issu des claims JWT kernel (sub, tid, oid, permissions).
 */
@Getter
public class KernelPrincipal implements UserDetails {

    private final UUID userId;
    private final String tenantId;
    private final UUID organizationId;
    private final List<String> permissions;
    private final Set<String> adminPermissions;

    /**
     * @param userId            identifiant utilisateur (claim sub)
     * @param tenantId          identifiant tenant (claim tid)
     * @param organizationId    identifiant organisation (claim oid)
     * @param permissions       permissions kernel
     * @param adminPermissions  permissions considérées comme admin
     */
    public KernelPrincipal(UUID userId, String tenantId, UUID organizationId,
            List<String> permissions, Set<String> adminPermissions) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.organizationId = organizationId;
        this.permissions = permissions != null ? List.copyOf(permissions) : List.of();
        this.adminPermissions = adminPermissions;
    }

    /**
     * @param properties configuration kernel
     * @return true si l'utilisateur possède une permission admin
     */
    public boolean isAdmin() {
        return permissions.stream().anyMatch(adminPermissions::contains);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(p -> new SimpleGrantedAuthority("PERM_" + p.replace(':', '_')))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return userId.toString();
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
