

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

