package com.tts.monitor.exception;

/**
 * TTS API 异常类
 * 
 * @author TTS Monitor System
 * @since 2026-01-23
 */
public class TtsApiException extends RuntimeException {

    private Integer code;

    public TtsApiException(String message) {
        super(message);
    }

    public TtsApiException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public TtsApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
