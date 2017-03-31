package net.eric.tpc.base;

public interface Either<A, B> {

    public static <L, R> Either<L, R> right(R value) {
        if(value == null){
            throw new NullPointerException("Either.right should not be null");
        }
        return new Right<L, R>(value);
    }

    public static <L, R> Either<L, R> left(L value) {
        if(value == null){
            throw new NullPointerException("Either.left should not be null");
        }

        return new Left<L, R>(value);
    }

    public static <L, R> Either<L, R> fromNullable(L leftValue, R rightValue) {
        if (rightValue == null) {
            return Either.left(leftValue);
        } else {
            return Either.right(rightValue);
        }
    }


    boolean isRight();

    A getLeft();

    B getRight();

    static class Left<L, R> implements Either<L, R> {
        private L value;

        public Left(L v) {
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

    static class Right<L, R> implements Either<L, R> {
        private R value;

        public Right(R v) {
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
