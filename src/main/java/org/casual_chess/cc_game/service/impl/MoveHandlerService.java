package org.casual_chess.cc_game.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.casual_chess.cc_game.dto.InvalidMoveEvent;
import org.casual_chess.cc_game.dto.MoveEvent;
import org.casual_chess.cc_game.entity.GameStatus;
import org.casual_chess.cc_game.entity.MoveEntity;
import org.casual_chess.cc_game.entity.PlayerColor;
import org.casual_chess.cc_game.model.GameWithMoves;
import org.casual_chess.cc_game.pubsub.IPubSubPublisher;
import org.casual_chess.cc_game.pubsub.IPubSubSubscriber;
import org.casual_chess.cc_game.repository.IGameCacheRepository;
import org.casual_chess.cc_game.service.IChessLogicService;
import org.casual_chess.cc_game.util.IJsonSerializerDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MoveHandlerService {
    private final IPubSubPublisher publisher;
    private final IPubSubSubscriber subscriber;

    @Autowired
    IGameCacheRepository inMemoryGameCacheRepository;

    @Autowired
    IJsonSerializerDeserializer jsonSerializerDeserializer;

    @Autowired
    IChessLogicService chessLogicService;

    public MoveHandlerService(IPubSubPublisher publisher, IPubSubSubscriber subscriber) {
        this.publisher = publisher;
        this.subscriber = subscriber;
    }

    @PostConstruct
    public void subscribeToMoveEvents() {
        subscriber.subscribe("game/*/move/made", this::handleUserMoveEvent);
    }

    public void handleUserMoveEvent(String topic, String message) {
        log.info("Received topic: {}, move event: {}", topic, message);
        log.debug("Current Thread: {}", Thread.currentThread());

        MoveEvent moveEvent = jsonSerializerDeserializer.deserialize(message, MoveEvent.class);
        if (moveEvent == null) {
            log.error("Error parsing move event: message: {}", message);
            return;
        }

        //* get game state from cache (or db)
        GameWithMoves game = inMemoryGameCacheRepository.get(moveEvent.getGameId());
        if (game == null) {
            //* if game does not exist
            log.error("Game not found: {}", moveEvent.getGameId());
            return;
        }

        GameWithMoves newGameState = playMoveWithValidation(game, moveEvent);
        if (newGameState == null) {
            log.info("Invalid move: {}, gameState: {}", moveEvent, game);
            publisher.publish("game/{gameId}/move/invalid", jsonSerializerDeserializer.serialize(new InvalidMoveEvent(moveEvent, game)));
            return;
        }

        //* update game state in cache and db and publish the updated game state
        //*TODO: update game state

        String gameStateUpdateTopic = "game/" + moveEvent.getGameId() + "/state/updated";
        publisher.publish(gameStateUpdateTopic, jsonSerializerDeserializer.serialize(newGameState));


    }

    private GameWithMoves playMoveWithValidation(GameWithMoves currentGameState, MoveEvent moveEvent) {
        //* if current to move is wrong or, move number is wrong
        if (moveEvent.getPlayerColor().equals(PlayerColor.white) && currentGameState.getPlayerToMove() != GameStatus.white_to_move
            || moveEvent.getPlayerColor().equals(PlayerColor.black) && currentGameState.getPlayerToMove() != GameStatus.black_to_move
            || moveEvent.getMoveNo() != currentGameState.getMovesPlayed().size()
        ) {
            return null;
        }

        //* check if move is legal using chess logic service
        //* if yes, then update the game state and return new game state [don't mutate the current game state]
        //* otherwise return null
        return chessLogicService.updateGameState(currentGameState, convertToMove(moveEvent));
    }

    private MoveEntity convertToMove(MoveEvent moveEvent) {
        return MoveEntity.builder()
            .moveNotation(moveEvent.getMoveAlgebraic())
            .player(moveEvent.getPlayerColor())
            .build();
    }
}
