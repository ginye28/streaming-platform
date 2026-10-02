package com.sp.api.chat.dto;

/**
 * 후원 금액이 어느 단계인지. 단계가 높을수록 채팅창에서 더 눈에 띄고 위에 더 오래 남는다.
 * 금액 기준과 고정 시간은 서버가 한 곳에서 정하고, 화면은 내려 준 값을 그대로 쓴다.
 */
public final class DonationTier {

    private DonationTier() {
    }

    /** 1(작은 후원) ~ 5(큰 후원). */
    public static int of(int amount) {

        if (amount >= 50_000) return 5;
        if (amount >= 30_000) return 4;
        if (amount >= 10_000) return 3;
        if (amount >= 5_000) return 2;

        return 1;
    }

    /** 채팅창 위에 후원 띠로 남는 시간(초). */
    public static int pinSeconds(int tier) {

        return switch (tier) {
            case 5 -> 600;
            case 4 -> 300;
            case 3 -> 180;
            case 2 -> 60;
            default -> 30;
        };
    }
}
