package com.ninemensmorris.core.rating;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.core.rating.Rating.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RatingTest {

    private static final int VETERAN = Rating.PLACEMENT_GAMES;

    private static Player veteran(int mmr) {
        return new Player(mmr, VETERAN);
    }

    @Nested
    class 상대_실력_반영 {

        @Test
        @DisplayName("강한 상대를 이기면 더 많이 얻는다")
        void 강한_상대를_이기면_많이_얻는다() {
            // when
            int againstStrong = Rating.delta(veteran(1000), veteran(1600), Rating.WIN);
            int againstEqual = Rating.delta(veteran(1000), veteran(1000), Rating.WIN);
            int againstWeak = Rating.delta(veteran(1000), veteran(400), Rating.WIN);

            // then
            assertThat(againstStrong).isGreaterThan(againstEqual);
            assertThat(againstEqual).isGreaterThan(againstWeak);
        }

        @Test
        @DisplayName("약한 상대에게 지면 더 많이 잃는다")
        void 약한_상대에게_지면_많이_잃는다() {
            // when
            int toWeak = Rating.delta(veteran(1600), veteran(1000), Rating.LOSS);
            int toStrong = Rating.delta(veteran(1000), veteran(1600), Rating.LOSS);

            // then
            assertThat(toWeak).isLessThan(toStrong);
        }

        @Test
        @DisplayName("400점 차이는 기대 승률 약 10:1 이라 강자가 이겨도 거의 얻지 못한다")
        void 사백점_차이의_의미() {
            // when
            int delta = Rating.delta(veteran(1400), veteran(1000), Rating.WIN);

            // then — 기대 승률이 약 0.909 라 32 * 0.091 ≈ 3
            assertThat(delta).isBetween(1, 5);
        }
    }

    @Nested
    class 무승부 {

        @Test
        @DisplayName("동급끼리 무승부면 변동이 없다")
        void 동급_무승부는_변동이_없다() {
            // then
            assertThat(Rating.delta(veteran(1000), veteran(1000), Rating.DRAW)).isZero();
        }

        @Test
        @DisplayName("낮은 쪽이 무승부를 하면 오른다")
        void 약자가_무승부하면_오른다() {
            // then
            assertThat(Rating.delta(veteran(1000), veteran(1600), Rating.DRAW)).isPositive();
        }
    }

    @Nested
    class 배치_판수 {

        @Test
        @DisplayName("둘 다 배치 중이면 변동 폭이 더 크다")
        void 배치중에는_크게_움직인다() {
            // when
            int placement = Rating.delta(new Player(1000, 0), new Player(1000, 0), Rating.WIN);
            int settled = Rating.delta(veteran(1000), veteran(1000), Rating.WIN);

            // then
            assertThat(placement).isGreaterThan(settled);
        }

        @Test
        @DisplayName("배치 중인 사람과 고참은 두 K 의 평균으로 움직인다")
        void 섞이면_평균_K_를_쓴다() {
            // when — 동급이라 기대 승률 0.5. 평균 K 40 의 절반
            int delta = Rating.delta(new Player(1000, 0), veteran(1000), Rating.WIN);

            // then
            assertThat(delta).isEqualTo(20);
        }
    }

    @Nested
    class 하한_제한 {

        @Test
        @DisplayName("패자가 하한까지 남은 만큼만 잃고 승자도 그만큼만 얻는다")
        void 하한까지만_주고받는다() {
            // given
            Player nearFloor = veteran(Rating.FLOOR + 3);

            // when
            int winnerGain = Rating.limitByFloor(20, veteran(1000), nearFloor);
            int loserLoss = Rating.limitByFloor(-20, nearFloor, veteran(1000));

            // then
            assertThat(winnerGain).isEqualTo(3);
            assertThat(loserLoss).isEqualTo(-3);
        }

        @Test
        @DisplayName("하한에서 멀면 그대로다")
        void 여유가_있으면_그대로다() {
            // then
            assertThat(Rating.limitByFloor(16, veteran(1000), veteran(1000))).isEqualTo(16);
            assertThat(Rating.limitByFloor(-16, veteran(1000), veteran(1000))).isEqualTo(-16);
        }
    }

    @Test
    @DisplayName("하한 아래로는 내려가지 않는다")
    void 하한을_적용한다() {
        // then
        assertThat(Rating.applyFloor(-50)).isEqualTo(Rating.FLOOR);
        assertThat(Rating.applyFloor(1000)).isEqualTo(1000);
    }

    @Nested
    class 티어 {

        @Test
        @DisplayName("배치를 마치기 전에는 언랭크다")
        void 배치_전에는_언랭크다() {
            // then
            assertThat(Tier.of(1500, 0)).isEqualTo(Tier.UNRANKED);
            assertThat(Tier.of(1500, Rating.PLACEMENT_GAMES - 1)).isEqualTo(Tier.UNRANKED);
        }

        @Test
        @DisplayName("시작 점수는 실버다")
        void 시작_점수는_실버다() {
            // then
            assertThat(Tier.of(Rating.INITIAL, VETERAN)).isEqualTo(Tier.SILVER);
        }

        @Test
        @DisplayName("구간 경계에서 등급이 바뀐다")
        void 경계에서_바뀐다() {
            // then
            assertThat(Tier.of(899, VETERAN)).isEqualTo(Tier.BRONZE);
            assertThat(Tier.of(900, VETERAN)).isEqualTo(Tier.SILVER);
            assertThat(Tier.of(1099, VETERAN)).isEqualTo(Tier.SILVER);
            assertThat(Tier.of(1100, VETERAN)).isEqualTo(Tier.GOLD);
            assertThat(Tier.of(1350, VETERAN)).isEqualTo(Tier.PLATINUM);
            assertThat(Tier.of(1650, VETERAN)).isEqualTo(Tier.DIAMOND);
            assertThat(Tier.of(2000, VETERAN)).isEqualTo(Tier.MASTER);
            assertThat(Tier.of(9999, VETERAN)).isEqualTo(Tier.MASTER);
        }

        @Test
        @DisplayName("위로 갈수록 구간이 넓어진다")
        void 위로_갈수록_넓어진다() {
            // when
            int silver = Tier.GOLD.floor() - Tier.SILVER.floor();
            int gold = Tier.PLATINUM.floor() - Tier.GOLD.floor();
            int platinum = Tier.DIAMOND.floor() - Tier.PLATINUM.floor();
            int diamond = Tier.MASTER.floor() - Tier.DIAMOND.floor();

            // then
            assertThat(silver).isLessThan(gold);
            assertThat(gold).isLessThan(platinum);
            assertThat(platinum).isLessThan(diamond);
        }
    }
}
