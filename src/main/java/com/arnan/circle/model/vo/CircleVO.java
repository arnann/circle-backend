package com.arnan.circle.model.vo;

import com.arnan.circle.model.domain.User;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class CircleVO implements Serializable {
    private Long id;

    /**
     * 圈子名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 图片
     */
    private String imageUrl;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建人
     */
    private User createUser;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否已加入
     */
    private Boolean hasJoin = false;


    /**
     * 圈子成员
     */
    private List<User> memberList;
}
