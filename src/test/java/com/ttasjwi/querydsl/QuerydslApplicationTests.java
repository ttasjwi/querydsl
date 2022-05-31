package com.ttasjwi.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ttasjwi.querydsl.hello.domain.Hello;
import com.ttasjwi.querydsl.hello.domain.QHello;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	private EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QHello qHello = QHello.hello;

		Hello result = query
				.selectFrom(qHello)
				.fetchOne();

		assertThat(result).isSameAs(hello);
		assertThat(result.getId()).isEqualTo(hello.getId());
	}

}
