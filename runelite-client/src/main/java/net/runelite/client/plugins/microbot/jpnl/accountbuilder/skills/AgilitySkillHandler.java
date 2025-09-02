package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.ObjectComposition; // added
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;

/**
 * Complete Agility skill handler for AIO bot system.
 * Supports all major agility courses with level-based progression.
 */
public class AgilitySkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "auto"; // auto, gnome, draynor, alkharid, varrock, canifis, seers, pollnivneach, rellekka, ardougne
    private AgilityCourse currentCourse;
    private int currentObstacleIndex = 0;
    private int failCount = 0;
    private static final int MAX_FAIL_COUNT = 5;

    // Agility data structures
    private static class AgilityObstacle {
        String name;
        WorldPoint location;
        String action;

        public AgilityObstacle(String name, WorldPoint location, String action) {
            this.name = name;
            this.location = location;
            this.action = action;
        }
    }

    private static class AgilityCourse {
        String name;
        int levelRequired;
        int experience;
        List<AgilityObstacle> obstacles;
        WorldPoint startLocation;

        public AgilityCourse(String name, int levelRequired, int experience, List<AgilityObstacle> obstacles, WorldPoint startLocation) {
            this.name = name;
            this.levelRequired = levelRequired;
            this.experience = experience;
            this.obstacles = obstacles;
            this.startLocation = startLocation;
        }
    }

    // Agility courses database
    private static final List<AgilityCourse> AGILITY_COURSES = Arrays.asList(
            // Gnome Stronghold Course
            new AgilityCourse("Gnome Stronghold", 1, 85, Arrays.asList(
                    new AgilityObstacle("Log balance", new WorldPoint(2474, 3436, 0), "Walk-across"),
                    new AgilityObstacle("Obstacle net", new WorldPoint(2474, 3426, 0), "Climb-over"),
                    new AgilityObstacle("Tree branch", new WorldPoint(2473, 3420, 1), "Climb"),
                    new AgilityObstacle("Balancing rope", new WorldPoint(2477, 3418, 2), "Walk-on"),
                    new AgilityObstacle("Tree branch", new WorldPoint(2486, 3418, 2), "Climb-down"),
                    new AgilityObstacle("Obstacle net", new WorldPoint(2484, 3431, 0), "Climb-through")
            ), new WorldPoint(2474, 3436, 0)),

            // Draynor Village Rooftop Course
            new AgilityCourse("Draynor Village", 10, 120, Arrays.asList(
                    new AgilityObstacle("Rough wall", new WorldPoint(3103, 3279, 0), "Climb"),
                    new AgilityObstacle("Tightrope", new WorldPoint(3098, 3277, 3), "Cross"),
                    new AgilityObstacle("Tightrope", new WorldPoint(3092, 3276, 3), "Cross"),
                    new AgilityObstacle("Narrow wall", new WorldPoint(3089, 3264, 3), "Balance"),
                    new AgilityObstacle("Wall", new WorldPoint(3088, 3256, 3), "Jump-up"),
                    new AgilityObstacle("Gap", new WorldPoint(3095, 3255, 3), "Jump-to"),
                    new AgilityObstacle("Crate", new WorldPoint(3103, 3261, 3), "Jump-to")
            ), new WorldPoint(3103, 3279, 0)),

            // Al Kharid Rooftop Course
            new AgilityCourse("Al Kharid", 20, 180, Arrays.asList(
                    new AgilityObstacle("Rough wall", new WorldPoint(3273, 3195, 0), "Climb"),
                    new AgilityObstacle("Tightrope", new WorldPoint(3272, 3181, 3), "Cross"),
                    new AgilityObstacle("Cable", new WorldPoint(3265, 3161, 3), "Swing-on"),
                    new AgilityObstacle("Zip line", new WorldPoint(3257, 3159, 3), "Swing-on"),
                    new AgilityObstacle("Tropical tree", new WorldPoint(3244, 3159, 2), "Swing-on"),
                    new AgilityObstacle("Roof top beams", new WorldPoint(3236, 3164, 2), "Jump-on"),
                    new AgilityObstacle("Tightrope", new WorldPoint(3245, 3179, 2), "Cross"),
                    new AgilityObstacle("Gap", new WorldPoint(3251, 3188, 2), "Jump")
            ), new WorldPoint(3273, 3195, 0)),

            // Varrock Rooftop Course
            new AgilityCourse("Varrock", 30, 238, Arrays.asList(
                    new AgilityObstacle("Rough wall", new WorldPoint(3221, 3414, 0), "Climb"),
                    new AgilityObstacle("Clothes line", new WorldPoint(3214, 3414, 3), "Cross"),
                    new AgilityObstacle("Gap", new WorldPoint(3208, 3414, 3), "Leap"),
                    new AgilityObstacle("Wall", new WorldPoint(3197, 3416, 1), "Balance"),
                    new AgilityObstacle("Gap", new WorldPoint(3193, 3416, 3), "Leap"),
                    new AgilityObstacle("Gap", new WorldPoint(3208, 3403, 3), "Leap"),
                    new AgilityObstacle("Gap", new WorldPoint(3232, 3403, 3), "Leap"),
                    new AgilityObstacle("Ledge", new WorldPoint(3236, 3403, 3), "Hurdle")
            ), new WorldPoint(3221, 3414, 0)),

            // Canifis Rooftop Course
            new AgilityCourse("Canifis", 40, 240, Arrays.asList(
                    new AgilityObstacle("Tall tree", new WorldPoint(3505, 3488, 0), "Climb"),
                    new AgilityObstacle("Gap", new WorldPoint(3496, 3504, 2), "Jump"),
                    new AgilityObstacle("Gap", new WorldPoint(3485, 3499, 2), "Jump"),
                    new AgilityObstacle("Gap", new WorldPoint(3474, 3491, 3), "Jump"),
                    new AgilityObstacle("Gap", new WorldPoint(3478, 3481, 2), "Jump"),
                    new AgilityObstacle("Pole-vault", new WorldPoint(3488, 3468, 3), "Vault"),
                    new AgilityObstacle("Gap", new WorldPoint(3503, 3476, 3), "Jump"),
                    new AgilityObstacle("Gap", new WorldPoint(3518, 3484, 2), "Jump")
            ), new WorldPoint(3505, 3488, 0)),

            // Seers' Village Rooftop Course
            new AgilityCourse("Seers Village", 60, 570, Arrays.asList(
                    new AgilityObstacle("Wall", new WorldPoint(2729, 3489, 0), "Climb-up"),
                    new AgilityObstacle("Gap", new WorldPoint(2713, 3489, 3), "Jump"),
                    new AgilityObstacle("Tightrope", new WorldPoint(2710, 3477, 2), "Cross"),
                    new AgilityObstacle("Gap", new WorldPoint(2710, 3472, 3), "Jump"),
                    new AgilityObstacle("Gap", new WorldPoint(2715, 3458, 2), "Jump")
            ), new WorldPoint(2729, 3489, 0)),

            // Pollnivneach Rooftop Course
            new AgilityCourse("Pollnivneach", 70, 890, Arrays.asList(
                    new AgilityObstacle("Basket", new WorldPoint(3351, 2962, 0), "Climb-on"),
                    new AgilityObstacle("Market stall", new WorldPoint(3352, 2972, 1), "Jump-on"),
                    new AgilityObstacle("Banner", new WorldPoint(3360, 2977, 1), "Grab"),
                    new AgilityObstacle("Gap", new WorldPoint(3366, 2976, 1), "Leap"),
                    new AgilityObstacle("Tree", new WorldPoint(3365, 2982, 1), "Jump-to"),
                    new AgilityObstacle("Rough wall", new WorldPoint(3355, 2981, 1), "Climb"),
                    new AgilityObstacle("Monkeybars", new WorldPoint(3357, 2990, 2), "Cross"),
                    new AgilityObstacle("Tree", new WorldPoint(3370, 2995, 2), "Jump-to"),
                    new AgilityObstacle("Drying line", new WorldPoint(3366, 3000, 2), "Jump-to")
            ), new WorldPoint(3351, 2962, 0)),

            // Rellekka Rooftop Course
            new AgilityCourse("Rellekka", 80, 780, Arrays.asList(
                    new AgilityObstacle("Rough wall", new WorldPoint(2625, 3677, 0), "Climb"),
                    new AgilityObstacle("Gap", new WorldPoint(2622, 3672, 3), "Leap"),
                    new AgilityObstacle("Tightrope", new WorldPoint(2615, 3658, 3), "Cross"),
                    new AgilityObstacle("Gap", new WorldPoint(2626, 3654, 3), "Leap"),
                    new AgilityObstacle("Gap", new WorldPoint(2639, 3653, 3), "Hurdle"),
                    new AgilityObstacle("Tightrope", new WorldPoint(2643, 3649, 3), "Cross"),
                    new AgilityObstacle("Pile of fish", new WorldPoint(2655, 3676, 3), "Jump-in")
            ), new WorldPoint(2625, 3677, 0)),

            // Ardougne Rooftop Course
            new AgilityCourse("Ardougne", 90, 793, Arrays.asList(
                    new AgilityObstacle("Wooden Beams", new WorldPoint(2673, 3297, 0), "Climb-up"),
                    new AgilityObstacle("Gap", new WorldPoint(2671, 3310, 1), "Jump"),
                    new AgilityObstacle("Plank", new WorldPoint(2665, 3318, 3), "Walk-on"),
                    new AgilityObstacle("Gap", new WorldPoint(2656, 3318, 3), "Jump"),
                    new AgilityObstacle("Gap", new WorldPoint(2653, 3314, 3), "Jump"),
                    new AgilityObstacle("Steep roof", new WorldPoint(2653, 3300, 3), "Balance"),
                    new AgilityObstacle("Gap", new WorldPoint(2658, 3298, 3), "Jump")
            ), new WorldPoint(2673, 3297, 0))
    );

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "auto";
            failCount = 0; // Reset fail count when settings change
        }
    }

    @Override
    public void execute() {
        try {
            if (!enabled) {
                Microbot.status = "Agility: disabled";
                return;
            }

            if (!Microbot.isLoggedIn()) return;

            // Determine current course
            currentCourse = determineCurrentCourse();
            if (currentCourse == null) {
                handleFailure("No suitable agility course for level " + Microbot.getClient().getRealSkillLevel(Skill.AGILITY));
                return;
            }

            // Check if we're at the course
            if (!isAtCourse()) {
                walkToCourse();
                return;
            }

            // Do agility course
            attemptObstacle();

            // Reset fail count on successful execution
            failCount = 0;

        } catch (Exception e) {
            handleFailure("Agility error: " + e.getMessage());
        }
    }

    private AgilityCourse determineCurrentCourse() {
        int agilityLevel = Microbot.getClient().getRealSkillLevel(Skill.AGILITY);

        if ("auto".equals(mode)) {
            // Find highest level course we can do
            AgilityCourse bestCourse = null;
            for (AgilityCourse course : AGILITY_COURSES) {
                if (agilityLevel >= course.levelRequired) {
                    bestCourse = course;
                }
            }
            return bestCourse;
        } else {
            // Try to find specific course
            for (AgilityCourse course : AGILITY_COURSES) {
                if (course.name.toLowerCase().contains(mode) && agilityLevel >= course.levelRequired) {
                    return course;
                }
            }
            return null;
        }
    }

    private boolean isAtCourse() {
        if (currentCourse == null) return false;
        return Rs2Player.getWorldLocation().distanceTo(currentCourse.startLocation) <= 20;
    }

    private void walkToCourse() {
        if (currentCourse == null) return;

        Microbot.status = "Agility: Walking to " + currentCourse.name + " course";
        Rs2Walker.walkTo(currentCourse.startLocation);
    }

    private void attemptObstacle() {
        if (currentCourse == null || currentCourse.obstacles.isEmpty()) return;

        // Determine which obstacle we should be at
        currentObstacleIndex = findCurrentObstacle();

        if (currentObstacleIndex >= currentCourse.obstacles.size()) {
            currentObstacleIndex = 0; // Reset to start of course
        }

        AgilityObstacle obstacle = currentCourse.obstacles.get(currentObstacleIndex);

        // Find and interact with obstacle using modern API
        List<GameObject> nearbyObjects = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition comp = Rs2GameObject.convertToObjectComposition(obj);
            String objName = comp != null ? comp.getName() : null;
            return objName != null && objName.contains(obstacle.name) &&
                obj.getWorldLocation().distanceTo(obstacle.location) <= 15;
        });

        if (!nearbyObjects.isEmpty()) {
            GameObject obstacleObject = nearbyObjects.get(0);
            if (Rs2GameObject.interact(obstacleObject, obstacle.action)) {
                Microbot.status = "Agility: " + obstacle.action + " " + obstacle.name + " (" + (currentObstacleIndex + 1) + "/" + currentCourse.obstacles.size() + ")";

                // Wait for obstacle completion
                waitForObstacleCompletion();

                // Move to next obstacle
                currentObstacleIndex = (currentObstacleIndex + 1) % currentCourse.obstacles.size();
            }
        } else {
            // Try to walk closer to obstacle
            if (Rs2Player.getWorldLocation().distanceTo(obstacle.location) > 5) {
                Rs2Walker.walkTo(obstacle.location);
            }
        }
    }

    private int findCurrentObstacle() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        int closestObstacle = 0;
        int shortestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < currentCourse.obstacles.size(); i++) {
            AgilityObstacle obstacle = currentCourse.obstacles.get(i);
            int distance = playerLocation.distanceTo(obstacle.location);

            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestObstacle = i;
            }
        }

        return closestObstacle;
    }

    private void waitForObstacleCompletion() {
        // Wait while player is moving/animating on obstacle
        long startTime = System.currentTimeMillis();
        while ((Rs2Player.isMoving() || Rs2Player.isAnimating()) &&
               System.currentTimeMillis() - startTime < 10000) {
            sleep(100, 200);
        }

        // Additional wait to ensure completion
        sleep(1000, 2000);
    }

    private void handleFailure(String message) {
        failCount++;
        Microbot.status = "Agility: " + message + " (Fails: " + failCount + "/" + MAX_FAIL_COUNT + ")";

        if (failCount >= MAX_FAIL_COUNT) {
            Microbot.status = "Agility: Max failures reached, moving to next task";
            enabled = false; // This will cause the queue to move to next task
        }
    }

    private void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Getters for debugging
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
    public AgilityCourse getCurrentCourse() { return currentCourse; }
    public int getCurrentObstacleIndex() { return currentObstacleIndex; }
    public int getFailCount() { return failCount; }
}