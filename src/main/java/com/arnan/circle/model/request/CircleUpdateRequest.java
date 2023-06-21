package com.arnan.circle.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CircleUpdateRequest implements Serializable {

    private Long id;

    private String name;

    private String description;

    private String imageUrl;

    private Integer maxNum;

    private Date expireTime;

    private Integer status;

    private String password;
}
