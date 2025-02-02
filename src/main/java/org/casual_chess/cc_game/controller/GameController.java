package org.casual_chess.cc_game.controller;

import org.casual_chess.cc_game.constants.ApiResponseConstants;
import org.casual_chess.cc_game.dto.ApiResponse;
import org.casual_chess.cc_game.dto.NewGameRequest;
import org.casual_chess.cc_game.model.Game;
import org.casual_chess.cc_game.service.impl.GameManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
public class GameController {

    @Autowired
    GameManagerService gameManagerService;

    //* Create a New Game for Given 2 Users
    @PostMapping
    public ResponseEntity<ApiResponse<Game>> createGame(@RequestBody NewGameRequest gameRequest) {
        Game game = gameManagerService.createGame(gameRequest);

        ApiResponse<Game> apiResponse = new ApiResponse<>(ApiResponseConstants.SUCCESS, game);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

//    // 2. Get All Active or Running Games for a Given User
//    @GetMapping("/active/{userId}")
//    public ResponseEntity<List<GameResponse>> getActiveGames(@PathVariable String userId) {
//        // Call service to get all active games for the user
//        List<GameResponse> activeGames = gameService.getActiveGamesForUser(userId);
//        return new ResponseEntity<>(activeGames, HttpStatus.OK);  // return 200 OK status
//    }
//
//    // 3. Get All Past Games for a Given User
//    @GetMapping("/past/{userId}")
//    public ResponseEntity<List<GameResponse>> getPastGames(@PathVariable String userId) {
//        // Call service to get all past games for the user
//        List<GameResponse> pastGames = gameService.getPastGamesForUser(userId);
//        return new ResponseEntity<>(pastGames, HttpStatus.OK);  // return 200 OK status
//    }
}
