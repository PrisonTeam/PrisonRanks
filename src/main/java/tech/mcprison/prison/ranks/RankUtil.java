/*
 * Copyright (C) 2017 The MC-Prison Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tech.mcprison.prison.ranks;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.economy.Economy;
import tech.mcprison.prison.internal.Player;
import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.ranks.data.Rank;
import tech.mcprison.prison.ranks.data.RankLadder;
import tech.mcprison.prison.ranks.data.RankPlayer;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Optional;

/**
 * Utilities for changing the ranks of players.
 *
 * @author Faizaan A. Datoo
 */
public class RankUtil {

    /*
     * Fields & Constants
     */

    public static final int RANKUP_SUCCESS = 0, RANKUP_FAILURE = 1, RANKUP_HIGHEST = 2,
        RANKUP_CANT_AFFORD = 3;

    /*
     * Constructor
     */

    private RankUtil() {
    }

    /*
     * Method
     */

    /**
     * Sends the player to the next rank.
     *
     * @param player     The {@link RankPlayer} to rank up.
     * @param ladderName The name of the ladder to rank up this player on.
     */
    public static RankUpResult rankUpPlayer(RankPlayer player, String ladderName) {

        // Store all of the data we need. If anything here is not present, there
        // is a high chance of data corruption. TODO Fail gracefully here

        Player prisonPlayer = Prison.get().getPlatform().getPlayer(player.uid).get();
        RankLadder ladder =
            PrisonRanks.getInstance().getLadderManager().getLadder(ladderName).get();
        Rank currentRank = player.getRank(ladder).get(); // TODO Add them to the lowest rank
        Optional<Rank> nextRankOptional = ladder.getNext(ladder.getPositionOfRank(currentRank));

        if (!nextRankOptional.isPresent()) {
            return new RankUpResult(RANKUP_HIGHEST,
                currentRank); // We're already at the highest rank.
        }

        Rank nextRank = nextRankOptional.get();

        // We're going to be making a transaction here
        // We'll check if the player can afford it first, and if so, we'll make the transaction and proceed.

        Economy economy = Prison.get().getPlatform().getEconomy();
        if (!economy.canAfford(prisonPlayer, nextRank.cost)) {
            return new RankUpResult(RANKUP_CANT_AFFORD, nextRank);
        }

        economy.removeBalance(prisonPlayer, nextRank.cost);

        player.addRank(ladder, nextRank);

        try {
            PrisonRanks.getInstance().getPlayerManager().savePlayer(player);
        } catch (IOException e) {
            Output.get().logError("An error occurred while saving player files.", e);
            return new RankUpResult(RANKUP_FAILURE, null);
        }

        return new RankUpResult(RANKUP_SUCCESS, nextRank);
    }

    public static String doubleToDollarString(double val) {
        return NumberFormat.getCurrencyInstance().format(val);
    }

    /*
     * Member Classes
     */


    static class RankUpResult {

        public int status;
        public Rank rank;

        public RankUpResult(int status, Rank rank) {
            this.status = status;
            this.rank = rank;
        }
    }


}