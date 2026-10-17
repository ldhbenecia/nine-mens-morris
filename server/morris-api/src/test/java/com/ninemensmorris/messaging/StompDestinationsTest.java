package com.ninemensmorris.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StompDestinationsTest {

    @Test
    @DisplayName("방 토픽은 번호 뒤에 경로가 더 붙으면 방 토픽으로 읽지 않는다")
    void 방_토픽은_번호로_끝나야_한다() {
        assertThat(StompDestinations.roomIdFromTopic("/topic/rooms/7")).contains(7L);
        assertThat(StompDestinations.roomIdFromTopic("/topic/rooms/7/x")).isEmpty();
        assertThat(StompDestinations.roomIdFromTopic("/topic/rooms/abc")).isEmpty();
        assertThat(StompDestinations.roomIdFromTopic("/topic/lobby")).isEmpty();
        assertThat(StompDestinations.roomIdFromTopic(null)).isEmpty();
    }

    @Test
    @DisplayName("앱 목적지는 방 번호 뒤의 행동 경로를 떼고 읽는다")
    void 앱_목적지는_행동_앞까지_읽는다() {
        assertThat(StompDestinations.roomIdFromAppDestination("/app/rooms/42/place"))
                .contains(42L);
        assertThat(StompDestinations.roomIdFromAppDestination("/app/rooms/42")).contains(42L);
        assertThat(StompDestinations.roomIdFromAppDestination("/app/whatever")).isEmpty();
    }
}
