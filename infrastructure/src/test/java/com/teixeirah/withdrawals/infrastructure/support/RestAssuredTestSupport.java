package com.teixeirah.withdrawals.infrastructure.support;

import io.restassured.RestAssured;

public final class RestAssuredTestSupport {

    private static final String LOCALHOST = "http://localhost";

    private RestAssuredTestSupport() {
    }

    public static void configureForPort(int port) {
        RestAssured.baseURI = LOCALHOST;
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
