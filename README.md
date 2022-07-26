

## 결과 조회
- 리스트 : `fetch()`
  - 결과가 없으면 빈 리스트가 반환된다.
- 단건 : `fetchOne()`
  - 결과가 없으면 null
  - 결과가 한 건 있으면 단 건
  - 결과가 여러 건 있으면 예외 발생
- `fetchFirst()` : `limit(1).fetchOne()`
- `fetchResults()` : 페이징 정보가 담긴 객체 가져옴
  - **deprecated됨. 쓰지 말 것.** 
  - totalCount를 계산하는 쿼리와, 페이징 컨텐츠 가져오는 쿼리를 분리해서 쓸 것
- `fetchCount()` : count 쿼리로 변경해서 count 수 조회.
  - **deprecated됨. 쓰지 말 것.**
  - 명시적으로 count 쿼리를 작성하는 것이 낫다.

---

## 조인

```java
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
```
```sql
select
    member0_.member_id as member_i1_1_,
    member0_.age as age2_1_,
    member0_.name as name3_1_,
    member0_.team_id as team_id4_1_ 
from
    member member0_
cross join
    team team1_ 
where
    member0_.name=team1_.name
```
- 사용법 : join(조인대상, 앨리어스명 또는 Qtype)
  - 기본 조인 : join, innerJoin(내부조인)
  - 외부 조인(out join)
    - leftJoin, rightJoin
- 연관관계가 없는 필드도 조인 가능(from절에 복수의 테이블 지정 -> cross join)

---

## 문자열 더하기(concat)
```java
        String result = queryFactory
                .select(member.name.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.name.eq("member1"))
                .fetchOne();
```
- concat을 통해 문자열 결합. 체이닝을 통해 연속해서 덧붙일 수 있음
- 문자타입이 아닌 경우( **숫자**, **enum**) stringValue 활용해서 문자열로 변환해서 쓸 것

---

## SQLFunction 호출

### 함수 호출 방법
```java
List<String> result = queryFactory
        .select(Expressions.stringTemplate(
                "function('replace', {0}, {1}, {2})",
                member.name, "member", "M"))
        .from(member)
        .fetch();
```
> Expressions.stringTemplate("function('함수명', {0}, {1}, ...", 인자0, 인자1, ...))

1. 기본적으로 각 DB별 Dialect에 함수들 대다수가 내장되어 있음.
2. 별도로 정의한 함수는 Dialect 상속 클래스에 함수를 등록해야함.

### ANSI 표준 SQL 함수 호출
```java
        List<String> result = queryFactory
                .select(member.name)
                .from(member)
//                .where(member.name.eq(Expressions.stringTemplate(
//                        "function('lower', {0})", member.name)))
                .where(member.name.eq(member.name.lower()))
                .fetch();
```
- querydsl에 기본 내장되어 있는 경우가 많아서 웬만해선 메서드 호출하면 됨

---

## 순수 JPA와 Querydsl
```java
@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;


    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }
```
- JPAQueryFactory는 EntityManager를 의존한다.
- 그런데 EntityManager는 싱글톤이기 때문에 동시성 문제가 발생할 수 있지 않겠냐는 걱정이 들 수 있다.
  - 결론 : 여기서 등록되는 EntityManager는 가짜 프록시 엔티티 매니저
    - 가짜 엔티티 매니저는 실제 사용 시점에 요청마다 트랜잭션 단위로 실제 엔티티 매니저(영속성 컨텍스트)를 할당해준다.
    - 상세 내용 : 자바 ORM 표준 JPA 프로그래밍 책 13.1 참조

---

## 동적 쿼리와 성능 최적화 조회 - Builder 사용
```java
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
```
- if문을 통해 파라미터의 존재 여부 등에 따라 `BooleanBuilder`를 동적으로 구성
- BooleanBuilder를 where절 파라미터를 통해 넘기면 된다.

### 주의점
- 검색 파라미터에 아무 것도 들어가지 않을 경우 모든 엔티티를 조회하고 메모리에 끌어옴
- 메모리 부족으로 서버가 죽을 수 있음!!! 매우 주의해야한다. 적절하게 끊어서 가져올 수 있도록 페이징 쿼리를 작성해야한다.

---

## 동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용
```java
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
            ? member.age.eq(ageGoeCond)
            : null;
}

private BooleanExpression ageLoe(Integer ageLoeCond) {
    return ageLoeCond != null
            ? member.age.eq(ageLoeCond)
            : null;
}
```
- 검색 조건들을 별도의 메서드로 분리하고 Where절에서 `콤마(,)`로 묶어서 사용
- Where절에서 `BooleanExpression`은
  - 기본적으로 `and`로 묶임
  - null일 경우 무시됨
- 파라미터의 null 여부에만 주의하면 됨
- **분리한 BooleanExpression 반환 메서드는 재사용이 가능하다!**

---
