package com.example.leagueticket.algorithm.seat;

import com.example.leagueticket.vo.*;
import java.time.LocalDateTime;

public interface SeatAllocateService {
    SeatAllocationResponse preview(Long matchZoneId,int ticketCount);
    SeatAllocationDebugResponse debug(Long matchZoneId,int ticketCount);
    SeatAllocationResponse selectAndClaimAvailable(Long matchZoneId,int ticketCount);
    SeatAllocationResponse selectAndLockSeats(Long matchZoneId,int ticketCount,Long orderId,
                                              LocalDateTime lockedAt,LocalDateTime expireTime);
}
