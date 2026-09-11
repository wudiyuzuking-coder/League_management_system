package com.example.leagueticket.service;
import com.example.leagueticket.dto.MatchScoreRequest;
import com.example.leagueticket.vo.MatchResultWorkflowResponse;
import java.util.List;
public interface MatchResultWorkflowService {MatchResultWorkflowResponse submit(Long matchId,Long eventAdminId,MatchScoreRequest request);MatchResultWorkflowResponse detail(Long matchId);List<MatchResultWorkflowResponse> pending();MatchResultWorkflowResponse confirm(Long matchId,Long adminId,MatchScoreRequest request);}
