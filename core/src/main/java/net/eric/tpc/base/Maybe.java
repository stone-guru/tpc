package net.eric.tpc.base;

import java.util.concurrent.Future;

public abstract class Maybe<T> extends Either<ActionStatus, T> {
    public static <T> Maybe<T> success(T t) {
        if (t == null) {
            throw new NullPointerException("Maybe.success should not be null");
        }
        return new Success<T>(t);
    }

    public static <T> Maybe<T> fail(ActionStatus status) {
        if (status == null) {
            throw new NullPointerException("Maybe.status should not be null");
        }
        return new Failure<T>(status);
    }

    public static <T> Maybe<T> fail(short code, String description) {
        return new Failure<T>(ActionStatus.create(code, description));
    }

    public static <T> Maybe<T> fromNullable(ActionStatus status, T v) {
        if (v == null) {
            return Maybe.fail(status);
        } else {
            return Maybe.success(v);
        }
    }

    public static <T> Maybe<T> fromNullable(T v, short code, String description) {
        if (v == null) {
            return Maybe.fail(code, description);
        } else {
            return Maybe.success(v);
        }
    }
    
    public static <T> Maybe<T> might(ActionStatus status, T v) {
        if (status.isOK()) {
            return Maybe.success(v);
        }
        return Maybe.fail(status);
    }

    
    public static <T> Maybe<T> safeCast(Object obj, Class<T> c, short errorCode, String description) {
        if (c.isInstance(obj)) {
            @SuppressWarnings("unchecked")
            final T t = (T)obj;
            return Maybe.success(t);
        }
        return Maybe.fail(new ActionStatus(errorCode, description));
    }
    
    public static <T> Maybe<T> force(Future<T> future) {
        try {
            T result = future.get();
            return Maybe.success(result);
        } catch (Exception e) {
            return Maybe.fail(ActionStatus.innerError(e.getMessage()));
        }
    }

    public static <T> Maybe<T> fromCondition(boolean condition, T v, short errorCode, String description){
        if(condition){
            return success(v);
        }
        else{
            return fail(errorCode, description);
        }
    }
    
    public abstract <B> Maybe<B> castLeft();
    
    final static class Success<T> extends Maybe<T> {
        private T value;
        Success(T v) {
            if (v == null) {
                throw new NullPointerException("Success value is null");
            }
            this.value = v;
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public ActionStatus getLeft() {
            throw new IllegalStateException("Not fail, no left value");
        }

        @Override
        public T getRight() {
            return value;
        }

        @Override
        public <B> Maybe<B> castLeft() {
            throw new IllegalStateException("Not fail, no left value");
        }
    }

    final static class Failure<T> extends  Maybe<T> {
        private ActionStatus status;
        Failure(ActionStatus status) {
            if (status == null) {
                throw new NullPointerException("status is null");
            }
            this.status = status;
        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public ActionStatus getLeft() {
            return this.status;
        }

        @Override
        public T getRight() {
            throw new IllegalStateException("Not success, no right value");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <B> Maybe<B> castLeft() {
            return (Maybe<B>)this;
        }
        
        @Override
        public String toString(){
            return "Fail " + this.status.toString();
        }
    }
}
