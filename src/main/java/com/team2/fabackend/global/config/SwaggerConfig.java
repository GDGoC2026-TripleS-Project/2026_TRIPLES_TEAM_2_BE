package com.team2.fabackend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT";
        String confirmTokenSchemeName = "Confirm Token";

        // 1. JWT Bearer 설정
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        // 2. Confirm Token 설정 (주석 해제 및 이름 확정)
        SecurityScheme confirmTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-Password-Confirm-Token");

        SecurityRequirement jwtOnly = new SecurityRequirement()
                .addList(jwtSchemeName);

        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, jwtScheme)
                .addSecuritySchemes(confirmTokenSchemeName, confirmTokenScheme);

        return new OpenAPI()
                .info(new Info()
                        .title("Toktory API for Android")
                        .description("""
                                <h2>🚀 Toktory 백엔드 API 명세서 (Android 전용)</h2>
                                <p>이 문서는 안드로이드 개발자를 위해 최적화된 API 가이드를 제공합니다.</p>
                                
                                <h3>🔑 인증 및 보안 가이드</h3>
                                <ol>
                                    <li><b>Access Token (JWT)</b>: 로그인 성공 시 발급되며, 모든 API 요청 시 <code>Authorization: Bearer {token}</code> 헤더에 포함해야 합니다.</li>
                                    <li><b>Refresh Token</b>: Access Token 만료 시 <code>/auth/refresh</code>를 통해 갱신하며, 보안을 위해 <b>EncryptedSharedPreferences</b> 또는 <b>DataStore</b>에 저장하는 것을 권장합니다.</li>
                                    <li><b>Confirm Token</b>: 비밀번호 변경, 회원 탈퇴 등 민감한 작업 시 2차 검증 후 발급되는 짧은 수명의 토큰입니다. <code>X-Password-Confirm-Token</code> 헤더에 포함하세요.</li>
                                </ol>
                                
                                <h3>🛠️ 개발 참고사항</h3>
                                <ul>
                                    <li><b>Retrofit2</b> 사용 시 <code>Response<Unit></code> 또는 <code>Deferred<Response<T>></code> 형식을 권장합니다.</li>
                                    <li>에러 발생 시 <code>ErrorResponse</code> 객체가 반환되므로 공통 에러 핸들러를 구성하세요.</li>
                                </ul>
                                """)
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url("https://dontory.duckdns.org").description("운영 서버"),
                        new Server().url("http://localhost:8080").description("로컬 테스트")
                ))
                .addSecurityItem(jwtOnly)
                .components(components);
    }
}