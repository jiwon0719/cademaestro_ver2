package com.ssafy.codemaestro.domain.boj.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BojUserResponse {
    private String handle;
    private int tier;
}