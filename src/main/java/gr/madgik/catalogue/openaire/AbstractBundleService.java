//package gr.madgik.catalogue.openaire;
//
//import eu.einfracentral.domain.Bundle;
//import eu.einfracentral.domain.Identifiable;
//import gr.athenarc.catalogue.service.GenericItemService;
//import gr.madgik.catalogue.BundleResourceOperations;
//import gr.madgik.catalogue.repository.RegistryCoreRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//
//public abstract class AbstractBundleService<T extends Identifiable, B extends Bundle<T>, ID> extends RegistryCoreRepository<B> implements BundleResourceOperations<T, B, ID> {
//
//    private static final Logger logger = LoggerFactory.getLogger(AbstractBundleService.class);
//    protected final String collectionName;
//    private final Class<B> bundleType;
//
//    protected AbstractBundleService(Class<B> bundleType, GenericItemService itemService) {
//        super(itemService);
//        this.bundleType = bundleType;
//        this.collectionName = bundleType.getSimpleName().substring(0, 1).toLowerCase() + bundleType.getSimpleName().substring(1);
//    }
//
//    @Override
//    public boolean validate(Object resource) {
//        return false;
//    }
//
//    @Override
//    public T register(T resource) {
//        return null;
//    }
//
//    @Override
//    public T update(ID id, T resource) {
//        return null;
//    }
//
//    @Override
//    public B verify(ID id, String status, Boolean active) {
//        return null;
//    }
//
//    @Override
//    public B activate(ID id, Boolean active) {
//        return null;
//    }
//}
