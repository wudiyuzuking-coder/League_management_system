package com.example.leagueticket.mapper;
import com.example.leagueticket.entity.*;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;
@Mapper public interface MatchResultMapper {
 @Insert("INSERT INTO match_result_submission(match_id,event_admin_id,home_score,away_score,submitted_at) VALUES(#{matchId},#{eventAdminId},#{homeScore},#{awayScore},#{submittedAt})") @Options(useGeneratedKeys=true,keyProperty="submissionId") int insertSubmission(MatchResultSubmission value);
 @Select("SELECT s.*,u.username submitter_name FROM match_result_submission s JOIN sys_user u ON u.user_id=s.event_admin_id WHERE s.match_id=#{matchId} ORDER BY s.submitted_at,s.submission_id") List<MatchResultSubmission> findSubmissions(Long matchId);
 @Select("SELECT * FROM match_result_review WHERE match_id=#{matchId}") MatchResultReview findReview(Long matchId);
 @Select("SELECT * FROM match_result_review WHERE match_id=#{matchId} FOR UPDATE") MatchResultReview findReviewForUpdate(Long matchId);
 @Insert("INSERT INTO match_result_review(match_id,review_status,review_reason,final_home_score,final_away_score,confirmed_by,confirmed_at) VALUES(#{matchId},#{reviewStatus},#{reviewReason},#{finalHomeScore},#{finalAwayScore},#{confirmedBy},#{confirmedAt}) ON DUPLICATE KEY UPDATE review_status=VALUES(review_status),review_reason=VALUES(review_reason),final_home_score=VALUES(final_home_score),final_away_score=VALUES(final_away_score),confirmed_by=VALUES(confirmed_by),confirmed_at=VALUES(confirmed_at)") int saveReview(MatchResultReview value);
 @Select("SELECT * FROM match_result_review WHERE review_status='PENDING_ADMIN_REVIEW' ORDER BY updated_at,match_id") List<MatchResultReview> findPending();
 @Select("SELECT COUNT(*) FROM match_result_review r JOIN match_info m ON m.match_id=r.match_id WHERE m.season_id=#{seasonId} AND r.review_status='PENDING_ADMIN_REVIEW'") int countPendingBySeason(Long seasonId);
 @Update("UPDATE match_info SET home_score=#{homeScore},away_score=#{awayScore},match_status='FINISHED' WHERE match_id=#{matchId} AND match_status IN ('PUBLISHED','IN_PROGRESS')") int publish(@Param("matchId")Long matchId,@Param("homeScore")Integer homeScore,@Param("awayScore")Integer awayScore);
}
