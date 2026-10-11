package com.ninemensmorris.game.domain;

import org.junit.jupiter.api.BeforeEach;

// 단위 테스트가 쓰는 저장소가 운영 저장소와 같은 약속을 지키는지
class InMemoryRoomRegistryTest implements RoomRegistryContract {

    private RoomRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryRoomRegistry();
    }

    @Override
    public RoomRegistry registry() {
        return registry;
    }

    @Override
    public RoomRegistry otherServer() {
        return registry;
    }
}
