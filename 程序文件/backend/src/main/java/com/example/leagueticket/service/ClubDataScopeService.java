package com.example.leagueticket.service;

import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.security.AuthenticatedUser;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class ClubDataScopeService {

    public Long requireBoundClubId(AuthenticatedUser principal) {
        if (principal.clubId() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "current CLUB account is not bound to a club");
        }
        return principal.clubId();
    }

    public void requireSameClub(Long expectedClubId, Long actualClubId) {
        if (expectedClubId == null || !expectedClubId.equals(actualClubId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "cannot access another club's data");
        }
    }
}
