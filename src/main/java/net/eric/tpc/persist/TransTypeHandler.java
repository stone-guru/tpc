package net.eric.tpc.persist;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import net.eric.tpc.entity.TransType;

public class TransTypeHandler extends BaseTypeHandler<TransType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TransType parameter, JdbcType jdbcType)
            throws SQLException {
        if(parameter == null){
            ps.setString(i, null);
        }
        ps.setString(i, parameter.code());
    }

    @Override
    public TransType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        if(rs.wasNull()){
            return null;
        }
        String code = rs.getString(columnName);
        return this.typeFromString(code);
    }

    @Override
    public TransType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        if(rs.wasNull()){
            return null;
        }
        String code = rs.getString(columnIndex);
        return this.typeFromString(code);
    }

    @Override
    public TransType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        if(cs.wasNull()){
            return null;
        }
        String code = cs.getString(columnIndex);
                
        return this.typeFromString(code);
    }
    
    private TransType typeFromString(String code){
        TransType t = TransType.fromCode(code);
        if(t == null){
            throw new PersistException("Unknown code for MessageType(" + code + ")");
        }
        return t;
    }

}
