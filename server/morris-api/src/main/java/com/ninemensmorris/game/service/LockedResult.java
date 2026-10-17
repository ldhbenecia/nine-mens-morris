package com.ninemensmorris.game.service;

// 방 락 안에서 만든 결과 + 락을 놓은 뒤 처리할 정산
//
// 락 안에서는 방 상태만 바꾸고 DB 기록은 미뤄야 함. DB 를 기다리는 동안 그 방의 다음 수가 막힘
// 람다는 값 하나만 돌려줄 수 있어 둘을 묶어서 꺼냄
// settlement 가 null 이면 이번 처리로 끝난 판이 없다는 뜻
record LockedResult<R>(R value, Settlement settlement) {

    static <R> LockedResult<R> of(R value) {
        return new LockedResult<>(value, null);
    }

    static <R> LockedResult<R> settled(R value, Settlement settlement) {
        return new LockedResult<>(value, settlement);
    }
}
