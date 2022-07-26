package com.ttasjwi.querydsl.member.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ttasjwi.querydsl.member.domain.Member;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static com.ttasjwi.querydsl.member.domain.QMember.member;

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
}
