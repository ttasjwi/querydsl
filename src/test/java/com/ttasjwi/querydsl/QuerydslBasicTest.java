package com.ttasjwi.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ttasjwi.querydsl.member.domain.Member;
import com.ttasjwi.querydsl.member.domain.QMember;
import com.ttasjwi.querydsl.team.domain.Team;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static com.ttasjwi.querydsl.member.domain.QMember.member;
import static com.ttasjwi.querydsl.team.domain.QTeam.team;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private EntityManagerFactory emf;

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
                        member.age.between(10, 30))
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

    @Test
    public void aggregation() {
        List<Tuple> results = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member)
                .fetch();

        Tuple tuple = results.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팀의 이름과 각 팀의 평균 연령
     */

    @Test
    public void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .having(member.age.avg().between(10, 40))
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * teamA에 속한 모든 회원
     */

    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) // 기본은 innerJoin, left, right 모두 가능
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result)
                .extracting("name")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(카테시안 조인)
     * 회원 이름이 팀 이름과 같은 회원 조회
     */

    @Test
    public void theta_join() {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // from절에 복수의 테이블 나열 -> 실제로 카테시안 조인(cross join) 쿼리 날아감
                .where(member.name.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("name")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * <p>
     * JPQL : SELECT m, t FROM Member as m LEFT JOIN m.team as t ON t.name = 'teamA'
     */

    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            log.info("tuple= {}", tuple);
        }
    }

    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.name.eq(team.name)) // 연관관계와 상관없이 하므로 member.team이 아닌, team을 바로 조인 -> 실제 sql에서도 외래키 관련 쿼리가 안 날아감
                .fetch();

        for (Tuple tuple : result) {
            log.info("tuple = {}", tuple);
        }
    }

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        //로딩 됐는지 여부
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());


        assertThat(loaded).as("페치조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team).fetchJoin()
                .where(member.name.eq("member1"))
                .fetchOne();

        //로딩 됐는지 여부
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());


        assertThat(loaded).as("페치조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQueryMax() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .contains(40);
    }

    /**
     * 나이가 가장 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        for (Member findMember : result) {
            log.info("findMember = {}", findMember);
        }

        assertThat(result).extracting("age")
                .contains(30, 40);
    }

    /**
     * in절을 이용, 테이블에서 10보다 큰 나이들을 조회하고 나이가 해당되는 회원들 조회
     */
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        for (Member findMember : result) {
            log.info("findMember = {}", findMember);
        }

        assertThat(result).extracting("age")
                .contains(20, 30, 40);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory
                .select(member.name,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            log.info("memberName = {} // ageAvg = {}",
                    tuple.get(member.name),
                    tuple.get(select(memberSub.age.avg())
                            .from(memberSub)));
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            log.info("s = {}",s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            log.info("s = {}",s);
        }
    }

    @Test
    public void rankPath() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3); // 복잡한 정렬조건을 부여하고 싶을 때

        List<Tuple> result = queryFactory
                .select(member.name, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc(), member.age.desc())
                .fetch();

        for (Tuple tuple : result) {
            log.info("memberName = {}, age =  {}, rank = {}",
                    tuple.get(member.name), tuple.get(member.age), tuple.get(rankPath));
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.name, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            log.info("memberName = {}, constant = {}",
                    tuple.get(member.name), tuple.get(Expressions.constant("A")));
        }
    }

    @Test
    public void concat() {
        String result = queryFactory
                .select(member.name.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        log.info("result = {}", result);
        assertThat(result).isEqualTo("member1_10");
    }
}
