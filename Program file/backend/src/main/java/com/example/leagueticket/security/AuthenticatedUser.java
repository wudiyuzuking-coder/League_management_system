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
        String phone,
        String realName,
        String employeeNo,
        String roleCode,
        Long clubId,
        String userStatus,
        List<String> permissions,
        Collection<? extends GrantedAuthority> authorities
) {
    public static AuthenticatedUser from(SysUser user, List<String> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRoleCode()));
        permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
        return new AuthenticatedUser(user.getUserId(), user.getUsername(), user.getPhone(), user.getRealName(),
                user.getEmployeeNo(), user.getRoleCode(), user.getClubId(), user.getUserStatus(),
                List.copyOf(permissions), List.copyOf(authorities));
    }
}
