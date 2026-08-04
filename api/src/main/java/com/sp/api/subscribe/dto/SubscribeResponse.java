package com.sp.api.subscribe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubscribeResponse {

    private boolean subscribed;
    private long subscriberCount;
}
