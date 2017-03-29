package net.eric.tpc.persist;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class PersisterFactory {
    public static String DEFAULT_JDBC_URL = "jdbc:h2:tcp://localhost:9100/bank";
    public static String MY_BATIS_CONFIG_FILE = "net/eric/tpc/persist/mapper/tpc-config.xml";

    static private SqlSessionFactory sqlSessionFactory;
    static private SqlSession sqlSession;

    public static void initialize(String jdbcUrl) {
        Reader reader = null;
      
        try {
            reader = Resources.getResourceAsReader(MY_BATIS_CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("MyBatis config file not  found", e);
        }

        Properties properties = new Properties();
        properties.put("url", jdbcUrl);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
        //sqlSessionFactory.getConfiguration().getEnvironment().getDataSource().
    }

    public static SqlSession getSession() {
        if (sqlSession != null) {
            return sqlSession;
        }
        synchronized (PersisterFactory.class) {
            if (sqlSession == null) {
                if(sqlSessionFactory == null){
                    throw new IllegalStateException("not initialized, call MyBatisSessionFactory.init first");
                }
                sqlSession = sqlSessionFactory.openSession(true);
            }
            return sqlSession;
        }
    }
    
    public static <T> T getMapper(Class<T> clz){
        return getSession().getMapper(clz);
    }
}
