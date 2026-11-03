package com.ncu.gym.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一转换为 Result
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(409, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
