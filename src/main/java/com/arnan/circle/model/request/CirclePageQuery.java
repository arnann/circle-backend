package com.arnan.circle.model.request;

import lombok.Data;

@Data
public class CirclePageQuery extends CircleSearchRequest {
    private long pageNum = 1;
    private long pageSize = 20;
}
