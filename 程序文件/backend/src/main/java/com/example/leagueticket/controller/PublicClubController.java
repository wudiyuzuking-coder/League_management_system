package com.example.leagueticket.controller;

import com.example.leagueticket.common.Result;
import com.example.leagueticket.dto.ClubQueryRequest;
import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.service.ClubInfoService;
import com.example.leagueticket.vo.PageResponse;
import com.example.leagueticket.vo.PublicClubSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs")
@Profile("dev")
@RequiredArgsConstructor
public class PublicClubController {
    private final ClubInfoService clubService;

    @GetMapping
    public Result<PageResponse<PublicClubSummaryResponse>> list(@Valid ClubQueryRequest request) {
        request.setStatus("ACTIVE");
        PageResponse<ClubInfo> page = clubService.list(request);
        var records = page.records().stream().map(this::summary).toList();
        return Result.success(new PageResponse<>(records, page.total(), page.page(), page.size()));
    }

    @GetMapping("/{id}")
    public Result<PublicClubSummaryResponse> detail(@PathVariable Long id) {
        ClubInfo club = clubService.getById(id);
        if (!"ACTIVE".equals(club.getClubStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "club not found");
        }
        return Result.success(summary(club));
    }

    private PublicClubSummaryResponse summary(ClubInfo club) {
        return new PublicClubSummaryResponse(club.getClubId(), club.getClubName(),
                club.getShortName(), club.getLogoUrl());
    }
}
