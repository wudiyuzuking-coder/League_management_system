package com.example.leagueticket.service;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class DoubleRoundRobinSchedulePlanner {
    public static final int MIN_CLUB_GAP_DAYS = 6;
    public List<PlannedGame> plan(int clubs, LocalDate startDate) {
        if(clubs<2||clubs>20)throw new IllegalArgumentException("clubs must be between 2 and 20");
        List<List<Pair>> rounds=fixtures(clubs);Map<Integer,LocalDate> lastPlayed=new HashMap<>();List<PlannedGame> result=new ArrayList<>();LocalDate cursor=startDate;
        for(int roundIndex=0;roundIndex<rounds.size();roundIndex++){List<Pair> pending=new ArrayList<>(rounds.get(roundIndex));while(!pending.isEmpty()){Pair chosen=null;for(Pair pair:pending)if(eligible(lastPlayed.get(pair.home),cursor)&&eligible(lastPlayed.get(pair.away),cursor)){chosen=pair;break;}if(chosen==null){cursor=cursor.plusDays(1);continue;}result.add(new PlannedGame(roundIndex+1,chosen.home,chosen.away,cursor));lastPlayed.put(chosen.home,cursor);lastPlayed.put(chosen.away,cursor);pending.remove(chosen);cursor=cursor.plusDays(1);}}
        return result;
    }
    public LocalDate expectedEndDate(int clubs,LocalDate startDate){List<PlannedGame> games=plan(clubs,startDate);return games.get(games.size()-1).date();}
    private boolean eligible(LocalDate last,LocalDate date){return last==null||ChronoUnit.DAYS.between(last,date)>=MIN_CLUB_GAP_DAYS;}
    private List<List<Pair>> fixtures(int clubs){int size=clubs%2==0?clubs:clubs+1;List<Integer> rotating=new ArrayList<>();for(int i=0;i<clubs;i++)rotating.add(i);if(size>clubs)rotating.add(null);List<List<Pair>> first=new ArrayList<>();for(int round=0;round<size-1;round++){List<Pair> games=new ArrayList<>();for(int i=0;i<size/2;i++){Integer left=rotating.get(i),right=rotating.get(size-1-i);if(left==null||right==null)continue;if((round+i)%2==0)games.add(new Pair(left,right));else games.add(new Pair(right,left));}first.add(games);Integer last=rotating.remove(size-1);rotating.add(1,last);}List<List<Pair>> all=new ArrayList<>(first);for(List<Pair> round:first)all.add(round.stream().map(p->new Pair(p.away,p.home)).toList());return all;}
    private record Pair(int home,int away){}
    public record PlannedGame(int roundNo,int homeIndex,int awayIndex,LocalDate date){}
}
