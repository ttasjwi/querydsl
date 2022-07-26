package com.ttasjwi.querydsl.member.web;

import com.ttasjwi.querydsl.member.dto.MemberSearchCondition;
import com.ttasjwi.querydsl.member.dto.MemberTeamDto;
import com.ttasjwi.querydsl.member.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(@ModelAttribute MemberSearchCondition condition) {
        return memberJpaRepository.search(condition);
    }
}
