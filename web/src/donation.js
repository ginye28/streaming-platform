/** 후원 금액이 어느 단계인지(1~5). 서버의 DonationTier 와 같은 기준이다. 채팅에 칠하는 색을 고를 때 쓴다. */
export function donationTier(amount) {
    if (amount >= 50000) return 5
    if (amount >= 30000) return 4
    if (amount >= 10000) return 3
    if (amount >= 5000) return 2
    return 1
}
