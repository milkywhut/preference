package com.dinsyaopin;

import com.dinsyaopin.Convention.Convention;
import com.dinsyaopin.Log.LogData;
import com.dinsyaopin.PlayerStrategy.TradingStrategy.PlayerTradingStrategy;
import com.dinsyaopin.PlayerStrategy.TurnsStrategy.PlayerTurnsStrategy;
import com.dinsyaopin.contracts.Contract;
import com.dinsyaopin.Log.LogDataInitial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static com.dinsyaopin.BotsQuery.countBotsIndexes;
import static com.dinsyaopin.Configuration.*;

public class Game {
    private GameBot bot1;
    private GameBot bot2;
    private GameBot bot3;
    private ArrayList<LogData> logData;
    private LogDataInitial logDataInitial = new LogDataInitial();
    private int[] botsIndexes;

    public void startGame() throws IOException {
        logData = new ArrayList<>();
        logData.add(logDataInitial);

        PlayerTradingStrategy playerTradingStrategy = getPlayerTradingStrategy();
        PlayerTurnsStrategy playerTurnsStrategy = getPlayerTurnsStrategy();
        Convention convention = getCurrentConvention();
        startGame(playerTradingStrategy, playerTurnsStrategy, convention);
    }

    private void startGame(PlayerTradingStrategy playerTradingStrategy, PlayerTurnsStrategy playerTurnsStrategy, Convention convention) throws IOException {
        int gamePool = getPool();

        logDataInitial.setPool(gamePool);
        initializeBots();
        setAliases();

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

        boolean gameEndingCondition = bot1.getPool() == gamePool || bot2.getPool() == gamePool || bot3.getPool() == gamePool;

        while (!gameEndingCondition) {

            dealer.initializeDeck();
            dealer.giveCardsToPlayers(gameBots);

            ArrayList<GameBot> bots = new ArrayList<>();

            botsIndexes = countBotsIndexes(currentBot + 1);

            for (int i = 0; i <= 2; i++) {
                bots.add(gameBots.get(botsIndexes[i]));
            }

            Contract winnerContract = playerTradingStrategy.toTrade(bots);
            GameBot winnerOfTrading = winnerContract.getWinner();
            if (winnerContract.toString().equals("Контракт с мастью") || winnerContract.toString().equals("Контракт без масти")) {
                dealer.giveBuyIn(winnerOfTrading);
                ArrayList<GameBot> botsWithoutContract = playerTradingStrategy.takeBotsWithoutContract(gameBots, winnerContract);
                playerTradingStrategy.tradeWhists(winnerContract, botsWithoutContract);
                //bots without contract whisting or not.
            }

            GameBot winnerOfTurn = new GameBot("");


            doTurns(bots, gameBots, winnerOfTurn, winnerContract);

            convention.countPoints(bots, winnerContract);

            //shitcode moves bots in array for next turn
            currentBot++;
            if (currentBot == 1) {
                currentBot = -1;
            }
        }
        countTotalPoints(gameBots, gamePool);
    }
    private void initializeBots() {
        bot1 = new GameBot("Player1");
        bot2 = new GameBot("Player2");
        bot3 = new GameBot("Player3");
    }

    private void setAliases() {
        bot1.setBotLeft(bot2);
        bot1.setBotRight(bot3);

        bot2.setBotLeft(bot3);
        bot2.setBotRight(bot1);

        bot3.setBotLeft(bot1);
        bot3.setBotRight(bot2);
    }

    private void doTurns(ArrayList<GameBot> bots, ArrayList<GameBot> gameBots, GameBot winnerOfTurn,
                        Contract winnerContract) {
        int countOfTurns = 10;
        int indexOfCurrentWinner = 0;

        for (int i = 0; i < countOfTurns; i++) {//turns
            //initialize table every turn
            Table table = new Table();
            //players put cards
            //table chooses trick winner

            if (winnerOfTurn == null) {
                bots.get(0).putCard(table, winnerContract, null);
                Suits turnSuit = table.getFirstCard().suit;
                bots.get(1).putCard(table, winnerContract, turnSuit);
                bots.get(2).putCard(table, winnerContract, turnSuit);
                winnerOfTurn = table.showTurnWinner(bots, turnSuit, winnerContract);//should check method/logic has done
                winnerOfTurn.addTrick();
                indexOfCurrentWinner = bots.indexOf(winnerOfTurn);
                bots.clear();
            }
            else {//sorting array with currentWinner as 0 element. needed 100%
                botsIndexes = countBotsIndexes(indexOfCurrentWinner);
                for (int j = 0; i < 2; i++) {
                    bots.add(gameBots.get(botsIndexes[j]));
                }
                bots.get(0).putCard(table, winnerContract, null);
                Suits turnSuit = table.getFirstCard().suit;
                bots.get(1).putCard(table, winnerContract, turnSuit);
                bots.get(2).putCard(table, winnerContract, turnSuit);
                winnerOfTurn = table.showTurnWinner(bots, turnSuit, winnerContract);//should check method/logic has done
                winnerOfTurn.addTrick();
                indexOfCurrentWinner = bots.indexOf(winnerOfTurn);
                bots.clear();
            }
        }
    }

    private void alignPool(ArrayList<GameBot> gameBots, int gamePool) {
        for (GameBot gameBot:
                gameBots) {
            if (gameBot.getPool() != gamePool) {
                int shortage = gamePool - gameBot.getPool();
                gameBot.addToPool(shortage);
                gameBot.addToMountain(shortage);
            }
        }
    }

    private void substractSmallestMountainFromAllMountains(ArrayList<GameBot> gameBots) {
        int[] mountains = {gameBots.get(0).getMountain(), gameBots.get(1).getMountain(), gameBots.get(2).getMountain()};
        Arrays.sort(mountains);
        for (GameBot gameBot:
             gameBots) {
            gameBot.setMountain(gameBot.getMountain() - mountains[0]);
        }
    }

    private int countMiddleMountain(ArrayList<GameBot> gameBots) {
        int mountSum = 0;
        for (GameBot gameBot:
             gameBots) {
            mountSum += gameBot.getMountain();
        }
        return mountSum * 10 / 3;
    }

    private void countMiddleMountainForEveryPlayer(ArrayList<GameBot> gameBots, int middleMountain) {
        for (GameBot gameBot:
             gameBots) {
            gameBot.setMountain(middleMountain - gameBot.getMountain() * 10);
        }
    }

    private void writeOffWhists(ArrayList<GameBot> gameBots) {
        GameBot bot1 = gameBots.get(0);
        GameBot bot2 = gameBots.get(1);
        GameBot bot3 = gameBots.get(2);
        int bot1LeftWhists = bot1.getWhistsToLeft() - bot2.getWhistsToRight();
        int bot1RightWhists = bot1.getWhistsToRight() - bot3.getWhistsToLeft();

        int bot2LeftWhists = bot2.getWhistsToLeft() - bot3.getWhistsToRight();
        int bot2RightWhists = bot2.getWhistsToRight() - bot1.getWhistsToLeft();

        int bot3LeftWhists = bot3.getWhistsToLeft() - bot1.getWhistsToRight();
        int bot3RightWhists = bot3.getWhistsToRight() - bot2.getWhistsToLeft();

        bot1.setTotalWhists(bot1LeftWhists + bot1RightWhists);
        bot2.setTotalWhists(bot2LeftWhists + bot2RightWhists);
        bot3.setTotalWhists(bot3LeftWhists + bot3RightWhists);
    }

    private void countTotalWhists(ArrayList<GameBot> gameBots) {
        for (GameBot gameBot:
                gameBots) {
            gameBot.setTotalWhists(gameBot.getMountain() + gameBot.getTotalWhists());
        }
    }

    private void countTotalPoints(ArrayList<GameBot> gameBots, int gamePool) {
        alignPool(gameBots, gamePool);
        substractSmallestMountainFromAllMountains(gameBots);
        int middleMountain = countMiddleMountain(gameBots);
        countMiddleMountainForEveryPlayer(gameBots, middleMountain);
        writeOffWhists(gameBots);
        countTotalWhists(gameBots);

        System.out.println("Player 1 has total whists: " + bot1.getTotalWhists());
        System.out.println("Player 2 has total whists: " + bot2.getTotalWhists());
        System.out.println("Player 3 has total whsits: " + bot3.getTotalWhists());
    }
}