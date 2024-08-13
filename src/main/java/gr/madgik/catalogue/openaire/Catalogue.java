package gr.madgik.catalogue.openaire;

import gr.madgik.catalogue.openaire.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class Catalogue<T, ID> {

    private static final Logger logger = LoggerFactory.getLogger(Catalogue.class);

    public enum Action {
        REGISTER,
        UPDATE,
        DELETE
    }

    private final Map<Action, ActionHandler<T, ID>> handlers = new HashMap<>();

    private final Repository<T, ID> repository;

    public Catalogue(Repository<T, ID> repository) {
        handlers.put(Action.REGISTER, new ActionHandler<>(){});
        handlers.put(Action.UPDATE, new ActionHandler<>(){});
        handlers.put(Action.DELETE, new ActionHandler<>(){});

        this.repository = repository;
    }

    public void registerHandler(Action action, ActionHandler<T, ID> handler) {
        this.handlers.put(action, handler);
    }

    public T register(T resource) {
        Context ctx = createContext();

        try {
            resource = handlers.get(Action.REGISTER).preHandle(resource, ctx);
        } catch (Exception e) {
            logger.info("Error prehandling shit", e);
            throw e;
        }

        try {
            // registration
            resource = repository.create(resource);
        } catch (Exception e) {
            logger.error("Error registering resource", e);
            handlers.get(Action.REGISTER).handleError(resource, e, ctx);
        }

        try {
            handlers.get(Action.REGISTER).postHandle(resource, ctx);
        } catch (Exception e) {
            logger.info("Error posthandling shit", e);
            throw e;
        }

        return resource;
    }

    public T update(ID id, T resource) {
        Context ctx = createContext();

        try {
            resource = handlers.get(Action.UPDATE).preHandle(resource, ctx);
        } catch (Exception e) {
            logger.info("Error prehandling shit", e);
            throw e;
        }

        try {
            // update
            resource = repository.update(id, resource);
        } catch (Exception e) {
            logger.error("Error updating resource", e);
            handlers.get(Action.UPDATE).handleError(resource, e, ctx);
        }

        try {
            handlers.get(Action.UPDATE).postHandle(resource, ctx);
        } catch (Exception e) {
            logger.info("Error posthandling shit", e);
            throw e;
        }

        return resource;
    }

    public void delete(ID id) {
        Context ctx = createContext();
        T resource = repository.get(id);

        try {
            resource = handlers.get(Action.DELETE).preHandle(resource, ctx);
        } catch (Exception e) {
            logger.info("Error prehandling shit", e);
            throw e;
        }

        try {
            repository.deleteById(id);
        } catch (Exception e) {
            logger.error("Error deleting resource", e);
            handlers.get(Action.DELETE).handleError(resource, e, ctx);
        }

        try {
            handlers.get(Action.DELETE).postHandle(resource, ctx);
        } catch (Exception e) {
            logger.info("Error posthandling shit", e);
            throw e;
        }
    }

    private Context createContext() {
        return new Context();
    }
}

