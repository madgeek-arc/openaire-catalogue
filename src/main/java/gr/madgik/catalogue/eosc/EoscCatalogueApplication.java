package gr.madgik.catalogue.eosc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class EoscCatalogueApplication {

	public static void main(String[] args) {
		SpringApplication.run(EoscCatalogueApplication.class, args);
	}

}
