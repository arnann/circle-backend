package com.arnan.circle.exception;

import com.arnan.circle.common.ErrorCode;

public class BusinessException extends RuntimeException {

    /**
     * 异常码
     */
    private final Integer code;

    /**
     * 描述
     */
    private final String description;

    public BusinessException(String message, Integer code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }


    public String getDescription() {
        return description;
    }
}
