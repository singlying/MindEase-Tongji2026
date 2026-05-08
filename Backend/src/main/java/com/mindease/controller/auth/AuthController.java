package com.mindease.controller.auth;

import com.mindease.common.constant.JwtClaimsConstant;
import com.mindease.common.constant.MessageConstant;
import com.mindease.common.constant.StatusConstant;
import com.mindease.common.result.Result;
import com.mindease.common.utils.JwtUtil;
import com.mindease.pojo.dto.UserLoginDTO;
import com.mindease.pojo.dto.UserRegisterDTO;
import com.mindease.pojo.dto.UserUpdateDTO;
import com.mindease.pojo.entity.User;
import com.mindease.pojo.vo.UserLoginVO;
import com.mindease.pojo.vo.UserProfileVO;
import com.mindease.pojo.vo.UserRegisterVO;
import com.mindease.pojo.vo.UserUpdateVO;
import com.mindease.service.AppointmentService;
import com.mindease.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户认证控制器
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Value("${mindease.jwt.secret-key}")
    private String secretKey;

    @Value("${mindease.jwt.ttl}")
    private Long ttl;

    /**
     * 用户注册
     *
     * @param userRegisterDTO
     * @return
     */
    @PostMapping("/register")
    public Result<UserRegisterVO> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        log.info("用户注册:{}", userRegisterDTO);

        User user = userService.register(userRegisterDTO);

        // 无论是普通用户还是咨询师，都生成 token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.ROLE, user.getRole());
        String token = JwtUtil.createJWT(secretKey, ttl, claims);

        // 根据用户类型返回不同的消息
        String message = "success";
        if (user.getStatus() == StatusConstant.PENDING) {
            // 咨询师注册
            message = MessageConstant.COUNSELOR_REGISTER_SUCCESS;
        }

        UserRegisterVO userRegisterVO = UserRegisterVO.builder()
                .userId(user.getId())
                .role(user.getRole().toLowerCase())
                .status(user.getStatus())
                .token(token)
                .build();

        return Result.success(userRegisterVO, message);
    }

    /**
     * 用户登录
     *
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录:{}", userLoginDTO);

        User user = userService.login(userLoginDTO, false);

        // 用户登录时，自动更新该用户已过期的预约状态
        try {
            appointmentService.autoCompleteExpiredAppointments(user.getId());
        } catch (Exception e) {
            log.error("自动更新预约状态失败", e);
            // 不影响登录流程
        }

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

    /**
     * 获取个人信息
     *
     * @param userId 从 JWT token 中解析出来的用户ID
     * @return
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile(@RequestAttribute Long userId) {
        log.info("获取个人信息，用户ID:{}", userId);

        User user = userService.getById(userId);

        UserProfileVO userProfileVO = UserProfileVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .createTime(user.getCreateTime())
                .build();

        return Result.success(userProfileVO);
    }

    /**
     * 更新个人信息
     *
     * @param userId        从 JWT token 中解析出来的用户ID
     * @param userUpdateDTO
     * @return
     */
    @PutMapping("/profile")
    public Result<UserUpdateVO> updateProfile(@RequestAttribute Long userId,
                                               @RequestBody UserUpdateDTO userUpdateDTO) {
        log.info("更新个人信息，用户ID:{}，更新内容:{}", userId, userUpdateDTO);

        userService.updateProfile(userId, userUpdateDTO);

        UserUpdateVO userUpdateVO = UserUpdateVO.builder()
                .success(true)
                .build();

        return Result.success(userUpdateVO);
    }
}

