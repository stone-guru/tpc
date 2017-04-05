package net.eric.tpc.persist;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UniFactory;

public class PersisterFactory extends UniFactory {

    public static String MY_BATIS_CONFIG_FILE = "net/eric/tpc/persist/mapper/tpc-config.xml";

    private SqlSessionFactory sqlSessionFactory;
    private SqlSession sqlSession;

    public PersisterFactory(String jdbcUrl) {
        Preconditions.checkNotNull(jdbcUrl);
        Reader reader = null;
        try {
            reader = Resources.getResourceAsReader(MY_BATIS_CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("MyBatis config file not  found", e);
        }

        Properties properties = new Properties();
        properties.put("url", jdbcUrl);
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
        this.sqlSession = sqlSessionFactory.openSession(true);
    }

    @Override
    protected <T> Optional<Pair<T, Boolean>> createObject(Class<T> clz, Object classifier) {
        if (!sqlSessionFactory.getConfiguration().hasMapper(clz)) {
            return Optional.absent();
        }
        T mapper = sqlSession.getMapper(clz);
        return Pair.of(mapper, false);
    }
}
