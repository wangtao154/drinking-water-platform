package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名、工号或手机号查询用户
     */
    default SysUser selectByAccount(String account) {
        return this.selectOne(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, account)
                .or().eq(SysUser::getEmployeeNo, account)
                .or().eq(SysUser::getPhone, account)
                .last("LIMIT 1")
        );
    }

    /**
     * 更新最后登录信息
     */
    @Update("UPDATE sys_user SET last_login_ip = #{ip}, last_login_at = NOW() WHERE id = #{userId}")
    int updateLastLogin(@Param("userId") Long userId, @Param("ip") String ip);
}
