package com.isaac.sliceofpie.watchlist;

import java.util.List;

import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistResponse;

public class WatchlistUtils {
   
    public static List<Long> getIdsFromWatchlistResponse(WatchlistResponse list){
        return list.items().stream().map(item -> item.instrumentId()).toList();
    }
}
