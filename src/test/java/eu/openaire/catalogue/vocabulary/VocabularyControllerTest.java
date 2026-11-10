package eu.openaire.catalogue.vocabulary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VocabularyControllerTest {

    private final VocabularyService vocabularies = mock(VocabularyService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Map<String, Object> greece = Map.of("id", "GR", "name", "Greece", "type", "Country");
        when(vocabularies.get("GR")).thenReturn(greece);
        when(vocabularies.byTypeAndId("Country", "GR")).thenReturn(greece);
        when(vocabularies.allByType()).thenReturn(Map.of("Country", List.of(greece)));
        mvc = MockMvcBuilders.standaloneSetup(new VocabularyController(vocabularies)).build();
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
}
