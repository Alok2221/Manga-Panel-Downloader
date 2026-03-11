package com.mangapanel.downloader.integration.translator;

import lombok.Getter;

@Getter
public class TranslatorClientException extends RuntimeException {

    private final String responseBody;

    public TranslatorClientException(String message) {
        super(message);
        this.responseBody = null;
    }

    public TranslatorClientException(String message, String responseBody) {
        super(message);
        this.responseBody = responseBody;
    }

    public TranslatorClientException(String message, Throwable cause) {
        super(message, cause);
        this.responseBody = null;
    }

}
