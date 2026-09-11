package com.xiantu.service;

import com.xiantu.common.BizException;
import com.xiantu.common.JwtUtil;
import com.xiantu.entity.User;
import com.xiantu.mapper.UserMapper;
import com.xiantu.web.dto.AuthResponse;
import com.xiantu.web.dto.LoginRequest;
import com.xiantu.web.dto.RegisterRequest;
import com.xiantu.web.dto.SpiritRootInfo;
import com.xiantu.web.dto.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoadoutService loadoutService;

    public UserService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       LoadoutService loadoutService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loadoutService = loadoutService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userMapper.selectByUsername(req.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        User user = new User(req.getUsername(), passwordEncoder.encode(req.getPassword()));
        // 昵称缺省时使用用户名
        String nick = (req.getNickname() != null && !req.getNickname().isBlank())
                ? req.getNickname().trim() : req.getUsername();
        user.setNickname(nick);
        // 系统分配 6 位全局唯一编号，首位非 0
        user.setUserNumber(generateUserNumber());
        // 灵力初始值
        user.setMana(50);
        user.setMaxMana(50);
        // 灵根留空（M2 测试后写入）
        user.setSpiritRoots(new ArrayList<>());
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        // 给新用户装上初始装备（青竹剑 / 祖布囊）
        try {
            loadoutService.equipStarterLoadout(user.getId());
        } catch (Exception e) {
            // 初始装备失败不应该阻塞注册；但要留 stack trace 便于排查
            e.printStackTrace();
        }
        return AuthResponse.of(jwtUtil.generate(user.getUsername()), user);
    }

    /** 生成 100000-999999 的 6 位数字编号，确保全局唯一（首位非 0） */
    private String generateUserNumber() {
        SecureRandom rnd = new SecureRandom();
        for (int attempt = 0; attempt < 20; attempt++) {
            int n = 100000 + rnd.nextInt(900000);
            String num = String.valueOf(n);
            if (userMapper.selectByUserNumber(num) == null) {
                return num;
            }
        }
        throw new BizException("仙途编号分配失败，请稍后重试");
    }

    public AuthResponse login(LoginRequest req) {
        User user = userMapper.selectByUsername(req.getUsername());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException("密码错误");
        }
        return AuthResponse.of(jwtUtil.generate(user.getUsername()), user);
    }

    public UserProfile me(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        UserProfile profile = UserProfile.of(user);

        // 用户灵根直接读 user.spiritRoots（JSON 字符串数组），按静态映射解析中文名
        List<String> codes = user.getSpiritRoots() != null ? user.getSpiritRoots() : List.of();
        if (!codes.isEmpty()) {
            List<SpiritRootInfo> infos = new ArrayList<>();
            for (String code : codes) {
                infos.add(SpiritRootInfo.of(code));
            }
            profile.setSpiritRoots(infos);
        }
        return profile;
    }
}