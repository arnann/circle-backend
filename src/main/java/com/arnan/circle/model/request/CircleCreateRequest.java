package com.arnan.circle.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CircleCreateRequest implements Serializable {

    private long id;

    private String searchText;

    private String name;

    private String description;

    private String imageUrl;

    private Integer maxNum;

    private Date expireTime;

    private Integer status;

    private String password;
}
