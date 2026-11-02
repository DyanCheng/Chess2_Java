package com.chess2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ChessApiTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void servesChessInterfaceAtRoot() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void registersPlayerCreatesBotGameAndMovesBothSides() throws Exception {
        String token = registerAndGetToken();
        String matchId = createMatch(token, "BOT");

        mvc.perform(post("/api/matches/{id}/moves", matchId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"e2","to":"e4"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board[4][4]").value("wP"))
                .andExpect(jsonPath("$.turn").value("w"))
                .andExpect(jsonPath("$.moves.length()").value(2));
    }

    @Test
    void localGameEnforcesTurnsAndResignationUpdatesLeaderboard() throws Exception {
        String token = registerAndGetToken();
        String matchId = createMatch(token, "LOCAL");
        move(token, matchId, "e2", "e4").andExpect(jsonPath("$.turn").value("b"));
        move(token, matchId, "e7", "e5").andExpect(jsonPath("$.turn").value("w"));

        mvc.perform(post("/api/matches/{id}/moves", matchId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"e7","to":"e6"}
                                """))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/matches/{id}/resign", matchId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.winner").value("b"));

        mvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").isNotEmpty())
                .andExpect(jsonPath("$[?(@.losses == 1)]").isNotEmpty());
    }

    @Test
    void rejectsIllegalMovesAndUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());

        String token = registerAndGetToken();
        String matchId = createMatch(token, "LOCAL");
        mvc.perform(post("/api/matches/{id}/moves", matchId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"e2","to":"e5"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String registerAndGetToken() throws Exception {
        String username = "player" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new Credentials(username, "samplePassword123"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = mapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    private String createMatch(String token, String mode) throws Exception {
        MvcResult result = mvc.perform(post("/api/matches")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new NewMatch(mode))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions move(
            String token, String matchId, String from, String to) throws Exception {
        return mvc.perform(post("/api/matches/{id}/moves", matchId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Move(from, to))))
                .andExpect(status().isOk());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Credentials(String username, String password) {}

    private record NewMatch(String mode) {}

    private record Move(String from, String to) {}
}
