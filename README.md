

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
