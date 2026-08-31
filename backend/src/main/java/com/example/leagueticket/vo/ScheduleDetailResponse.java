package com.example.leagueticket.vo;

import com.example.leagueticket.entity.SeasonScheduleBatch;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data @EqualsAndHashCode(callSuper=true)
public class ScheduleDetailResponse extends SeasonScheduleBatch {
    private List<ScheduleRoundResponse> rounds;
}
