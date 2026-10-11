package com.ninemensmorris.core.rating;

// Elo 기반 MMR
//
// 한 판의 변동은 하나만 계산하고 상대는 부호만 뒤집음
// 두 사람에게 따로 계산하면 K 나 하한 처리가 달라져 판마다 총합이 늘어남
public final class Rating {

    public static final int INITIAL = 1000; // 중간 구간(SILVER)에서 시작해야 오르내릴 여지가 생김
    public static final int FLOOR = 100;
    public static final int PLACEMENT_GAMES = 10;

    public static final double WIN = 1.0;
    public static final double DRAW = 0.5;
    public static final double LOSS = 0.0;

    private static final int K_PLACEMENT = 48; // 배치 중에는 제자리를 빨리 찾도록 크게
    private static final int K_DEFAULT = 32;

    private Rating() {}

    public record Player(int mmr, int gamesPlayed) {}

    // me 기준 변동. 상대에게는 -delta 를 적용해야 함
    // K 는 두 사람 값의 평균. 둘 다 배치면 48, 둘 다 고참이면 32
    public static int delta(Player me, Player opponent, double myScore) {
        double expected = 1.0 / (1.0 + Math.pow(10, (opponent.mmr() - me.mmr()) / 400.0));
        int k = (kFactor(me) + kFactor(opponent)) / 2;
        return (int) Math.round(k * (myScore - expected));
    }

    // 잃는 쪽이 하한 아래로 내려갈 만큼은 깎음. 깎은 만큼 얻는 쪽도 덜 얻음
    public static int limitByFloor(int myDelta, Player me, Player opponent) {
        if (myDelta > 0) {
            return Math.min(myDelta, Math.max(0, opponent.mmr() - FLOOR));
        }
        return -Math.min(-myDelta, Math.max(0, me.mmr() - FLOOR));
    }

    public static int applyFloor(int mmr) {
        return Math.max(FLOOR, mmr);
    }

    private static int kFactor(Player player) {
        return player.gamesPlayed() < PLACEMENT_GAMES ? K_PLACEMENT : K_DEFAULT;
    }
}
