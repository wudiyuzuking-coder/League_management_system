package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.*;
import com.example.leagueticket.domain.SeasonStatus;
import com.example.leagueticket.entity.*;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.*;
import com.example.leagueticket.service.ClubSeasonEnrollmentService;
import com.example.leagueticket.service.ClubHomeStadiumService;
import com.example.leagueticket.service.SystemTimeService;
import com.example.leagueticket.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class ClubSeasonEnrollmentServiceImpl implements ClubSeasonEnrollmentService {
    private static final Set<String> POSITIONS=Set.of("GOALKEEPER","DEFENDER","MIDFIELDER","FORWARD");
    private final ClubSeasonEnrollmentMapper mapper;
    private final SeasonInfoMapper seasonMapper;
    private final ClubInfoMapper clubMapper;
    private final StadiumInfoMapper stadiumMapper;
    private final PlayerInfoMapper playerMapper;
    private final CoachInfoMapper coachMapper;
    private final SystemTimeService timeService;
    private final SeasonScheduleMapper scheduleMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ClubHomeStadiumService homeStadiumService;
    private final com.example.leagueticket.service.ClubPersonnelService personnelService;

    @Override public List<AvailableSeasonResponse> availableSeasons(Long clubId){return mapper.findAvailable(clubId,timeService.now(),SeasonStatus.REGISTRATION);}

    @Override @Transactional
    public EnrollmentResponse submit(Long clubId,EnrollmentRequest request){
        ClubInfo club=clubMapper.findById(clubId);
        if(club==null||!"ACTIVE".equals(club.getClubStatus()))throw new BusinessException(HttpStatus.FORBIDDEN,"current club is unavailable");
        SeasonInfo season=seasonMapper.findByIdForUpdate(request.seasonId());
        if(season==null)throw new BusinessException(HttpStatus.NOT_FOUND,"season not found");
        if(scheduleMapper.findBySeasonForUpdate(season.getSeasonId())!=null)throw new BusinessException(HttpStatus.CONFLICT,"赛程已生成，不能继续报名");
        LocalDateTime now=timeService.now();
        validateWindow(season,now);
        if(mapper.countBySeasonClub(season.getSeasonId(),clubId)>0)
            throw new BusinessException(HttpStatus.CONFLICT,"该俱乐部已报名此赛季");
        SeasonInfo conflict=mapper.findConflict(clubId,season.getSeasonId(),season.getStartDate(),season.getEndDate());
        if(conflict!=null)throw new BusinessException(HttpStatus.CONFLICT,"该赛季与已报名赛季时间冲突: "+conflict.getSeasonName()+" ["+conflict.getStartDate()+" ~ "+conflict.getEndDate()+"]");
        if(mapper.findSubmittedIdsForUpdate(season.getSeasonId()).size()>=season.getMaxClubs())
            throw new BusinessException(HttpStatus.CONFLICT,"赛季报名名额已满");
        ClubHomeStadiumConfig homeConfig=homeStadiumService.requireComplete(clubId);
        validateStadium(club,homeConfig.getStadiumId());
        personnelService.requireCompliant(clubId);
        List<PlayerInfo> players=validatePlayers(clubId);
        List<CoachInfo> coaches=validateCoaches(clubId);

        ClubSeasonEnrollment enrollment=new ClubSeasonEnrollment();
        enrollment.setSeasonId(season.getSeasonId());enrollment.setClubId(clubId);enrollment.setStadiumId(homeConfig.getStadiumId());
        enrollment.setEnrollmentStatus("SUBMITTED");enrollment.setSubmittedAt(now);
        try{mapper.insert(enrollment);}catch(DataIntegrityViolationException e){throw new BusinessException(HttpStatus.CONFLICT,"该俱乐部已报名此赛季");}
        for(int i=0;i<players.size();i++){
            PlayerInfo p=players.get(i);
            ClubSeasonEnrollmentPlayer row=new ClubSeasonEnrollmentPlayer();row.setEnrollmentId(enrollment.getEnrollmentId());row.setPlayerId(p.getPlayerId());
            row.setLineupRole(p.getLineupRole());row.setPlayerNameSnapshot(p.getPlayerName());row.setShirtNoSnapshot(p.getShirtNo());
            row.setPositionSnapshot(p.getPosition());row.setBirthDateSnapshot(p.getBirthDate());row.setBirthYearSnapshot(birthYear(p));row.setNationalitySnapshot(p.getNationality());mapper.insertPlayer(row);
        }
        for(CoachInfo c:coaches){ClubSeasonEnrollmentCoach row=new ClubSeasonEnrollmentCoach();row.setEnrollmentId(enrollment.getEnrollmentId());
            row.setCoachId(c.getCoachId());row.setCoachNameSnapshot(c.getCoachName());row.setTitleSnapshot(c.getTitle());row.setBirthYearSnapshot(c.getBirthYear());row.setNationalitySnapshot(c.getNationality());mapper.insertCoach(row);}
        eventPublisher.publishEvent(new com.example.leagueticket.service.ScheduleEligibilityEvent(season.getSeasonId()));
        return detailAdmin(enrollment.getEnrollmentId());
    }

    @Override public List<EnrollmentResponse> listClub(Long clubId){LocalDateTime now=timeService.now();return mapper.findByClub(clubId,now).stream().map(e->summary(e,now)).toList();}
    @Override public EnrollmentResponse detailClub(Long clubId,Long enrollmentId){ClubSeasonEnrollment e=required(enrollmentId,timeService.now());if(!clubId.equals(e.getClubId()))throw new BusinessException(HttpStatus.FORBIDDEN,"cannot access another club's enrollment");return detail(e);}
    @Override public PageResponse<EnrollmentResponse> listAdmin(EnrollmentQueryRequest q){int page=q.safePage(),size=q.safeSize();LocalDateTime now=timeService.now();long total=mapper.countAdmin(q);List<EnrollmentResponse> rows=mapper.findAdminPage(q,now,(long)(page-1)*size,size).stream().map(e->summary(e,now)).toList();return new PageResponse<>(rows,total,page,size);}
    @Override public EnrollmentResponse detailAdmin(Long enrollmentId){return detail(required(enrollmentId,timeService.now()));}

    private void validateWindow(SeasonInfo s,LocalDateTime now){
        if(!SeasonStatus.REGISTRATION.matches(s.getSeasonStatus()))throw new BusinessException(HttpStatus.CONFLICT,"赛季当前不可报名");
        if(s.getRegistrationStartTime()==null||s.getRegistrationDeadline()==null||s.getMaxClubs()==null)
            throw new BusinessException(HttpStatus.CONFLICT,"赛季报名配置不完整");
        if(now.isBefore(s.getRegistrationStartTime()))throw new BusinessException(HttpStatus.CONFLICT,"赛季报名尚未开始");
        if(!now.isBefore(s.getRegistrationDeadline()))throw new BusinessException(HttpStatus.CONFLICT,"赛季报名已截止");
    }
    private void validateStadium(ClubInfo club,Long stadiumId){
        if(club.getHomeStadiumId()==null)throw new BusinessException("current club has no default home stadium");
        if(!club.getHomeStadiumId().equals(stadiumId))throw new BusinessException(HttpStatus.FORBIDDEN,"报名场馆必须是当前俱乐部默认主场");
        StadiumInfo stadium=stadiumMapper.findById(stadiumId);
        if(stadium==null||!"ACTIVE".equals(stadium.getStadiumStatus()))throw new BusinessException("default home stadium is unavailable");
        if(mapper.countActiveZones(stadiumId)<1)throw new BusinessException("default home stadium has no active zone");
        if(mapper.countActiveSeats(stadiumId)<1)throw new BusinessException("default home stadium has no active seat");
    }
    private List<PlayerInfo> validatePlayers(Long clubId){
        List<PlayerInfo> result=playerMapper.findActiveByClubId(clubId).stream().filter(p->p.getLineupRole()!=null).sorted(Comparator.comparing((PlayerInfo p)->"STARTER".equals(p.getLineupRole())?0:1).thenComparing(PlayerInfo::getShirtNo)).toList();
        Set<Integer> shirts=new HashSet<>();for(PlayerInfo p:result){if(!POSITIONS.contains(p.getPosition()))throw new BusinessException("unsupported player position");if(p.getShirtNo()==null||!shirts.add(p.getShirtNo()))throw new BusinessException("报名阵容球衣号码不能重复");}return result;
    }
    private List<CoachInfo> validateCoaches(Long clubId){
        return coachMapper.findActiveByClubId(clubId).stream().sorted(Comparator.comparing(c->"HEAD_COACH".equals(c.getTitle())?0:1)).toList();
    }
    private ClubSeasonEnrollment required(Long id,LocalDateTime now){ClubSeasonEnrollment e=mapper.findById(id,now);if(e==null)throw new BusinessException(HttpStatus.NOT_FOUND,"enrollment not found");return e;}
    private EnrollmentResponse detail(ClubSeasonEnrollment e){LocalDateTime now=timeService.now();EnrollmentResponse out=summary(e,now);
        out.setPlayers(mapper.findPlayers(e.getEnrollmentId()).stream().map(p->new EnrollmentPlayerResponse(p.getPlayerId(),p.getPlayerNameSnapshot(),p.getShirtNoSnapshot(),p.getPositionSnapshot(),p.getLineupRole(),p.getBirthYearSnapshot()!=null?p.getBirthYearSnapshot():(p.getBirthDateSnapshot()==null?null:p.getBirthDateSnapshot().getYear()),age(p.getBirthDateSnapshot(),p.getBirthYearSnapshot(),now.toLocalDate()),p.getNationalitySnapshot())).toList());
        out.setCoaches(mapper.findCoaches(e.getEnrollmentId()).stream().map(c->new EnrollmentCoachResponse(c.getCoachId(),c.getCoachNameSnapshot(),c.getTitleSnapshot(),c.getBirthYearSnapshot(),c.getNationalitySnapshot())).toList());return out;}
    private EnrollmentResponse summary(ClubSeasonEnrollment e,LocalDateTime now){EnrollmentResponse out=new EnrollmentResponse();out.setEnrollmentId(e.getEnrollmentId());out.setSeasonId(e.getSeasonId());out.setSeasonName(e.getSeasonName());out.setStartDate(e.getStartDate());out.setEndDate(e.getEndDate());out.setClubId(e.getClubId());out.setClubName(e.getClubName());out.setStadiumId(e.getStadiumId());out.setStadiumName(e.getStadiumName());out.setEnrollmentStatus(e.getEnrollmentStatus());out.setSubmittedAt(e.getSubmittedAt());out.setPlayerCount(e.getPlayerCount());out.setCoachCount(e.getCoachCount());out.setNextMatchTime(e.getNextMatchTime());out.setDaysUntilNextMatch(e.getNextMatchTime()==null?null:ChronoUnit.DAYS.between(now.toLocalDate(),e.getNextMatchTime().toLocalDate()));return out;}
    private Integer age(LocalDate birth,Integer year,LocalDate now){if(year!=null)return now.getYear()-year;return birth==null||birth.isAfter(now)?null:Period.between(birth,now).getYears();}
    private Integer birthYear(PlayerInfo player){return player.getBirthYear()!=null?player.getBirthYear():(player.getBirthDate()==null?null:player.getBirthDate().getYear());}
}
