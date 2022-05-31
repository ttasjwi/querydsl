package com.ttasjwi.querydsl.hello.domain;

import lombok.Getter;

import javax.persistence.*;

@Entity
@Getter
public class Hello {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hello_id")
    private Long id;
}
