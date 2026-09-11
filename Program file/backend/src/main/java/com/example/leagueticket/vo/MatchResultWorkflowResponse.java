package com.example.leagueticket.vo;
import com.example.leagueticket.entity.MatchResultSubmission;
import java.time.LocalDateTime;
import java.util.List;
public record MatchResultWorkflowResponse(Long matchId,String reviewStatus,String reviewReason,Integer finalHomeScore,Integer finalAwayScore,Long confirmedBy,LocalDateTime confirmedAt,List<MatchResultSubmission> submissions){}
