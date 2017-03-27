package net.eric.tpc.common;

public abstract class Either<A, B> {

    public static <L, R> Either<L, R> right(R value) {
        return new Right<L, R>(value);
    }

    public static <L, R> Either<L, R> left(L value) {
        return new Left<L, R>(value);
    }

    public static <L, R> Either<L, R> fromNullable(L leftValue, R rightValue) {
        if (rightValue == null) {
            return Either.left(leftValue);
        } else {
            return Either.right(rightValue);
        }
    }

    private Either() {
    }

    public abstract boolean isRight();

    public abstract A getLeft();

    public abstract B getRight();

    public final static class Left<L, R> extends Either<L, R> {
        private L value;

        private Left(L v) {
            this.value = v;
        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public L getLeft() {
            return value;
        }

        @Override
        public R getRight() {
            throw new IllegalStateException("This is Left, no right value");
        }
    }

    public final static class Right<L, R> extends Either<L, R> {
        private R value;

        private Right(R v) {
            this.value = v;
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public L getLeft() {
            throw new IllegalStateException("This is Left, no right value");
        }

        @Override
        public R getRight() {
            return value;
        }
    }

}
