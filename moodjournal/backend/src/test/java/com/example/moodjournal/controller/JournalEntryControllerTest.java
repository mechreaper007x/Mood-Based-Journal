
package com.example.moodjournal.controller;

import com.example.moodjournal.dto.CreateJournalEntryRequest;
import com.example.moodjournal.dto.UpdateJournalEntryRequest;
import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.User;
import com.example.moodjournal.service.JournalEntryService;
import com.example.moodjournal.service.UserService;
import com.example.moodjournal.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JournalEntryController.class)
public class JournalEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JournalEntryService journalEntryService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private JournalEntry journalEntry1;
    private JournalEntry journalEntry2;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");

        journalEntry1 = new JournalEntry();
        journalEntry1.setId(1L);
        journalEntry1.setTitle("Test Title 1");
        journalEntry1.setContent("Test Content 1");
        journalEntry1.setUser(user);

        journalEntry2 = new JournalEntry();
        journalEntry2.setId(2L);
        journalEntry2.setTitle("Test Title 2");
        journalEntry2.setContent("Test Content 2");
        journalEntry2.setUser(user);

        when(userService.findByEmail(anyString())).thenReturn(Optional.of(user));
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void createJournalEntry_shouldReturnCreated() throws Exception {
        CreateJournalEntryRequest request = new CreateJournalEntryRequest();
        request.setTitle("Test Title 1");
        request.setContent("Test Content 1");

        when(journalEntryService.create(anyLong(), any(JournalEntry.class))).thenReturn(journalEntry1);

        mockMvc.perform(post("/api/journal").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Title 1"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void getAllJournalEntries_shouldReturnOk() throws Exception {
        when(journalEntryService.getByUser(1L)).thenReturn(Arrays.asList(journalEntry1, journalEntry2));

        mockMvc.perform(get("/api/journal/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].title").value("Test Title 1"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void getJournalEntryById_whenEntryExists_shouldReturnOk() throws Exception {
        when(journalEntryService.getById(1L, 1L)).thenReturn(Optional.of(journalEntry1));

        mockMvc.perform(get("/api/journal/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Title 1"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void getJournalEntryById_whenEntryDoesNotExist_shouldReturnNotFound() throws Exception {
        when(journalEntryService.getById(1L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/journal/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void updateJournalEntry_whenEntryExists_shouldReturnOk() throws Exception {
        UpdateJournalEntryRequest request = new UpdateJournalEntryRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated Content");

        journalEntry1.setTitle("Updated Title");

        when(journalEntryService.update(anyLong(), anyLong(), any(UpdateJournalEntryRequest.class))).thenReturn(journalEntry1);

        mockMvc.perform(put("/api/journal/1").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void deleteJournalEntry_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/journal/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
