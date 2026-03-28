package com.enhancedjobs.quests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The complete quest pool for the Farmer profession.
 * Each template generates an {@link ActiveQuest} with a random amount
 * and proportionally scaled XP when {@link QuestTemplate#generate()} is called.
 */
public class FarmerQuestPool {

    private static final List<QuestTemplate> TEMPLATES = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        // 1. Harvest wheat (1000–5000, 100–600 XP)
        TEMPLATES.add(new QuestTemplate(
                "harvest_wheat", "FARMER", QuestTaskType.HARVEST_WHEAT,
                "Harvest {amount} wheat",
                1000, 5000, 100, 600));

        // 2. Breed cows (10–50, 100–300 XP)
        TEMPLATES.add(new QuestTemplate(
                "breed_cows", "FARMER", QuestTaskType.BREED_COWS,
                "Breed {amount} cows",
                10, 50, 100, 300));

        // 3. Plant crops (500–7500, 100–600 XP)
        TEMPLATES.add(new QuestTemplate(
                "plant_crops", "FARMER", QuestTaskType.PLANT_CROPS,
                "Plant {amount} crops",
                500, 7500, 100, 600));

        // 4. Collect eggs (50–250, 200–1000 XP)
        TEMPLATES.add(new QuestTemplate(
                "collect_eggs", "FARMER", QuestTaskType.COLLECT_EGGS,
                "Collect {amount} eggs",
                50, 250, 200, 1000));

        // 5. Milk cows (50–300, 50–300 XP)
        TEMPLATES.add(new QuestTemplate(
                "milk_cows", "FARMER", QuestTaskType.MILK_COWS,
                "Milk {amount} cows",
                50, 300, 50, 300));

        // 6. Create a scarecrow (fixed amount 1, fixed 250 XP)
        TEMPLATES.add(new QuestTemplate(
                "create_scarecrow", "FARMER", QuestTaskType.CREATE_SCARECROW,
                "Craft a Scarecrow",
                1, 1, 250, 250));

        // 7. Eat melons/apples/carrots (40–650, 30–600 XP)
        TEMPLATES.add(new QuestTemplate(
                "eat_food", "FARMER", QuestTaskType.EAT_FOOD,
                "Eat {amount} melons, apples, or carrots",
                40, 650, 30, 600));

        // 8. Harvest carrots (500–2500, 200–900 XP)
        TEMPLATES.add(new QuestTemplate(
                "harvest_carrots", "FARMER", QuestTaskType.HARVEST_CARROTS,
                "Harvest {amount} carrots",
                500, 2500, 200, 900));

        // 9. Sell crops to vendor (1000–10000, 100–1000 XP)
        TEMPLATES.add(new QuestTemplate(
                "sell_crops", "FARMER", QuestTaskType.SELL_CROPS,
                "Sell {amount} crops to a vendor",
                1000, 10000, 100, 1000));
    }

    /**
     * Returns a shuffled sample of {@code count} distinct quest templates.
     * If count exceeds the pool size, returns all templates shuffled.
     */
    public static List<QuestTemplate> pickRandom(int count) {
        List<QuestTemplate> pool = new ArrayList<>(TEMPLATES);
        Collections.shuffle(pool, RANDOM);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    /** Returns the full unmodifiable template list. */
    public static List<QuestTemplate> getAll() {
        return Collections.unmodifiableList(TEMPLATES);
    }
}
