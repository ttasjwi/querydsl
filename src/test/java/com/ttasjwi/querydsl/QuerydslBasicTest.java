package com.ttasjwi.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ttasjwi.querydsl.member.domain.Member;
import com.ttasjwi.querydsl.member.domain.QMember;
import com.ttasjwi.querydsl.team.domain.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static com.ttasjwi.querydsl.member.domain.QMember.*;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    private EntityManager em;

    private JPAQueryFactory queryFactory;

    @BeforeEach
    void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        //JPQL로 쿼리 작성
        String query =
                "SELECT m " +
                "From Member as m " +
                "Where m.name = :name";

        Member findMember = em.createQuery(query, Member.class)
                .setParameter("name", "member1")
                .getSingleResult();

        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        //QMember m = new QMember("m1"); // QMember의 인자가 JPQL의 alias로 쓰임. 같은 테이블 조인할 때 쓰기

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam1() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.name.eq("member1")
                                .and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam2() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.name.eq("member1"),
                        member.age.between(10,30))
                .fetchOne();
        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest() throws Exception {
//        List<Member> fetch = queryFactory.selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory.selectFrom(member)
//                .fetchOne();

        // fetch First는 limit(1).fetchOne()과 같다.
//        Member fetchFirst = queryFactory.selectFrom(QMember.member)
//                .fetchFirst();

//        QueryResults<Member> results = queryFactory.selectFrom(member) // deprecated
//                .fetchResults();
//
//        long total = results.getTotal();
//        List<Member> contents = results.getResults();
//
//        queryFactory // deprecated
//                .selectFrom(member)
//                .fetchCount();

        //Querydsl 5.0 이후에는 fetchCount가 deprecated 되었으므로 쓰지 않도록 할 것
        // 명시적으로 페이징 쿼리랑, total 쿼리를 두개 분리해서 사용하는 것이 성능 최적화에 유리.
       Long totalCount = queryFactory
                .select(member.count()) // select count(member.member_id)
                .from(member)
                .fetchOne();
        assertThat(totalCount).isEqualTo(4);
    }
}
