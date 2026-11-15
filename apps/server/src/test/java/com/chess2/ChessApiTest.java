package com.chess2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.chess2.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties={"app.admin.username=test_admin","app.admin.email=admin@chess2.test","app.admin.password=TestAdminPassword123!"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChessApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    @Test
    void registrationUsesJwtAndProtectsProfile() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        String token = register();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.gold").value(500));
        mvc.perform(get("/api/cards/inventory").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.deckSlot == 1)]").exists())
                .andExpect(jsonPath("$[?(@.deckSlot == 2)]").exists())
                .andExpect(jsonPath("$[?(@.deckSlot == 3)]").exists());
    }

    @Test
    void configuredAdminCanOpenDashboardAndManageWallets() throws Exception {
        MvcResult login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"test_admin\",\"password\":\"TestAdminPassword123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.player.role").value("ADMIN")).andReturn();
        JsonNode json=mapper.readTree(login.getResponse().getContentAsString());String token=json.get("accessToken").asText();String id=json.at("/player/id").asText();
        mvc.perform(get("/api/admin/overview").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.cards").value(15));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/users/{id}/wallet",id).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"gold\":900,\"gems\":12}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gold").value(900)).andExpect(jsonPath("$.gems").value(12));
    }

    @Test
    void frontendAssetsArePublicButGameplayRemainsProtected() throws Exception {
        mvc.perform(get("/assets/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/assets/css/app.css")).andExpect(status().isOk());
        mvc.perform(get("/admin/")).andExpect(status().isOk());
        mvc.perform(get("/admin/admin.js")).andExpect(status().isOk());
        mvc.perform(get("/pixel-chess-v1/pieces/W_King.png")).andExpect(status().isOk());
        mvc.perform(get("/api/games/history")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cards")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/shop")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/leaderboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/overview").header("Authorization", bearer(register()))).andExpect(status().isForbidden());
    }

    @Test
    void securityHeadersArePresentAndNoPublicWebSocketBackdoorExists() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",containsString("default-src 'self'")))
                .andExpect(header().string("X-Frame-Options","DENY"))
                .andExpect(header().string("Referrer-Policy","same-origin"));
        mvc.perform(get("/ws/games")).andExpect(status().isUnauthorized());
        mvc.perform(get("/ws/games").header("Upgrade","websocket").header("Connection","Upgrade").header("Sec-WebSocket-Protocol","chess2, jwt.invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordsAreBcryptHashedAndNeverStoredAsPlaintext() throws Exception {
        String username="hash"+UUID.randomUUID().toString().replace("-","").substring(0,8),password="PlainSecret123!";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Registration(username,username+"@example.com",password))))
                .andExpect(status().isOk());
        String stored=users.findByUsernameIgnoreCase(username).orElseThrow().getPassword();
        Assertions.assertNotEquals(password,stored);
        Assertions.assertTrue(stored.startsWith("$2"));
        Assertions.assertTrue(passwordEncoder.matches(password,stored));
    }

    @Test
    void trustBoundaryDtosRejectMalformedInputBeforeGameLogic() throws Exception {
        String token=register();
        mvc.perform(post("/api/puzzles/{id}/submit",UUID.randomUUID()).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"from\":\"z9\",\"to\":\"e8\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/cards/deck").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"cardIds\":[null]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/games/{id}/cards",UUID.randomUUID()).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"target\":\"e7\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"password\":\"xxxxxxxx\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failedPurchaseRollsBackWalletAndInventoryTogether() throws Exception {
        String token=register();
        JsonNode shop=mapper.readTree(mvc.perform(get("/api/shop").header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode item=shop.get(0);
        for(JsonNode candidate:shop)if(candidate.get("price").asLong()>item.get("price").asLong())item=candidate;
        String itemId=item.get("id").asText(),cardId=item.get("cardId").asText();
        mvc.perform(post("/api/shop/{id}/purchase",itemId).header("Authorization",bearer(token))).andExpect(status().isNoContent());
        mvc.perform(post("/api/shop/{id}/purchase",itemId).header("Authorization",bearer(token))).andExpect(status().isConflict());
        mvc.perform(get("/api/auth/me").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.gold").value(500-item.get("price").asLong()));
        JsonNode inventory=mapper.readTree(mvc.perform(get("/api/cards/inventory").header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        int quantity=0;for(JsonNode owned:inventory)if(cardId.equals(owned.get("cardId").asText()))quantity=owned.get("quantity").asInt();
        Assertions.assertEquals(1,quantity);
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
    void pveCanStartAsBlackAndBotMakesTheOpeningMove() throws Exception {
        String token=register();
        mvc.perform(post("/api/games").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PVE\",\"side\":\"BLACK\",\"difficulty\":\"EASY\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.playerSide").value("BLACK"))
                .andExpect(jsonPath("$.currentTurn").value("BLACK"));
    }

    @Test
    void capturingAPieceReducesArcadeHealth() throws Exception {
        String token=register();String gameId=createGame(token,"LOCAL");
        move(token,gameId,"e2","e4");move(token,gameId,"d7","d5");
        mvc.perform(post("/api/games/{id}/moves",gameId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"from\":\"e4\",\"to\":\"d5\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.blackHp").value(95)).andExpect(jsonPath("$.whiteHp").value(100));
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
        String token=mapper.readTree(second.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/cards/inventory").header("Authorization",bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
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
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING")).andExpect(jsonPath("$.whiteUsername").isNotEmpty()).andExpect(jsonPath("$.whiteRating").value(1000)).andExpect(jsonPath("$.roomExpiresAt").isNotEmpty()).andReturn();
        JsonNode waitingJson=mapper.readTree(waiting.getResponse().getContentAsString());String gameId=waitingJson.get("id").asText(),roomCode=waitingJson.get("roomCode").asText();Assertions.assertTrue(roomCode.matches("\\d{6}"));
        mvc.perform(post("/api/games/quick-match").header("Authorization",bearer(blackToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(gameId)).andExpect(jsonPath("$.status").value("PLAYING")).andExpect(jsonPath("$.blackUsername").isNotEmpty()).andExpect(jsonPath("$.blackRating").value(1000));
        mvc.perform(post("/api/games/{id}/resign",gameId).header("Authorization",bearer(blackToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESIGNED")).andExpect(jsonPath("$.winnerColor").value("WHITE"));
        mvc.perform(get("/api/leaderboard").header("Authorization",bearer(whiteToken))).andExpect(status().isOk()).andExpect(jsonPath("$[0].rating").value(1024));
    }

    @Test
    void numberedRoomCanBeJoinedAndExpiresAfterFiveMinutes() throws Exception {
        String ownerToken=register(),friendToken=register();
        MvcResult created=mvc.perform(post("/api/games").header("Authorization",bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PVP\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING")).andReturn();
        JsonNode room=mapper.readTree(created.getResponse().getContentAsString());String id=room.get("id").asText(),code=room.get("roomCode").asText();
        mvc.perform(post("/api/games/rooms/{code}/join",code).header("Authorization",bearer(friendToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.status").value("PLAYING")).andExpect(jsonPath("$.blackUsername").isNotEmpty());

        MvcResult stale=mvc.perform(post("/api/games").header("Authorization",bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PVP\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode staleRoom=mapper.readTree(stale.getResponse().getContentAsString());String staleId=staleRoom.get("id").asText(),staleCode=staleRoom.get("roomCode").asText();
        jdbc.update("update games set created_at=dateadd('MINUTE',-6,current_timestamp) where id=?",UUID.fromString(staleId));
        mvc.perform(get("/api/games/{id}",staleId).header("Authorization",bearer(ownerToken))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABANDONED"));
        mvc.perform(post("/api/games/rooms/{code}/join",staleCode).header("Authorization",bearer(friendToken))).andExpect(status().isConflict());
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
        String gameId = createGame(token, "PVE");
        mvc.perform(post("/api/games/{id}/cards", gameId).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"cardId\":\"" + cardId + "\",\"target\":\"e7\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frozenSquare").value("e7"));
    }

    @Test
    void healAtFullHpIsRejectedWithoutConsumingTheCard() throws Exception {
        String token=register();
        JsonNode cards=mapper.readTree(mvc.perform(get("/api/cards").header("Authorization",bearer(token))).andReturn().getResponse().getContentAsString());
        String healId=null;
        for(JsonNode card:cards)if("HEAL".equals(card.get("effectType").asText()))healId=card.get("id").asText();
        String gameId=createGame(token,"LOCAL");
        mvc.perform(post("/api/games/{id}/cards",gameId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"cardId\":\""+healId+"\",\"target\":\"OPPONENT\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("HP đã đầy. Thẻ Hồi phục chưa được sử dụng."));
        JsonNode inventory=mapper.readTree(mvc.perform(get("/api/cards/inventory").header("Authorization",bearer(token))).andReturn().getResponse().getContentAsString());
        for(JsonNode item:inventory)if(healId.equals(item.get("cardId").asText()))org.junit.jupiter.api.Assertions.assertEquals(1,item.get("quantity").asInt());
        mvc.perform(get("/api/games/{id}/logs",gameId).header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deckIsSnapshottedBeforeMatchAndMoveLogPersistsEvents() throws Exception {
        String token=register();
        JsonNode cards=mapper.readTree(mvc.perform(get("/api/cards").header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String healId=null,freezeId=null;
        for(JsonNode card:cards){if("HEAL".equals(card.get("effectType").asText()))healId=card.get("id").asText();if("FREEZE".equals(card.get("effectType").asText()))freezeId=card.get("id").asText();}
        org.junit.jupiter.api.Assertions.assertNotNull(healId);org.junit.jupiter.api.Assertions.assertNotNull(freezeId);
        mvc.perform(put("/api/cards/deck").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"cardIds\":[\""+healId+"\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.deckSlot == 1)]").exists());
        String gameId=createGame(token,"LOCAL");
        mvc.perform(put("/api/cards/deck").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"cardIds\":[\""+freezeId+"\"]}"))
                .andExpect(status().isOk());
        move(token,gameId,"e2","e4");move(token,gameId,"d7","d5");move(token,gameId,"e4","d5");
        mvc.perform(post("/api/games/{id}/cards",gameId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"cardId\":\""+healId+"\",\"target\":\"OPPONENT\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.blackHp").value(100));
        mvc.perform(post("/api/games/{id}/cards",gameId).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"cardId\":\""+freezeId+"\",\"target\":\"e7\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/games/{id}/logs",gameId).header("Authorization",bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[3].eventType").value("CARD")).andExpect(jsonPath("$[3].effectCode").value("HEAL"))
                .andExpect(jsonPath("$[3].occurredAt").isNotEmpty()).andExpect(jsonPath("$[2].capturedPiece").value("bP"));
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
    private void move(String token,String id,String from,String to) throws Exception {mvc.perform(post("/api/games/{id}/moves",id).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"from\":\""+from+"\",\"to\":\""+to+"\"}")).andExpect(status().isOk());}
    private static String bearer(String token) { return "Bearer "+token; }
    private record Registration(String username,String email,String password) { }
    private record Guest(String guestKey) { }
}
