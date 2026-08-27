package com.sp.api.vtuber.dto;

import com.sp.api.user.entity.User;
import com.sp.api.vtuber.entity.ChannelProfile;
import com.sp.api.vtuber.entity.ModelCredit;

import java.time.LocalDate;
import java.util.List;

/**
 * 채널 페이지에서 보여 줄 그 사람의 정보.
 * 아직 아무것도 채우지 않은 채널도 같은 형태로 내려간다 — 화면이 분기하지 않게.
 */
public record ChannelProfileResponse(
        Long channelId,
        String nickname,
        String profileImage,
        String oshiMarkUrl,
        /** 팬덤 이름. 없으면 화면은 "구독자" 라고 쓴다. */
        String fanName,
        long subscriberCount,
        LocalDate debutOn,
        /** 데뷔일이 아직 오지 않았으면 남은 날수. 지났으면 null. */
        Long daysUntilDebut,
        LocalDate graduatedOn,
        boolean graduated,
        List<Credit> credits
) {

    public record Credit(String role, String name, String link) {

        static Credit from(ModelCredit credit) {
            return new Credit(credit.getRole().name(), credit.getName(), credit.getLink());
        }
    }

    public static ChannelProfileResponse of(
            User channel,
            ChannelProfile profile,
            long subscriberCount,
            List<ModelCredit> credits
    ) {
        return new ChannelProfileResponse(
                channel.getId(),
                channel.getNickname(),
                channel.getProfileImage(),
                profile == null ? null : profile.getOshiMarkUrl(),
                profile == null ? null : profile.getFanName(),
                subscriberCount,
                profile == null ? null : profile.getDebutOn(),
                profile == null ? null : profile.daysUntilDebut(),
                profile == null ? null : profile.getGraduatedOn(),
                profile != null && profile.isGraduated(),
                credits.stream().map(Credit::from).toList()
        );
    }
}
