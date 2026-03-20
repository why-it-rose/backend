package com.whyitrose.apiserver.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 50)
        String nickname,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$", //영문 + 숫자 + 특수문자를 각각 최소 1개 이상 포함하고, 전체 길이는 8자 이상
                message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        String password
) {
}
