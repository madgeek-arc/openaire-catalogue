package eu.openaire.catalogue.vocabulary;

import eu.openaire.catalogue.api.ErrorHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VocabularyControllerTest {

    private static final VocabularyService vocabularies = mock(VocabularyService.class);
    private static final String FRANCE = "{\"id\":\"FR\",\"name\":\"France\",\"type\":\"Country\"}";

    private AnnotationConfigApplicationContext context;
    private MockMvc mvc;

    @Configuration
    @EnableMethodSecurity
    static class Controllers {

        @Bean
        VocabularyController vocabularyController() {
            return new VocabularyController(vocabularies);
        }
    }

    @BeforeEach
    void setUp() {
        reset(vocabularies);
        Map<String, Object> greece = Map.of("id", "GR", "name", "Greece", "type", "Country");
        when(vocabularies.get("GR")).thenReturn(greece);
        when(vocabularies.byTypeAndId("Country", "GR")).thenReturn(greece);
        when(vocabularies.allByType()).thenReturn(Map.of("Country", List.of(greece)));
        context = new AnnotationConfigApplicationContext(Controllers.class);
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(VocabularyController.class))
                .setControllerAdvice(new ErrorHandler())
                .build();
        signIn("ROLE_ANONYMOUS");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void bothPrefixesServeTheSameRoutes() throws Exception {
        for (String prefix : List.of("/vocabulary", "/vocabularies")) {
            mvc.perform(get(prefix + "/GR")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Greece"));
            mvc.perform(get(prefix + "/types/Country/GR")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("GR"));
        }
    }

    @Test
    void mappingsAreServedUnderBothPrefixes() throws Exception {
        for (String prefix : List.of("/vocabulary", "/vocabularies")) {
            mvc.perform(get(prefix + "/mappings")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.Country[0].id").value("GR"))
                    .andExpect(jsonPath("$.Country[0].name").value("Greece"));
        }
    }

    @Test
    void onlyAdminsWriteEntries() throws Exception {
        signIn("ROLE_USER");
        mvc.perform(post("/vocabularies").contentType(MediaType.APPLICATION_JSON).content(FRANCE))
                .andExpect(status().isForbidden());
        mvc.perform(put("/vocabularies/FR").contentType(MediaType.APPLICATION_JSON).content(FRANCE))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/vocabularies/FR")).andExpect(status().isForbidden());
        verify(vocabularies, never()).add(anyMap());

        signIn("ROLE_USER", "ROLE_ADMIN");
        mvc.perform(post("/vocabularies").contentType(MediaType.APPLICATION_JSON).content(FRANCE))
                .andExpect(status().isCreated());
        mvc.perform(post("/vocabularies/bulk").contentType(MediaType.APPLICATION_JSON).content("[" + FRANCE + "]"))
                .andExpect(status().isCreated());
        mvc.perform(post("/vocabularies/validate").contentType(MediaType.APPLICATION_JSON).content(FRANCE))
                .andExpect(status().isOk());
        mvc.perform(delete("/vocabularies/FR")).andExpect(status().isOk());
        verify(vocabularies).add(Map.of("id", "FR", "name", "France", "type", "Country"));
        verify(vocabularies).delete("FR");
    }

    @Test
    void aWriteRefreshesTheMappings() throws Exception {
        mvc.perform(get("/vocabularies/mappings")).andExpect(jsonPath("$.Country.length()").value(1));
        Map<String, Object> france = Map.of("id", "FR", "name", "France", "type", "Country");
        when(vocabularies.allByType()).thenReturn(Map.of("Country", List.of(
                Map.of("id", "GR", "name", "Greece", "type", "Country"), france)));

        signIn("ROLE_ADMIN");
        mvc.perform(post("/vocabularies").contentType(MediaType.APPLICATION_JSON).content(FRANCE));

        mvc.perform(get("/vocabularies/mappings")).andExpect(jsonPath("$.Country.length()").value(2));
    }

    private static void signIn(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(authorities[0].equals("ROLE_ANONYMOUS")
                ? new AnonymousAuthenticationToken("key", "anonymous", AuthorityUtils.createAuthorityList(authorities))
                : UsernamePasswordAuthenticationToken.authenticated("user", null,
                AuthorityUtils.createAuthorityList(authorities)));
    }
}
