package net.eric.tpc.base;

public interface Maybe<T> extends Either<ActionStatus, T> {
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

    public static <T> Maybe<T> fail(String code, String description) {
        if (code == null) {
            throw new NullPointerException("Maybe.status.code should not be null");
        }
        return new Failure<T>(ActionStatus.create(code, description));
    }

    public static <T> Maybe<T> fromNullable(ActionStatus status, T v) {
        if (v == null) {
            return Maybe.fail(status);
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

    
    public static <T> Maybe<T> safeCast(Object obj, Class<T> c, String errorCode, String description) {
        if (c.isInstance(obj)) {
            @SuppressWarnings("unchecked")
            final T t = (T)obj;
            return Maybe.success(t);
        }
        return Maybe.fail(new ActionStatus(errorCode, description));
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
