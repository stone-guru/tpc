package net.eric.tpc.service;

import com.google.inject.Binder;
import com.google.inject.EricTypeLiteral;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public class GuiceTest {
    public static interface Manager<B> {
        int function(B b);
    }
    
    public static class ManagerImpl<B> implements Manager<B> {
        @Override
        public int function(B b) {
            return b.hashCode();
        }
    }
    
    public static interface Repository<B> {
        int procedure(B b);
    }
    
    public static class RepositoryImpl<B> implements Repository<B>{
        @Inject
        private Manager<B> manager;
        
        @Override
        public int procedure(B b) {
            return b.hashCode() + manager.function(b);
        }
    }
    
    public static class Service<B> {
        Repository<B> repo;
        
        @Inject
        public Service(Repository<B> repo){
            this.repo = repo;
        }
        
        public int serve(B b){
            return this.repo.procedure(b);
        }
    }
    
    public static class ServiceModule<T> implements Module{
        private final Class<T> clz;
        
        public ServiceModule(Class<T> c){
            this.clz = c;
        }
        
        @Override
        public void configure(Binder binder) {
            binder.bind(new EricTypeLiteral(Manager.class, clz)).to(new EricTypeLiteral(ManagerImpl.class, clz));
            //binder.bind(new TypeLiteral<Repository<Integer>>(){}).to(new TypeLiteral<RepositoryImpl<Integer>>(){});
            binder.bind(new EricTypeLiteral(Repository.class, clz)).to(new EricTypeLiteral(RepositoryImpl.class, clz));
            binder.bind(new EricTypeLiteral(Service.class, clz));
        }
    }
    
    public static void main(String[] args) {
        Injector in = Guice.createInjector(new ServiceModule<Integer>(Integer.class));
        Service<Integer> s = in.getInstance(new Key<Service<Integer>>(){});
        System.out.println(s.serve(100));
    }
}
