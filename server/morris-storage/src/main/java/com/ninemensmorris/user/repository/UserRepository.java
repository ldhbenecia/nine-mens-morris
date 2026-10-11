package com.ninemensmorris.user.repository;

import com.ninemensmorris.user.domain.Provider;
import com.ninemensmorris.user.domain.User;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인마다 도는 조회. uk_users_provider 를 그대로 탄다
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    // 상위 N명. 기존에는 EntityManager 로 전체를 메모리에 올린 뒤 잘랐다
    // 동점자 순서를 userId 로 고정하지 않으면 호출할 때마다 순위가 뒤바뀐다
    //
    // 비로그인 계정은 순위에서 뺀다. 무한히 만들 수 있어 등재를 허용하면 랭킹이 의미를 잃는다
    // 제외 대상을 파라미터로 받지 않고 질의에 박아 호출부가 실수로 다른 값을 넘기지 못하게 함
    default List<User> findRanked(Pageable pageable) {
        return findRankedAmong(EnumSet.complementOf(EnumSet.of(Provider.VISITOR)), pageable);
    }

    // <> VISITOR 로 쓰면 범위가 둘로 갈라져 인덱스 순서를 못 쓰고 회원 전체를 정렬함
    // 회원 제공자가 하나인 동안 IN 은 등호가 되어 (provider, mmr desc) 를 N개만 읽고 멈춤
    // 제공자가 늘면 정렬로 돌아갈 뿐 결과는 같음
    @Query("select u from User u where u.provider in :providers order by u.mmr desc, u.userId asc")
    List<User> findRankedAmong(@Param("providers") Set<Provider> providers, Pageable pageable);

    // 유휴 비로그인 계정 정리
    // 브라우저가 토큰을 잃으면(캐시 삭제, 다른 기기) 같은 사람이라도 새 행이 생긴다
    // 막을 수 없는 특성이므로 쌓이는 쪽을 주기적으로 지운다
    //
    // 회원을 지우지 않도록 제외 조건이 아니라 VISITOR 만 고르는 조건으로 쓴다
    @Modifying(clearAutomatically = true)
    @Query("delete from User u where u.provider = com.ninemensmorris.user.domain.Provider.VISITOR"
            + " and u.createdAt < :threshold")
    int deleteVisitorsCreatedBefore(@Param("threshold") Instant threshold);

    // 내 등수. 기존에는 전체 목록을 받아 프론트가 세는 수밖에 없었다
    @Query("select count(u) + 1 from User u where u.mmr > :mmr"
            + " and u.provider <> com.ninemensmorris.user.domain.Provider.VISITOR")
    int findRankByMmr(@Param("mmr") int mmr);
}
