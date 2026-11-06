package com.chess2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChessApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void registrationUsesJwtAndProtectsProfile() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        String token = register();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.gold").value(500));
    }

    @Test
    void pveGameValidatesMoveAndReturnsAfterBotTurn() throws Exception {
        String token=register(); String gameId=createGame(token,"PVE");
        mvc.perform(post("/api/games/{id}/moves",gameId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"from\":\"e2\",\"to\":\"e5\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/games/{id}/moves",gameId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"from\":\"e2\",\"to\":\"e4\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentTurn").value("WHITE"));
    }

    @Test
    void quickMatchPairsPlayersAndUpdatesRankedResultOnResign() throws Exception {
        String whiteToken=register(); String blackToken=register();
        MvcResult waiting=mvc.perform(post("/api/games/quick-match").header("Authorization",bearer(whiteToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING")).andReturn();
        String gameId=mapper.readTree(waiting.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/games/quick-match").header("Authorization",bearer(blackToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(gameId)).andExpect(jsonPath("$.status").value("PLAYING"));
        mvc.perform(post("/api/games/{id}/resign",gameId).header("Authorization",bearer(blackToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESIGNED")).andExpect(jsonPath("$.winnerColor").value("WHITE"));
        mvc.perform(get("/api/leaderboard")).andExpect(status().isOk()).andExpect(jsonPath("$[0].rating").value(1024));
    }

    private String register() throws Exception {
        String user="player"+UUID.randomUUID().toString().replace("-","").substring(0,8);
        MvcResult result=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new Registration(user,user+"@example.com","samplePassword123"))))
                .andExpect(status().isOk()).andReturn();
        JsonNode json=mapper.readTree(result.getResponse().getContentAsString()); return json.get("accessToken").asText();
    }
    private String createGame(String token,String type) throws Exception {
        MvcResult result=mvc.perform(post("/api/games").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\""+type+"\"}"))
                .andExpect(status().isOk()).andReturn(); return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
    private static String bearer(String token) { return "Bearer "+token; }
    private record Registration(String username,String email,String password) { }
}
