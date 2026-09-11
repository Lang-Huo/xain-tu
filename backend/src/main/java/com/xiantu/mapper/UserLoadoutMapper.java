package com.xiantu.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantu.entity.UserLoadout;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserLoadoutMapper extends BaseMapper<UserLoadout> {

    default List<UserLoadout> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<UserLoadout>()
                .eq(UserLoadout::getUserId, userId));
    }

    default UserLoadout selectByUserAndSlot(Long userId, String slot) {
        return selectOne(new LambdaQueryWrapper<UserLoadout>()
                .eq(UserLoadout::getUserId, userId)
                .eq(UserLoadout::getSlot, slot));
    }
}