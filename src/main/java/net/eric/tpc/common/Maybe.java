package net.eric.tpc.common;

public interface Maybe<T> extends Either<ActionStatus, T> {
    public static <T> Maybe<T> success(T t) {
        if(t == null){
            throw new NullPointerException("Maybe.success should not be null");
        }
        return new Success<T>(t);
    }

    public static <T> Maybe<T> fail(ActionStatus status) {
        if(status == null){
            throw new NullPointerException("Maybe.status should not be null");
        }
        return new Failure<T>(status);
    }

    public static <T> Maybe<T> fromNullable(ActionStatus status, T v) {
        if (v == null) {
            return Maybe.fail(status);
        } else {
            return Maybe.success(v);
        }
    }
    
    final static class Success<T> extends Either.Right<ActionStatus, T> implements Maybe<T> {
        Success(T v) {
            super(v);
        }
    }

    final static class Failure<T> extends Either.Left<ActionStatus, T> implements Maybe<T> {
        Failure(ActionStatus status) {
            super(status);
        }
    }
}
