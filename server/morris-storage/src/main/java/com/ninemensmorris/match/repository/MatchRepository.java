package com.ninemensmorris.match.repository;

import com.ninemensmorris.match.domain.Match;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, Long> {

    // 같은 두 사람이 최근에 몇 판을 뒀는지. 반복 대전 감쇠 판단에 쓴다
    @Query(
            """
            select count(m) from Match m
             where m.finishedAt >= :since
               and ((m.black.userId = :one and m.white.userId = :other)
                 or (m.black.userId = :other and m.white.userId = :one))
            """)
    int countRecentBetween(@Param("one") Long one, @Param("other") Long other, @Param("since") Instant since);
}
