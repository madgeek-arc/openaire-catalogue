package gr.madgik.catalogue.openaire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class OpenAIRECatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenAIRECatalogueApplication.class, args);
    }

}
