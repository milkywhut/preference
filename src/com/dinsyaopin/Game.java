package com.dinsyaopin;

import com.dinsyaopin.Convention.Convention;
import com.dinsyaopin.Log.LogData;
import com.dinsyaopin.PlayerStrategy.TradingStrategy.PlayerTradingStrategy;
import com.dinsyaopin.PlayerStrategy.TurnsStrategy.PlayerTurnsStrategy;
import com.dinsyaopin.contracts.Contract;
import com.dinsyaopin.Log.LogDataInitial;
import com.dinsyaopin.contracts.ContractWithSuit;
import com.dinsyaopin.contracts.Misere;
import com.dinsyaopin.contracts.Pass;

import java.io.IOException;
import java.util.ArrayList;

import static com.dinsyaopin.BotsQuery.countBotsIndexes;
import static com.dinsyaopin.Configuration.*;

public class Game {
    private GameBot bot1;
    private GameBot bot2;
    private GameBot bot3;
    private ArrayList<LogData> logData;
    private LogDataInitial logDataInitial = new LogDataInitial();

    public void startGame() throws IOException {
        logData = new ArrayList<>();
        logData.add(logDataInitial);

        PlayerTradingStrategy playerTradingStrategy = getPlayerTradingStrategy();
        PlayerTurnsStrategy playerTurnsStrategy = getPlayerTurnsStrategy();
        Convention convention = getCurrentConvention();
        startGame(playerTradingStrategy, playerTurnsStrategy, convention);
    }

    private void startGame(PlayerTradingStrategy playerTradingStrategy, PlayerTurnsStrategy playerTurnsStrategy, Convention convention) throws IOException {
        int pool = getPool();
        logDataInitial.setPool(pool);
        bot1 = new GameBot("Player1");
        bot2 = new GameBot("Player2");
        bot3 = new GameBot("Player3");
        logDataInitial.setGameBot1Name(bot1.getBotName());
        logDataInitial.setGameBot2Name(bot2.getBotName());
        logDataInitial.setGameBot3Name(bot3.getBotName());
        bot1.setPlayerTurnsStrategy(playerTurnsStrategy);
        bot2.setPlayerTurnsStrategy(playerTurnsStrategy);
        bot3.setPlayerTurnsStrategy(playerTurnsStrategy);
        ArrayList<GameBot> gameBots = new ArrayList<>();
        gameBots.add(bot1);
        gameBots.add(bot2);
        gameBots.add(bot3);
        Dealer dealer = new Dealer();

        int currentBot = -1;

        while (bot1.getPool() != pool || bot2.getPool() != pool || bot3.getPool() != pool) {

            dealer.initializeDeck();
            dealer.giveCardsToPlayer(bot1);
            dealer.giveCardsToPlayer(bot2);
            dealer.giveCardsToPlayer(bot3);
            ArrayList<GameBot> bots = new ArrayList<>();

            int[] botsIndexes = countBotsIndexes(currentBot + 1);

            for (int i = 0; i <= 2; i++) {
                bots.add(gameBots.get(botsIndexes[i]));
            }

            Contract winnerContract = playerTradingStrategy.toTrade(bots);
            GameBot currentWinner = null;
            int countOfTurns = 10;
            int indexOfCurrentWinner = 0;

            for (int i = 0; i < countOfTurns; i++) {//turns
                //initialize table every turn
                Table table = new Table();
                //players put cards
                //table chooses trick winner
                //convention counts points

                if (currentWinner == null) {
                    bots.get(0).putCard(table, winnerContract, null);
                    Suits turnSuit = table.getFirstCard().suit;
                    bots.get(1).putCard(table, winnerContract, turnSuit);
                    bots.get(2).putCard(table, winnerContract, turnSuit);
                    currentWinner = table.showTurnWinner(bots, turnSuit, winnerContract);//should check method/logic has done
                    currentWinner.addTrick();
                    indexOfCurrentWinner = bots.indexOf(currentWinner);
                }
                else {//sorting array with currentWinner as 0 element. needed 100%
                    botsIndexes = countBotsIndexes(indexOfCurrentWinner);
                    for (int j = 0; i < 2; i++) {
                        bots.add(gameBots.get(botsIndexes[j]));
                    }
                }
            }
            convention.countPoints(bots, currentWinner, winnerContract);

            //shitcode moves bots in array for next turn
            currentBot++;
            if (currentBot == 1) {
                currentBot = -1;
            }
        }
    }
}