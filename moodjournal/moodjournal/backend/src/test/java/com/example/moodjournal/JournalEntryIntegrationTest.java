package com.example.moodjournal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.Mood;
import com.example.moodjournal.model.User;
import com.example.moodjournal.model.Visibility;
import com.example.moodjournal.repository.JournalEntryRepository;
import com.example.moodjournal.repository.UserRepository;

@SpringBootTest(properties = {
                "app.encryption.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
})
@AutoConfigureMockMvc
public class JournalEntryIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private JournalEntryRepository journalEntryRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private User victim;
        private User attacker;
        private JournalEntry victimEntry;

        @BeforeEach
        public void setup() {
                journalEntryRepository.deleteAll();
                userRepository.deleteAll();

                
                victim = new User();
                victim.setUsername("victim");
                victim.setEmail("victim@example.com");
                victim.setPassword(passwordEncoder.encode("password"));
                victim.setAge(25);
                victim = userRepository.save(victim);

                
                attacker = new User();
                attacker.setUsername("attacker");
                attacker.setEmail("attacker@example.com");
                attacker.setPassword(passwordEncoder.encode("password"));
                attacker.setAge(30);
                attacker = userRepository.save(attacker);

                
                victimEntry = new JournalEntry();
                victimEntry.setTitle("Secret Diary");
                victimEntry.setContent("This is private content.");
                victimEntry.setMood(Mood.SAD);
                victimEntry.setVisibility(Visibility.PRIVATE);
                victimEntry.setUser(victim);
                victimEntry = journalEntryRepository.save(victimEntry);
        }

        @Test
        @WithMockUser(username = "attacker@example.com")
        public void testAttackerCannotAccessVictimEntry() throws Exception {
                
                
                
                mockMvc.perform(get("/api/journal/" + victimEntry.getId()))
                                .andExpect(status().isNotFound()); 
        }
}
