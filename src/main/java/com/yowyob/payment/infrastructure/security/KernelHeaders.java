package com.yowyob.payment.infrastructure.security;

/**
 * Noms des headers kernel requis pour les appels API.
 */
public final class KernelHeaders {

    public static final String CLIENT_ID = "X-Client-Id";
    public static final String API_KEY = "X-Api-Key";
    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String ORGANIZATION_ID = "X-Organization-Id";
    public static final String CLIENT_CREDENTIALS_ATTR = "kernelClientCredentialsAuthenticated";

    private KernelHeaders() {
    }
}
