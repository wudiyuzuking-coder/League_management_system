package com.example.leagueticket.service;

import com.example.leagueticket.dto.EnrollmentQueryRequest;
import com.example.leagueticket.dto.EnrollmentRequest;
import com.example.leagueticket.vo.*;
import java.util.List;

public interface ClubSeasonEnrollmentService {
    List<AvailableSeasonResponse> availableSeasons(Long clubId);
    EnrollmentResponse submit(Long clubId, EnrollmentRequest request);
    List<EnrollmentResponse> listClub(Long clubId);
    EnrollmentResponse detailClub(Long clubId,Long enrollmentId);
    PageResponse<EnrollmentResponse> listAdmin(EnrollmentQueryRequest query);
    EnrollmentResponse detailAdmin(Long enrollmentId);
}
