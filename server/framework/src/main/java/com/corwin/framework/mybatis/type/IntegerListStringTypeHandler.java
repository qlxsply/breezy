package com.corwin.framework.mybatis.type;

import com.corwin.framework.jpa.IntegerListStringConverter;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * MyBatis {@link org.apache.ibatis.type.TypeHandler} that maps {@code List<Integer>}
 * to/from a comma-separated string column.
 *
 * @author Corwin 2026/7/28
 */
public class IntegerListStringTypeHandler extends BaseTypeHandler<List<Integer>> {

    private final IntegerListStringConverter converter = new IntegerListStringConverter();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Integer> parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, converter.convertToDatabaseColumn(parameter));
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return converter.convertToEntityAttribute(rs.getString(columnName));
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return converter.convertToEntityAttribute(rs.getString(columnIndex));
    }

    @Override
    public List<Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return converter.convertToEntityAttribute(cs.getString(columnIndex));
    }
}
