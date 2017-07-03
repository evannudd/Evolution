package gamestates;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import entities.Asteroid;
import entities.Bullet;
import entities.Food;
import entities.Ship;
import managers.Game;
import managers.GameStateManager;
import managers.Settings;

import java.util.ArrayList;
import java.util.List;

public class PlayState extends gamestates.GameState {
    private ShapeRenderer sr;

    private ArrayList<Ship> ships;
    private ArrayList<ArrayList<Bullet>> bullets;
    private ArrayList<Asteroid> asteroids;
    private List<Food> food;

    private int numShips = Settings.NUMBER_OF_SHIPS;
    private int numAsteroids = Settings.INITIAL_NUMBER_OF_ASTEROIDS;
    private int numFood = Settings.NUMBER_OF_FOOD;

    public PlayState(GameStateManager gsm) {
        super(gsm);
    }

    public void init() {
        sr = new ShapeRenderer();

        ships = new ArrayList<>();
        bullets = new ArrayList<>();
        asteroids = new ArrayList<>();
        food = new ArrayList<>();

        for (int i = 0; i < numShips; i++) {
            bullets.add(new ArrayList<>());
            ships.add(new Ship(bullets.get(i)));
        }

        asteroids.add(new Asteroid(100, 100, Asteroid.LARGE));
        asteroids.add(new Asteroid(200, 100, Asteroid.MEDIUM));
        asteroids.add(new Asteroid(300, 100, Asteroid.SMALL));

        spawnAsteroids();
        spawnFood();
    }

    private float[] generatePositionFarFromShip(int min_distance) {
        double dist;
        float[] position = new float[2];

        do {
            position[0] = MathUtils.random(Game.WIDTH);
            position[1] = MathUtils.random(Game.HEIGHT);
            float dx = position[0] - ships.get(0).getx();
            float dy = position[1] - ships.get(0).gety();
            dist = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        } while (dist < min_distance);

        return position;
    }

    private void spawnShip() {
        bullets.add(new ArrayList<>());
        ships.add(new Ship(bullets.get(bullets.size() - 1)));
    }

    /**
     * Creates a new Asteroid.
     * TODO: decidir se mantenho a verificação de proximidade da posição inicial do asteroide à nave.
     */
    private void spawnSingleAsteroid() {
        float[] position = generatePositionFarFromShip(Settings.DISTANCE_SHIP_ASTEROID);
        asteroids.add(new Asteroid(position[0], position[1], Asteroid.LARGE));
    }

    private void spawnAsteroids() {
        asteroids.clear();

        for (int i = 0; i < numAsteroids; i++)
            spawnSingleAsteroid();
    }

    private void splitAsteroid(Asteroid a) {
        if (a.getType() == Asteroid.LARGE) {
            asteroids.add(new Asteroid(a.getx(), a.gety(), Asteroid.MEDIUM));
            asteroids.add(new Asteroid(a.getx(), a.gety(), Asteroid.MEDIUM));
        } else if (a.getType() == Asteroid.MEDIUM)
            asteroids.add(new Asteroid(a.getx(), a.gety(), Asteroid.SMALL));
    }

    private void spawnSingleFood() {
        float[] position = generatePositionFarFromShip(Settings.DISTANCE_SHIP_FOOD);
        food.add(new Food(position[0], position[1]));
    }

    private void spawnFood() {
        food.clear();

        for (int i = 0; i < numFood; i++)
            spawnSingleFood();
    }

    public void update(float dt) {
        // get user input
        handleInput();

        // update ship
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            s.nearestFood(asteroids);
            s.update(dt);
            if (s.shouldRemove()) {
                ships.remove(i);
                i--;
            }
        }

        // update ship bullets
        for (ArrayList<Bullet> bullets_of_ship : bullets)
            for (int i = 0; i < bullets_of_ship.size(); i++) {
                bullets_of_ship.get(i).update(dt);
                if (bullets_of_ship.get(i).shouldRemove()) {
                    bullets_of_ship.remove(i);
                    i--;
                }
            }

        // update asteroids
        for (int i = 0; i < asteroids.size(); i++) {
            Asteroid a = asteroids.get(i);
            a.update(dt);
            if (a.shouldRemove()) {
                asteroids.remove(i);
                i--;
            }
        }

        checkCollisions();

        while (ships.size() < numShips)
            spawnShip();
        while (asteroids.size() < numAsteroids)
            spawnSingleAsteroid();
        while (food.size() < numFood)
            spawnSingleFood();
    }

    /**
     * Checks if any Ship collided with a Asteroid.
     */
    private void checkShipsAsteroidsCollisions() {
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            for (int j = 0; j < asteroids.size(); j++) {
                Asteroid a = asteroids.get(j);
                if (a.intersects(s)) {
                    ships.remove(i);
                    bullets.remove(i);
                    i--;
                    asteroids.remove(j);
                    splitAsteroid(a);
                    break;
                }
            }
        }
    }

    /**
     * Check if any Ship collided with a Food.
     */
    private void checkShipsFoodCollisions() {
        for (Ship s : ships) {
            for (int j = 0; j < food.size(); j++) {
                Food f = food.get(j);
                if (s.contains(f.getx(), f.gety())) {
                    s.setTimer(0);
                    food.remove(j);
                    break;
                }
            }
        }
    }

    /**
     * Check if any Ship collided with a Bullet.
     */
    private void checkShipsBulletsCollisions() {
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            for (int j = 0; j < bullets.size(); j++)
                if (i != j) {
                    ArrayList<Bullet> enemy_bullets = bullets.get(j);
                    for (int k = 0; k < enemy_bullets.size(); k++) {
                        Bullet b = enemy_bullets.get(k);
                        if (s.contains(b.getx(), b.gety())) {
                            enemy_bullets.remove(b);
                            ships.remove(i);
                            bullets.remove(i);
                            i--;
                            j--;
                            break;
                        }
                    }
                }
        }
    }

    /**
     * Check if any Bullet collided with an Asteroid.
     */
    private void checkBulletsAsteroidsCollisions() {
        for (ArrayList<Bullet> bullets_flying : bullets)
            for (int i = 0; i < bullets_flying.size(); i++) {
                Bullet b = bullets_flying.get(i);
                for (int j = 0; j < asteroids.size(); j++) {
                    Asteroid a = asteroids.get(j);
                    if (a.contains(b.getx(), b.gety())) {
                        bullets_flying.remove(b);
                        i--;
                        asteroids.remove(a);
                        splitAsteroid(a);
                        break;
                    }
                }
            }
    }

    private void checkCollisions() {
        checkShipsBulletsCollisions();
        checkShipsFoodCollisions();
        checkShipsAsteroidsCollisions();
        checkBulletsAsteroidsCollisions();
    }

    private void drawShips() {
        for (Ship ship : ships)
            ship.draw(sr);
    }

    private void drawBullets() {
        for (ArrayList<Bullet> bullets_flying : bullets)
            for (Bullet bullet : bullets_flying)
                bullet.draw(sr);
    }

    private void drawAsteroids() {
        for (Asteroid asteroid : asteroids)
            asteroid.draw(sr);
    }

    private void drawFood() {
        for (Food f : food)
            f.draw(sr);
    }

    public void draw() {
        drawShips();
        drawBullets();
        drawAsteroids();
        drawFood();
    }

    public void handleInput() {
        /*
        ships.get(0).setLeft(GameKeys.isDown(GameKeys.LEFT));
        ships.get(0).setRight(GameKeys.isDown(GameKeys.RIGHT));
        ships.get(0).setUp(GameKeys.isDown(GameKeys.UP));
        if (GameKeys.isPressed(GameKeys.SPACE)) {
            ships.get(0).shoot();
        }
        */
    }

    public void dispose() {
    }
}