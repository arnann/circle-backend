package com.arnan.circle.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CircleJoinRequest implements Serializable {
    private Long circleId;
    private String password;
}
