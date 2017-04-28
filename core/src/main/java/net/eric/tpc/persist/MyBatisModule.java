package net.eric.tpc.persist;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;

import net.eric.tpc.base.NightWatch;

public class MyBatisModule implements Module {

    public static SqlSession createSession(String configFilePath, String jdbcUrl) {
        Preconditions.checkNotNull(jdbcUrl);
        Reader reader = null;
        try {
            reader = Resources.getResourceAsReader(configFilePath);
        } catch (IOException e) {
            throw new RuntimeException("MyBatis config file not  found", e);
        }

        Properties properties = new Properties();
        properties.put("url", jdbcUrl);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        
        NightWatch.regCloseable("MyBatis Session", sqlSession);
        
        return sqlSession;
    }
    
    private SqlSession sqlSession;
    private Class<?>[] daoClasses;
    
    public MyBatisModule(SqlSession sqlSession, Class<?>[] daoClasses) {
        Preconditions.checkNotNull(daoClasses);

        this.sqlSession = sqlSession;
        this.daoClasses = daoClasses;
    }

    @Override
    public void configure(Binder binder) {
        for (Class<?> t : this.daoClasses)
            bindDao(binder, t);
    }

    private <T> void bindDao(Binder binder, Class<T> c) {
        binder.bind(c).toProvider(new DaoProvider<T>(c));
    }

    private final class DaoProvider<T> implements Provider<T> {
        private Class<T> clz;

        public DaoProvider(Class<T> c) {
            this.clz = c;
        }

        @Override
        public T get() {
            return sqlSession.getMapper(clz);
        }
    }
}
