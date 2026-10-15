package com.chuseok22.whoim.server.auth.infrastructure.config;

public final class CorsOrigins {

  public static final String[] ALLOWED_ORIGINS = {
      "http://localhost:5173",
      "http://localhost:3000",
      "https://www.whoim.chuseok22.com"
  };

  private CorsOrigins() {
  }
}
