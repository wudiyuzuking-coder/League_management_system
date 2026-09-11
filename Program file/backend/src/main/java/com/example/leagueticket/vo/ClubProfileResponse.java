package com.example.leagueticket.vo;

import com.example.leagueticket.entity.*;
import java.math.BigDecimal;

public record ClubProfileResponse(
        Long clubId,String clubName,String shortName,String logoUrl,String description,String clubStatus,
        String leaderName,String leaderPhone,String leaderNickname,String leaderStatus,
        Long homeStadiumId,String venueModel,String city,String stadiumName,String address,Integer capacity,
        Integer rowsPerZone,Integer longSideSeatsPerRow,Integer shortSideSeatsPerRow,
        BigDecimal vipPrice,BigDecimal normalPrice,boolean standardHomeComplete) {
    public static ClubProfileResponse from(ClubInfo club, StadiumInfo stadium, ClubHomeStadiumConfig config, boolean complete) {
        return new ClubProfileResponse(club.getClubId(),club.getClubName(),club.getShortName(),club.getLogoUrl(),club.getDescription(),club.getClubStatus(),
                club.getLeaderName(),club.getLeaderPhone(),club.getLeaderNickname(),club.getLeaderStatus(),club.getHomeStadiumId(),
                stadium==null?null:stadium.getVenueModel(),stadium==null?club.getHomeCity():stadium.getCity(),
                stadium==null?null:stadium.getStadiumName(),stadium==null?club.getHomeAddress():stadium.getAddress(),stadium==null?null:stadium.getCapacity(),
                config==null?null:config.getRowsPerZone(),config==null?null:config.getLongSideSeatsPerRow(),config==null?null:config.getShortSideSeatsPerRow(),
                config==null?null:config.getVipPrice(),config==null?null:config.getNormalPrice(),complete);
    }
}
