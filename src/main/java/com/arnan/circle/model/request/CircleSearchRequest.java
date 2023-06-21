package com.arnan.circle.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CircleSearchRequest implements Serializable {
    private Long id;

    private String searchText;
    /**
     * 圈子名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
}
