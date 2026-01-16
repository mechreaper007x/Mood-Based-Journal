
package com.example.moodjournal;

import com.example.moodjournal.dto.CreateJournalEntryRequest;
import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.Mood;
import com.example.moodjournal.model.User;
import com.example.moodjournal.model.Visibility;
import com.example.moodjournal.repository.UserRepository;
import com.example.moodjournal.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
                "google.api.key=dummy-key",
                "google.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash:generateContent",
                "jwt.secret=f26a7f89e5b95eafea207206072e4d81"
})
public class JournalEntryIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @MockitoBean
        private GeminiService geminiService;

        private User user;

        @BeforeEach
        void setUp() {
                user = new User();
                user.setUsername("integration_test_user");
                user.setEmail("integration_test_user@example.com");
                user.setPassword("password");
                userRepository.save(user);
        }

        @Test
        @WithMockUser(username = "integration_test_user@example.com")
        public void testFullCrudLifecycle() throws Exception {
                // Step 1: Create a new journal entry
                // Mock Gemini to return a valid emotion analysis
                when(geminiService.analyzeEmotions(anyString())).thenReturn(
                                "{\"Anger\":\"10%\",\"Happy\":\"70%\",\"Sadness\":\"20%\",\"DominantEmotion\":\"Happy\"}");
                CreateJournalEntryRequest newEntryRequest = new CreateJournalEntryRequest();
                newEntryRequest.setTitle("Integration Test Title");
                newEntryRequest.setContent("Integration Test Content");
                newEntryRequest.setVisibility(Visibility.PRIVATE.toString());

                MvcResult createResult = mockMvc.perform(post("/api/journal").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newEntryRequest)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String json = createResult.getResponse().getContentAsString();
                JournalEntry createdEntry = objectMapper.readValue(json, JournalEntry.class);
                createdEntry.setVisibility(Visibility.PRIVATE); // Explicitly set visibility

                // Step 2: Read the journal entry
                mockMvc.perform(get("/api/journal/" + createdEntry.getId()).with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Integration Test Title"));

                // Step 3: Update the journal entry
                createdEntry.setTitle("Updated Integration Test Title");
                mockMvc.perform(put("/api/journal/" + createdEntry.getId()).with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createdEntry)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Updated Integration Test Title"));

                // Step 4: Delete the journal entry
                mockMvc.perform(delete("/api/journal/" + createdEntry.getId()).with(csrf()))
                                .andExpect(status().isNoContent());

                // Step 5: Verify the entry is deleted
                mockMvc.perform(get("/api/journal/" + createdEntry.getId()).with(csrf()))
                                .andExpect(status().isNotFound());
        }
}
