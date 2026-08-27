package com.sp.api.vtuber.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChannelProfileRequest {

    /** 팬이 채팅에서 달고 다닐 표식. 이미지 주소 (/api/files/upload 로 올린 것). */
    private String oshiMarkUrl;

    @Size(max = 30, message = "팬네임은 30자 이하여야 합니다.")
    private String fanName;

    private LocalDate debutOn;

    private LocalDate graduatedOn;

    /** 통째로 갈아 끼운다. 보내지 않으면 기존 크레딧을 그대로 둔다. */
    @Valid
    @Size(max = 20, message = "크레딧은 20줄까지 넣을 수 있습니다.")
    private List<ModelCreditPayload> credits;
}
