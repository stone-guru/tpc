package net.eric.bank.persist;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import net.eric.bank.entity.AccountType;
import net.eric.tpc.persist.PersistException;

public class AccountTypeHandler extends BaseTypeHandler<AccountType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AccountType parameter, JdbcType jdbcType)
            throws SQLException {
        if(parameter == null){
            ps.setString(i, null);
        }
        ps.setString(i, parameter.code());
    }

    @Override
    public AccountType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        if(rs.wasNull()){
            return null;
        }
        String code = rs.getString(columnName);
        return this.typeFromString(code);
    }

    @Override
    public AccountType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        if(rs.wasNull()){
            return null;
        }
        String code = rs.getString(columnIndex);
        return this.typeFromString(code);
    }

    @Override
    public AccountType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        if(cs.wasNull()){
            return null;
        }
        String code = cs.getString(columnIndex);
                
        return this.typeFromString(code);
    }
    
    private AccountType typeFromString(String code){
        AccountType t = AccountType.fromCode(code);
        if(t == null){
            throw new PersistException("Unknown code for AccountType(" + code + ")");
        }
        return t;
    }

}
