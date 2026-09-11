package com.xiantu.common.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * t_user.spirit_root_code 的 JSON 类型处理器（List&lt;String&gt; ↔ VARCHAR）：
 * 用户灵根的 code 数组，如 ["GOLD","WOOD"]；元素个数即用户灵根数量，无主副之分。
 *
 * <p>注册方式：
 * <ol>
 *   <li>{@code mybatis-plus.type-handlers-package} 自动扫描本包</li>
 *   <li>{@link MappedTypes} 显式声明 Java 类型，让 MyBatis 在自动注册时知道把 List 映射到这个 handler</li>
 *   <li>{@code @TableField(typeHandler=...)} 在字段上显式指定</li>
 * </ol>
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<List<String>>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("序列化灵根JSON失败", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<String> parse(String s) throws SQLException {
        if (s == null || s.isEmpty()) return new ArrayList<>();
        try {
            return MAPPER.readValue(s, TYPE);
        } catch (Exception e) {
            throw new SQLException("反序列化灵根JSON失败: " + s, e);
        }
    }
}