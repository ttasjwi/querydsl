package com.ttasjwi.querydsl;

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

import static com.ttasjwi.querydsl.member.domain.QMember.*;
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

}
