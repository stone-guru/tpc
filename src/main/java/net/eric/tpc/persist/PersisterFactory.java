package net.eric.tpc.persist;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import com.google.common.base.Preconditions;

import net.eric.tpc.common.UniFactory;

public class PersisterFactory extends UniFactory {

    public static void register()  {
        UniFactory.register(PersisterFactory.class, 0);
    }
    
    public static String DEFAULT_JDBC_URL = "jdbc:h2:tcp://localhost:9100/bank";
    public static String MY_BATIS_CONFIG_FILE = "net/eric/tpc/persist/mapper/tpc-config.xml";

    private SqlSessionFactory sqlSessionFactory;
    private SqlSession sqlSession;

    
    @Override
    protected void init(Object param){
        Preconditions.checkNotNull(param);
        String jdbcUrl = param.toString();
        
        Reader reader = null;
        try {
            reader = Resources.getResourceAsReader(MY_BATIS_CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("MyBatis config file not  found", e);
        }

        Properties properties = new Properties();
        properties.put("url", jdbcUrl);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
        sqlSession = sqlSessionFactory.openSession(true);
    }
    
    @Override
    protected <T> T createObject(Class<T> clz, String classifer) {
        if(!sqlSessionFactory.getConfiguration().hasMapper(clz)){
            return null;
        }
            
        return sqlSession.getMapper(clz);
    }
}
