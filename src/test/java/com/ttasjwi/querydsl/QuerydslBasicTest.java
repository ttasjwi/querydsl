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


    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(null)
     * 3. 단, 2에서 회원 이름이 없을 경우 마지막에 출력 (null last)
     */

    @Test
    public void sort() throws Exception {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.name.asc().nullsLast())
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getName()).isEqualTo("member5");
        assertThat(member6.getName()).isEqualTo("member6");
        assertThat(memberNull.getName()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1) // 0부터 시작
                .limit(2) // 2개 가져옴
                .fetch();

        Member member3 = result.get(0);
        Member member2 = result.get(1);

        assertThat(result.size()).isEqualTo(2);
        assertThat(member3.getName()).isEqualTo("member3");
        assertThat(member2.getName()).isEqualTo("member2");
    }

    @Test
    public void paging2() {
        //deprecated
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1) // 0부터 시작
                .limit(2) // 2개 가져옴
                .fetchResults();

        long totalCount = queryResults.getTotal();
        long limit = queryResults.getLimit();
        long offset = queryResults.getOffset();

        List<Member> results = queryResults.getResults();

        assertThat(totalCount).isEqualTo(4);
        assertThat(limit).isEqualTo(2);
        assertThat(offset).isEqualTo(1);
        assertThat(results.size()).isEqualTo(2);
    }
}
