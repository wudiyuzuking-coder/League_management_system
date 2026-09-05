package com.example.leagueticket.vo;
import com.example.leagueticket.entity.MatchSeatInventory;
import java.util.List;
public record InventoryRowResponse(int rowNo,String rowLabel,List<MatchSeatInventory> seats) {}
