package gr.madgik.catalogue.openaire.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import springfox.documentation.oas.web.OpenApiTransformationContext;
import springfox.documentation.oas.web.WebMvcOpenApiTransformationFilter;
import springfox.documentation.spi.DocumentationType;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
public class OpenApiConfig implements WebMvcOpenApiTransformationFilter {

    @Value("${swagger.host}")
    String host;

    @Override
    public OpenAPI transform(OpenApiTransformationContext<HttpServletRequest> context) {
        OpenAPI openAPI = context.getSpecification();
        if (host != null && !"".equals(host)) {
            Server server = new Server();
            server.setUrl(host);
            openAPI.setServers(List.of(server));
        }
        return openAPI;
    }

    @Override
    public boolean supports(DocumentationType docType) {
        return docType.equals(DocumentationType.OAS_30);
    }
}
