package com.mindease.controller.admin;

import com.mindease.common.constant.JwtClaimsConstant;
import com.mindease.common.result.Result;
import com.mindease.common.utils.JwtUtil;
import com.mindease.pojo.dto.UserLoginDTO;
import com.mindease.pojo.entity.User;
import com.mindease.pojo.vo.UserLoginVO;
import com.mindease.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 *  超级管理员管理员工
 */
@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

    @Autowired
    private UserService userService;

    @Value("${mindease.jwt.secret-key}")
    private String secretKey;

    @Value("${mindease.jwt.ttl}")
    private Long ttl;

    /**
     * 管理员登录
     *
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("管理端登录:{}", userLoginDTO);

        User user = userService.login(userLoginDTO, true);

        // 生成 JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.ROLE, user.getRole());
        String token = JwtUtil.createJWT(secretKey, ttl, claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .build();

        return Result.success(userLoginVO);
    }
}
