package com.gitcode.mcsm_backend.config;

//测试阶段，上线需限制安全策略
import com.gitcode.mcsm_backend.service.userDetailsService;
import com.gitcode.mcsm_backend.util.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.Arrays;

/**
 * 安全配置 - 配置 Spring Security 过滤链、JWT 认证、CORS 和权限规则
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private userDetailsService userDetailsService;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private MyAuthenticationEntryPoint myAuthenticationEntryPoint;

    @Value("${security.permit-all-paths}")
    private String permitAllPaths;





    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 开启跨域并使用下面定义的 corsConfigurationSource
                .cors(Customizer.withDefaults())
                // 2. 禁用 CSRF（前后端分离项目必须禁用）
                .csrf(csrf -> csrf.disable())
                // 3. 设置为无状态 Session（JWT 模式的标准配置）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 4. 允许登录和注册接口匿名访问

                        .requestMatchers(permitAllPaths.split(",")).permitAll()
                        // 5. 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                //使用自定义的身份拦截
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(myAuthenticationEntryPoint)
                )

                .userDetailsService(userDetailsService);

        // 6. 将 JWT 过滤器放在用户名密码过滤器之前
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 核心修改：定义测试环境下的全开放跨域规则
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许所有来源（测试环境下使用 Pattern 可以绕过带 Credentials 的限制）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 允许所有 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许所有请求头（包括你前端传的 token 字段）
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许发送 Cookie 或授权信息
        configuration.setAllowCredentials(true);

        // 预检请求（OPTIONS）的缓存时间：1小时
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}