package cn.mario.aiplatform.exception;

import lombok.Getter;

/**
 * 业务异常处理
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = 0;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
