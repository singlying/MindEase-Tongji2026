package com.mindease.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.common.constant.JwtClaimsConstant;
import com.mindease.common.constant.StatusConstant;
import com.mindease.common.result.Result;
import com.mindease.common.utils.JwtUtil;
import com.mindease.mapper.UserMapper;
import com.mindease.pojo.entity.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT Token 拦截器
 */
@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Value("${mindease.jwt.secret-key}")
    private String secretKey;

    @Autowired
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 待审核咨询师允许访问的接口白名单
     */
    private static final List<String> PENDING_COUNSELOR_WHITELIST = Arrays.asList(
            "/counselor/audit/submit",
            "/counselor/audit/status",
            "/auth/profile"  // 允许查看个人信息
    );

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader("token");

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(secretKey, token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            String role = claims.get(JwtClaimsConstant.ROLE).toString();
            log.info("当前用户id：{}，角色：{}", userId, role);

            //3、检查待审核咨询师的访问权限
            if ("COUNSELOR".equalsIgnoreCase(role)) {
                User user = userMapper.getById(userId);
                if (user != null && user.getStatus() == StatusConstant.PENDING) {
                    // 待审核咨询师，检查是否访问允许的接口
                    String requestURI = request.getRequestURI();
                    if (!isAllowedForPendingCounselor(requestURI)) {
                        log.warn("待审核咨询师尝试访问受限接口，用户ID:{}，接口:{}", userId, requestURI);
                        sendForbiddenResponse(response, "账号待审核，暂时只能访问审核相关接口");
                        return false;
                    }
                }
            }
            
            //4、通过，放行，并将用户id和角色存入request
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);
            return true;
        } catch (Exception ex) {
            //5、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }

    /**
     * 检查待审核咨询师是否允许访问该接口
     */
    private boolean isAllowedForPendingCounselor(String requestURI) {
        for (String allowedPath : PENDING_COUNSELOR_WHITELIST) {
            if (requestURI.equals(allowedPath) || requestURI.startsWith(allowedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发送403 Forbidden响应
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<Object> result = Result.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}

