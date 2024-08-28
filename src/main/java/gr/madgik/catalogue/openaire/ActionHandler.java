package gr.madgik.catalogue.openaire;

public interface ActionHandler<T, ID> {

    default T preHandle(T t, Context ctx) {
        return t;
    }

    default void postHandle(T t, Context ctx) {
    }

    default void handleError(T t, Throwable throwable, Context ctx) {
    }
}
