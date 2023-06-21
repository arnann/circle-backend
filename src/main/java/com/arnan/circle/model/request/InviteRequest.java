package com.arnan.circle.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class InviteRequest implements Serializable {
    private Long userId;
    private Long circleId;
}
