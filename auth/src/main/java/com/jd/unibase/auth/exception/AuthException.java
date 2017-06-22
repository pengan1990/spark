package com.jd.unibase.auth.exception;

/**
 * Created by pengan on 17-6-21.
 */
public class AuthException extends RuntimeException {
    private final String errMsg;
    public AuthException(String msg) {
        this.errMsg = msg;
    }

    @Override
    public String toString() {
        StringBuilder errMsg = new StringBuilder();
        if (super.getMessage() != null) {
            errMsg.append(super.getMessage()).append("|");
        }
        if (this.errMsg != null) {
            errMsg.append(this.errMsg);
        }
        return errMsg.toString();
    }
}
