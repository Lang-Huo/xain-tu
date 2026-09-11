package com.xiantu.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantu.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 通过 username 查询用户。
     * 使用 BaseMapper.selectOne + LambdaQueryWrapper，确保 MyBatis-Plus 的自动映射
     * （包括 @TableField(value="spirit_root_code", typeHandler=StringListJsonTypeHandler.class)）
     * 一定生效——避免自定义 @Select("SELECT * ...") 在某些 MyBatis-Plus 版本下不应用 TableInfo 自动映射的坑。
     */
    default User selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    /**
     * 通过 user_number 查询用户（同上理由：使用 BaseMapper 自动映射）。
     */
    default User selectByUserNumber(String userNumber) {
        return selectOne(new LambdaQueryWrapper<User>().eq(User::getUserNumber, userNumber));
    }
}
