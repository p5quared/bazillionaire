package net.peterv.bazillionaire.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameUiAssetsTest {

    @Test
    void gameTemplateContainsOverlayRootsAndBootstrapData() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/GameController/game.html"));

        assertTrue(template.contains("id=\"game-data\" data-game-id=\"{gameId}\" data-player-id=\"{playerId}\""));
        assertTrue(template.contains("id=\"game-status-chips\""));
        assertTrue(template.contains("id=\"game-toast-stack\""));
        assertTrue(template.contains("id=\"game-controls\""));
    }

    @Test
    void clientScriptHandlesPowerupAndFreezeEvents() throws IOException {
        String script = Files.readString(Path.of("src/main/resources/META-INF/resources/js/game.js"));

        assertTrue(script.contains("POWERUP_AWARDED"));
        assertTrue(script.contains("FREEZE_STARTED"));
        assertTrue(script.contains("FREEZE_EXPIRED"));
        assertTrue(script.contains("GAME_FINISHED"));
    }
}
