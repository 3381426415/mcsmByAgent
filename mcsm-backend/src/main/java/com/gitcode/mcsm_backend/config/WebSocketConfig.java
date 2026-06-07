package com.gitcode.mcsm_backend.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.mapper.MyUserMapper;
import com.gitcode.mcsm_backend.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

/**
 * WebSocket 配置 - 配置 STOMP 消息代理、端点和频道拦截器
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    @Autowired
    private MyUserMapper  myUserMapper;

    @Autowired
    private JwtUtils jwtUtils;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 从 CONNECT 帧的头中提取 token（注意！现在是 STOMP 帧头）
                    String token = accessor.getFirstNativeHeader("token");
                    if (token == null || token.isEmpty()) {
                        log.error("[STOMP] CONNECT 缺少 token");
                        // 拒绝连接：可以抛出异常或返回 null（后者会断开连接）
                        throw new org.springframework.messaging.MessageDeliveryException("缺少认证 token");
                    }

                    // 解析 token（复用 JwtUtils）
                    String username = jwtUtils.getUsernameFromToken(token);
                    if (username == null) {
                        log.error("[STOMP] token 无效或过期");
                        throw new org.springframework.messaging.MessageDeliveryException("token 无效");
                    }

                    // 验证用户存在
                    MyUser user = myUserMapper.selectOne(
                            new LambdaQueryWrapper<MyUser>()
                                    .eq(MyUser::getUsername, username)
                    );
                    if (user == null) {
                        log.error("[STOMP] 用户不存在: {}", username);
                        throw new org.springframework.messaging.MessageDeliveryException("用户不存在");
                    }

                    // 将用户信息保存到 STOMP session 中，供后续使用
                    accessor.getSessionAttributes().put("userId", user.getId().toString());
                    accessor.getSessionAttributes().put("username", username);
                    // 设置 Principal，以便在控制器中直接注入
                    accessor.setUser(new Principal() {
                        @Override
                        public String getName() {
                            return username;
                        }
                    });
                    log.info("[STOMP] CONNECT 认证成功: userId={}", user.getId());
                }
                return message;
            }
        });
    }









}