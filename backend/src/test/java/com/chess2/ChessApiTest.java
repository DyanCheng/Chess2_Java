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
    void frontendAssetsArePublicButGameplayRemainsProtected() throws Exception {
        mvc.perform(get("/assets/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/assets/css/app.css")).andExpect(status().isOk());
        mvc.perform(get("/pixel-chess-v1/pieces/W_King.png")).andExpect(status().isOk());
        mvc.perform(get("/api/games/history")).andExpect(status().isUnauthorized());
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
    void guestKeyRestoresTheSameAccountInsteadOfCreatingAnotherOne() throws Exception {
        String guestKey = UUID.randomUUID().toString();
        String request = mapper.writeValueAsString(new Guest(guestKey));
        MvcResult first = mvc.perform(post("/api/auth/guest").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.player.accountType").value("GUEST")).andReturn();
        MvcResult second = mvc.perform(post("/api/auth/guest").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andReturn();
        String firstUser = mapper.readTree(first.getResponse().getContentAsString()).at("/player/username").asText();
        String secondUser = mapper.readTree(second.getResponse().getContentAsString()).at("/player/username").asText();
        org.junit.jupiter.api.Assertions.assertEquals(firstUser, secondUser);
    }

    @Test
    void passwordResetRequestNeedsMailConfigurationInsteadOfPretendingToSend() throws Exception {
        mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isServiceUnavailable());
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

    @Test
    void puzzleRewardCanOnlyBeClaimedOnce() throws Exception {
        String token=register();
        MvcResult puzzle=mvc.perform(get("/api/puzzles").header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn();
        String puzzleId=mapper.readTree(puzzle.getResponse().getContentAsString()).get(0).get("id").asText();
        String submission="{\"from\":\"e2\",\"to\":\"e8\"}";
        mvc.perform(post("/api/puzzles/{id}/submit",puzzleId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(true)).andExpect(jsonPath("$.rewardGranted").value(true));
        mvc.perform(post("/api/puzzles/{id}/submit",puzzleId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(true)).andExpect(jsonPath("$.rewardGranted").value(false));
        mvc.perform(get("/api/auth/me").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.gold").value(575));
    }

    @Test
    void freezeCardLocksTheChosenOpponentPiece() throws Exception {
        String token = register();
        JsonNode cards = mapper.readTree(mvc.perform(get("/api/cards").header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String cardId = null;
        for (JsonNode card : cards) if ("FREEZE".equals(card.get("effectType").asText())) cardId = card.get("id").asText();
        org.junit.jupiter.api.Assertions.assertNotNull(cardId);
        JsonNode shop = mapper.readTree(mvc.perform(get("/api/shop")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String itemId = null;
        for (JsonNode item : shop) if (cardId.equals(item.get("cardId").asText())) itemId = item.get("id").asText();
        org.junit.jupiter.api.Assertions.assertNotNull(itemId);
        mvc.perform(post("/api/shop/{id}/purchase", itemId).header("Authorization", bearer(token))).andExpect(status().isNoContent());
        String gameId = createGame(token, "PVE");
        mvc.perform(post("/api/games/{id}/cards", gameId).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"cardId\":\"" + cardId + "\",\"target\":\"e7\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frozenSquare").value("e7"));
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
    private record Guest(String guestKey) { }
}
