/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire.invitations;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EntityScan(basePackageClasses = Invitation.class)
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "invitationEntityManagerFactory",
        transactionManagerRef = "invitationTransactionManager",
        basePackageClasses = {InvitationRepository.class})
public class InvitationConfig {

    @Autowired
    Environment environment;

    @Bean("invitationDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.invitation")
    public DataSource invitationDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("invitationEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean invitationEntityManagerFactory(EntityManagerFactoryBuilder builder) {

        Map<String, String> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.dialect", environment.getRequiredProperty("spring.jpa.invitation.hibernate.dialect"));
        jpaProperties.put("hibernate.hbm2ddl.auto", environment.getRequiredProperty("spring.jpa.invitation.hibernate.hbm2ddl.auto"));

        return builder
                .dataSource(invitationDataSource())
                .packages(Invitation.class)
                .properties(jpaProperties)
                .build();
    }

    @Bean(name = "invitationTransactionManager")
    public PlatformTransactionManager invitationTransactionManager(
            @Qualifier("invitationEntityManagerFactory") EntityManagerFactory invitationEntityManagerFactory) {
        return new JpaTransactionManager(invitationEntityManagerFactory);
    }

}
