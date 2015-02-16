package com.github.ziohimself.qumi.impl.util;

import com.atlassian.fugue.Option;
import com.google.common.base.Function;

import javax.annotation.Nonnull;

public abstract class Try<V> {

    private Try() {
    }

    public abstract Boolean isSuccess();

    public abstract Boolean isFailure();

    public abstract <V2> Try<V2> map (@Nonnull Function<V, V2> mapFn);
    public abstract <V2> Try<V2> flatMap (@Nonnull Function<V, Try<V2>> mapFn);
    public abstract Try<V> recover(@Nonnull Function<Exception, V> mapFailureFn);
    public abstract Try<V> recoverWith(@Nonnull Function<Exception, Try<V>> mapFailureFn);
    public abstract Option<V> toOption();

    /** Completes this Try with an exception wrapped in a Success. The exception is either the exception that the Try failed with (if a Failure) or an UnsupportedOperationException. */
    public abstract Try<Exception> failed();
    /** Returns the value from this Success or throws the exception if this is a Failure. */
    public abstract V get();
    /** TODO: implement these */
//    def getOrElse[T](default: T): T
//    Returns the value from this Success or the given default argument if this is a Failure.
//    def orElse[T](default: Try[T]): Try[T]
//    Returns this Try if it's a Success or the given default argument if this is a Failure.

    public static <V> Try<V> failure(String message) {
        return new Failure<V>(message);
    }

    public static <V> Try<V> failure(String message, Exception e) {
        return new Failure<V>(message, e);
    }

    public static <V> Try<V> failure(Exception e) {
        return new Failure<V>(e);
    }

    public static <V> Try<V> success(V value) {
        return new Success<V>(value);
    }

    private static class Failure<V> extends Try<V> {

        private RuntimeException exception;

        public Failure(String message) {
            super();
            this.exception = new IllegalStateException(message);
        }

        public Failure(String message, Exception e) {
            super();
            this.exception = new IllegalStateException(message, e);
        }

        public Failure(Exception e) {
            super();
            this.exception = new IllegalStateException(e);
        }

        @Override
        public Boolean isSuccess() {
            return false;
        }

        @Override
        public Boolean isFailure() {
            return true;
        }

        @Override
        public <V2> Try<V2> map(@Nonnull Function<V, V2> mapFn) {
            return Try.failure(exception);
        }
        @Override
        public <V2> Try<V2> flatMap(@Nonnull Function<V, Try<V2>> mapFn) {
            return Try.failure(exception);
        }

        @Override
        public Try<V> recover(@Nonnull Function<Exception, V> mapFailureFn) {
            V recoveredValue;
            try {
                recoveredValue = mapFailureFn.apply(exception);
            } catch (Exception e) {
                return Try.failure(e);
            }
            return Try.success(recoveredValue);
        }

        @Override
        public Try<V> recoverWith(@Nonnull Function<Exception, Try<V>> mapFailureFn) {
            @SuppressWarnings("UnnecessaryLocalVariable")
            Try<V> tryOfV = mapFailureFn.apply(exception);
            return tryOfV;
        }

        @Override
        public Option<V> toOption() {
            return Option.none();
        }

        @Override
        public Try<Exception> failed() {
            return Try.<Exception>success(exception);
        }

        @Override
        public V get() {
            throw exception;
        }
    }

    private static class Success<V> extends Try<V> {

        private V value;

        public Success(V value) {
            super();
            this.value = value;
        }

        @Override
        public Boolean isSuccess() {
            return true;
        }

        @Override
        public Boolean isFailure() {
            return false;
        }

        @Override
        public <V2> Try<V2> map(@Nonnull Function<V, V2> mapFn) {
            V2 v2;
            try {
                v2 = mapFn.apply(value);
            } catch (Exception e) {
                return Try.failure(e);
            }
            return Try.success(v2);
        }
        @Override
        public <V2> Try<V2> flatMap(@Nonnull Function<V, Try<V2>> mapFn) {
            @SuppressWarnings("UnnecessaryLocalVariable")
            Try<V2> v2Try = mapFn.apply(value);
            return v2Try;
        }

        @Override
        public Try<V> recover(@Nonnull Function<Exception, V> mapFailureFn) {
            return Try.success(value);
        }

        @Override
        public Try<V> recoverWith(@Nonnull Function<Exception, Try<V>> mapFailureFn) {
            return Try.success(value);
        }

        @Override
        public Option<V> toOption() {
            return Option.some(value);
        }

        @Override
        public Try<Exception> failed() {
            return Try.failure(new UnsupportedOperationException("This try is instance of Try.Success. It has not failed."));
        }

        @Override
        public V get() {
            return value;
        }
    }
}