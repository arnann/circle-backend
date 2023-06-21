package com.arnan.circle.enums;

public enum CircleStatusEnum {
    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");
    private int value;
    private String text;

    CircleStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static CircleStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (CircleStatusEnum circleStatusEnum : CircleStatusEnum.values()) {
            if (circleStatusEnum.value == value) {
                return circleStatusEnum;
            }
        }
        return null;
    }
}
