package com.boardgaming.assistant.adapter.out.cache;

import com.boardgaming.assistant.application.dto.GroupProfileDto;

public class EstimateCacheKeyBuilder {

    public String build(String gameId, GroupProfileDto profile) {
        return normalize(gameId)
                + ":" + profile.playerCount()
                + ":" + normalize(profile.groupFamiliarity())
                + ":" + normalize(profile.turnPace())
                + ":" + normalize(profile.analysisStyle())
                + ":" + profile.childrenIncluded();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.strip().toLowerCase();
    }
}
