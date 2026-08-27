package com.example.leagueticket.security;

import com.example.leagueticket.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(
        Long userId,
        String username,
        String realName,
        String roleCode,
        Long clubId,
        List<String> permissions,
        Collection<? extends GrantedAuthority> authorities
) {
    public static AuthenticatedUser from(SysUser user, List<String> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRoleCode()));
        permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
        return new AuthenticatedUser(user.getUserId(), user.getUsername(), user.getRealName(),
                user.getRoleCode(), user.getClubId(), List.copyOf(permissions), List.copyOf(authorities));
    }
}
