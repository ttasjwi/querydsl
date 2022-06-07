package com.ttasjwi.querydsl.member.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class UserDto {

    private String userName;
    private int age;

    public UserDto(String userName, int age) {
        this.userName = userName;
        this.age = age;
    }
}
