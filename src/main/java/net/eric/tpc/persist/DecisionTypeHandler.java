package net.eric.tpc.persist;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import net.eric.tpc.proto.Decision;

public class DecisionTypeHandler extends BaseTypeHandler<Decision> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Decision parameter, JdbcType jdbcType)
            throws SQLException {
        if(parameter == null){
            ps.setString(i, null);
        }
        ps.setString(i, parameter.code());
    }

    @Override
    public Decision getNullableResult(ResultSet rs, String columnName) throws SQLException {
        if(rs.wasNull()){
            return null;
        }
        String code = rs.getString(columnName);
        return this.typeFromString(code);
    }

    @Override
    public Decision getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        if(rs.wasNull()){
            return null;
        }
        String code = rs.getString(columnIndex);
        return this.typeFromString(code);
    }

    @Override
    public Decision getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        if(cs.wasNull()){
            return null;
        }
        String code = cs.getString(columnIndex);
                
        return this.typeFromString(code);
    }
    
    private Decision typeFromString(String code){
        Decision t = Decision.fromCode(code);
        if(t == null){
            throw new PersistException("Unknown code for Decision(" + code + ")");
        }
        return t;
    }

}
