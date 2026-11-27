package eu.openaire.management;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.Assert;

import jakarta.servlet.DispatcherType;

import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * Registers the application's session filter on the management server, so that a signed-in session is recognised by
 * the actuator endpoints served on the management port.
 * Lives outside the component-scanned packages and is loaded through the management context imports only: a
 * management context configuration must not be picked up by the application context.
 */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnBean(value = SessionRepositoryFilter.class, search = SearchStrategy.ANCESTORS)
class ManagementSessionConfiguration {

    @Bean
    FilterRegistrationBean<SessionRepositoryFilter<?>> managementSessionRepositoryFilter(
            HierarchicalBeanFactory beanFactory) {
        BeanFactory parent = beanFactory.getParentBeanFactory();
        Assert.state(parent != null, "'parent' must not be null");
        FilterRegistrationBean<SessionRepositoryFilter<?>> registration =
                new FilterRegistrationBean<>(parent.getBean(SessionRepositoryFilter.class));
        SessionProperties.Servlet servlet = parent.getBean(SessionProperties.class).getServlet();
        registration.setOrder(servlet.getFilterOrder());
        if (servlet.getFilterDispatcherTypes() != null) {
            registration.setDispatcherTypes(servlet.getFilterDispatcherTypes().stream()
                    .map(type -> DispatcherType.valueOf(type.name()))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(DispatcherType.class))));
        }
        return registration;
    }
}
