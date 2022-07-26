package com.ttasjwi.querydsl.member.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ttasjwi.querydsl.member.domain.Member;
import com.ttasjwi.querydsl.member.dto.MemberSearchCondition;
import com.ttasjwi.querydsl.member.dto.MemberTeamDto;
import com.ttasjwi.querydsl.member.dto.QMemberTeamDto;
import com.ttasjwi.querydsl.team.domain.QTeam;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static com.ttasjwi.querydsl.member.domain.QMember.member;
import static com.ttasjwi.querydsl.team.domain.QTeam.*;
import static org.springframework.util.StringUtils.*;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;


    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    /**
     * 회원 등록
     */
    public void save(Member member) {
        em.persist(member);
    }

    /**
     * 식별자로 회원 조회
     */
    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    /**
     * 회원 전체 조회
     */
    public List<Member> findAll() {
        return em.createQuery("SELECT m FROM Member as m", Member.class)
                .getResultList();
    }

    /**
     * 회원 이름으로 조회
     */
    public List<Member> findByName(String name) {
        return em.createQuery(
                        "SELECT m " +
                                "FROM Member as m " +
                                "WHERE m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }

    /**
     * Querydsl 사용 - 사용자 전체 조회
     */
    public List<Member> findAll_Querydsl() {
        return queryFactory
                .select(member)
                .from(member)
                .fetch();
    }

    /**
     * Querydsl 사용 - 회원 이름으로 조회
     */
    public List<Member> findByName_Querydsl(String name) {
        return queryFactory.select(member)
                .from(member)
                .where(member.name.eq(name))
                .fetch();
    }

    /**
     * Builder를 사용한 동적 쿼리
     * 회원명, 팀명, 나이(ageGoe, ageLoe)
     */
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getMemberName())) {
            builder.and(member.name.eq(condition.getMemberName()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }
        return queryFactory.select(new QMemberTeamDto(
                member.id, member.name, member.age, team.id, team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    /**
     * Builder를 사용한 동적 쿼리 - Where절 파라미터 사용
     * 회원명, 팀명, 나이(ageGoe, ageLoe)
     */
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(member.id, member.name, member.age, team.id, team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        memberNameEq(condition.getMemberName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    private BooleanExpression memberNameEq(String memberNameCond) {
        return hasText(memberNameCond)
                ? member.name.eq(memberNameCond)
                : null;
    }
    private BooleanExpression teamNameEq(String teamNameCond) {
        return hasText(teamNameCond)
                ? team.name.eq(teamNameCond)
                : null;
    }

    private BooleanExpression ageGoe(Integer ageGoeCond) {
        return ageGoeCond != null
                ? member.age.goe(ageGoeCond)
                : null;
    }

    private BooleanExpression ageLoe(Integer ageLoeCond) {
        return ageLoeCond != null
                ? member.age.loe(ageLoeCond)
                : null;
    }
}
