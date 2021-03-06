package com.ttasjwi.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ttasjwi.querydsl.member.domain.Member;
import com.ttasjwi.querydsl.member.domain.QMember;
import com.ttasjwi.querydsl.member.dto.MemberDto;
import com.ttasjwi.querydsl.member.dto.QMemberDto;
import com.ttasjwi.querydsl.member.dto.UserDto;
import com.ttasjwi.querydsl.team.domain.Team;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
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
        //JPQL??? ?????? ??????
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
        //QMember m = new QMember("m1"); // QMember??? ????????? JPQL??? alias??? ??????. ?????? ????????? ????????? ??? ??????

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

        // fetch First??? limit(1).fetchOne()??? ??????.
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

        //Querydsl 5.0 ???????????? fetchCount??? deprecated ??????????????? ?????? ????????? ??? ???
        // ??????????????? ????????? ?????????, total ????????? ?????? ???????????? ???????????? ?????? ?????? ???????????? ??????.
        Long totalCount = queryFactory
                .select(member.count()) // select count(member.member_id)
                .from(member)
                .fetchOne();
        assertThat(totalCount).isEqualTo(4);
    }


    /**
     * ?????? ?????? ??????
     * 1. ?????? ?????? ????????????(desc)
     * 2. ?????? ?????? ????????????(null)
     * 3. ???, 2?????? ?????? ????????? ?????? ?????? ???????????? ?????? (null last)
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
                .offset(1) // 0?????? ??????
                .limit(2) // 2??? ?????????
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
                .offset(1) // 0?????? ??????
                .limit(2) // 2??? ?????????
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
     * ?????? ????????? ??? ?????? ?????? ??????
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
     * teamA??? ?????? ?????? ??????
     */

    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) // ????????? innerJoin, left, right ?????? ??????
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result)
                .extracting("name")
                .containsExactly("member1", "member2");
    }

    /**
     * ?????? ??????(???????????? ??????)
     * ?????? ????????? ??? ????????? ?????? ?????? ??????
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
                .from(member, team) // from?????? ????????? ????????? ?????? -> ????????? ???????????? ??????(cross join) ?????? ?????????
                .where(member.name.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("name")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ???) ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
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
                .leftJoin(team).on(member.name.eq(team.name)) // ??????????????? ???????????? ????????? member.team??? ??????, team??? ?????? ?????? -> ?????? sql????????? ????????? ?????? ????????? ??? ?????????
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

        //?????? ????????? ??????
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());


        assertThat(loaded).as("???????????? ?????????").isFalse();
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

        //?????? ????????? ??????
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());


        assertThat(loaded).as("???????????? ??????").isTrue();
    }

    /**
     * ????????? ?????? ?????? ?????? ??????
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
     * ????????? ?????? ?????? ????????? ?????? ??????
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
     * in?????? ??????, ??????????????? 10?????? ??? ???????????? ???????????? ????????? ???????????? ????????? ??????
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
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            log.info("s = {}", s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20???")
                        .when(member.age.between(21, 30)).then("21~30???")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            log.info("s = {}", s);
        }
    }

    @Test
    public void rankPath() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3); // ????????? ??????????????? ???????????? ?????? ???

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

    @Test
    public void simpleProjection() {
        List<String> names = queryFactory
                .select(member.name)
                .from(member)
                .fetch();

        for (String name : names) {
            log.info("memberName = {}", name);
        }
    }

    @Test
    public void tupleProjection() {
        //tuple??? repository ??????????????? ??????
        //????????? ???????????? DTO??? ???????????? ????????????
        List<Tuple> result = queryFactory
                .select(member.name, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            log.info("memberName = {}, memberAge = {}",
                    tuple.get(member.name), tuple.get(member.age));
        }
    }

    @Test
    public void findDtoByJPQL() {
        // FQCN??? ????????? ?????? ????????? ???????????????..
        List<MemberDto> memberDtos = em.createQuery("SELECT new com.ttasjwi.querydsl.member.dto.MemberDto(m.name, m.age) " +
                        "FROM Member as m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : memberDtos) {
            log.info("memberName = {}, memberAge = {}", memberDto.getName(), memberDto.getAge());
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDto> memberDtos = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : memberDtos) {
            log.info("memberName = {}, memberAge = {}", memberDto.getName(), memberDto.getAge());
        }
    }

    @Test
    public void findDtoByField() {
        List<MemberDto> memberDtos = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : memberDtos) {
            log.info("memberName = {}, memberAge = {}", memberDto.getName(), memberDto.getAge());
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> memberDtos = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : memberDtos) {
            log.info("memberName = {}, memberAge = {}", memberDto.getName(), memberDto.getAge());
        }
    }

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> userDtos = queryFactory
                .select(Projections.fields(UserDto.class,
                                member.name.as("userName"), // ???????????? ?????? ???
                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub), "age") // ???????????? ????????? alias ??? ??? ??????????????? ?????? ???
                        )
                )
                .from(member)
                .fetch();

        for (UserDto userDto : userDtos) {
            log.info("userName = {}, age = {}", userDto.getUserName(), userDto.getAge());
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.name, member.age)) // ????????? ????????? ????????? ?????? ??? ??????
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            log.info("memberDto = {}", memberDto);
        }
    }

    @Test
    public void distinct() {
        em.persist(new Member("member1")); // ?????? ??????
        em.persist(new Member("member2"));
        em.persist(new Member("member5"));

        List<String> names = queryFactory
                .select(member.name).distinct()
                .from(member)
                .fetch();

        assertThat(names.size()).isEqualTo(5);
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {

        String nameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(nameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String nameCondition, Integer ageCondition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (nameCondition != null) {
            builder.and(member.name.eq(nameCondition));
        }
        if (ageCondition != null) {
            builder.and(member.age.eq(ageCondition));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {
        String nameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(nameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String nameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where(memberNameEq(nameParam), memberAgeEq(ageParam))
                .fetch();
    }

    private BooleanExpression memberNameEq(String nameCondition) {
        return nameCondition != null
                ? member.name.eq(nameCondition)
                : null;
    }

    private BooleanExpression memberAgeEq(Integer ageCondition) {
        return ageCondition != null
                ? member.age.eq(ageCondition)
                : null;
    }

    // ?????????, ?????? ??????
    private BooleanExpression allEq(String nameCondition, Integer ageCondition) {
        // ???????????? null ?????? ???????????????
        return memberNameEq(nameCondition).and(memberAgeEq(ageCondition));
    }

    @Test
    public void bulkUpdate() {
        long updatedRowCount = queryFactory
                .update(member)
                .set(member.name, "?????????")
                .set(member.age, 0) // ?????? ???????????? ????????? ????????? set??? ????????? ???????????? ??????.
                .where(member.age.lt(21))
                .execute();

        em.clear();
        // ?????? ?????? ????????? ????????? ??????????????? ????????? DB??? ????????? ????????? ????????? ??? ???. ????????? ??????????????? ?????????.

        assertThat(updatedRowCount).isEqualTo(2);

        List<Member> members = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member findMember : members) {
            log.info("findMember = {}", findMember);
        }
    }

    @Test
    public void bulkAddUpdate() {
        long updatedRowCount = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        em.clear();

        List<Member> members = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member findMember : members) {
            log.info("findMember = {}", findMember);
        }

        Integer memberAge1 = queryFactory
                .select(member.age)
                .from(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        assertThat(memberAge1).isEqualTo(11);
    }

    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
        em.clear();

        List<Member> members = queryFactory.selectFrom(member).fetch();

        for (Member findMember : members) {
            log.info("findMember = {}", findMember);
        }

        assertThat(count).isEqualTo(3);
        assertThat(members.size()).isEqualTo(1);
    }
}
