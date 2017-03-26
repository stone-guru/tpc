package net.eric.tpc.bankserver.persist;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcConnectionPool;

public class ConnectionPool {
    private static ConnectionPool pool;
    private JdbcConnectionPool jdbcPool;
    
    private ConnectionPool(){
        String dbPath = "/home/bison/workspace/tpc/db/bank";
        jdbcPool = JdbcConnectionPool.create("jdbc:h2:" + dbPath, "sa", "");
        jdbcPool.setMaxConnections(16);
    }
    
    public static ConnectionPool getInstance(){
        if(pool == null){
            pool = new ConnectionPool();
        }
        return pool;
    }
    
    public Connection getConnection() {
        try {
            return jdbcPool.getConnection();
        } catch (SQLException e) {
            throw new PersistException(e);
        }
    }
}
