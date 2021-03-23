import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

public class GameField extends JPanel {

    private final int FIELD_SIZE = 10;

    private final int GAME_MODE_INIT = 0;
    private final int GAME_MODE_STARTED = 1;
    private final int GAME_MODE_FINISHED = 2;
    private int gameMode = GAME_MODE_INIT;

    private final Color COLOR_LINES = Color.GRAY;
    private final Color COLOR_BACKGROUND = Color.WHITE;
    private final Color COLOR_SHIP = Color.BLACK;
    private final Color COLOR_SHIP_HIT = Color.RED;
    private final Color COLOR_SHOT = Color.ORANGE;
    private final Color COLOR_MARKED = Color.CYAN;

    private int cellWidth;
    private int cellHeight;

    private MainWindow main;

    private int[] shipsTemplate = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

    private int[][][] field;
    private final int FIELD_CLEAR = 0;
    private final int FIELD_SHIP = 1;
    private final int FIELD_BORDER = 2;
    private final int FIELD_MARKED = 4;
    private final int FIELD_SHIP_HIT = 8;
    private final int FIELD_SHOT = 16;

    private Ship[] ships;
    private int shipsAlive;
    private boolean active;

    private final Random rand = new Random();

    private final int SHOOTING_RESULT_MISSED = 0;
    private final int SHOOTING_RESULT_HIT = 1;
    private final int SHOOTING_RESULT_KILLED = 2;
    private final int SHOOTING_RESULT_REPEAT = 4;


    private static int currentPlayerTurn;

    private int playerId;

    private boolean hidden = true;

    private GameField opponent;

    private long time;

    // AI's variables
    private final int DIRECTION_UNKNOWN = -1;
    private final int DIRECTION_HORIZONTAL = 0;
    private final int DIRECTION_VERTICAL = 1;

    private int checkingDirection = DIRECTION_UNKNOWN;
    private int dx;
    private int dy;

    private int x = -1;
    private int y = -1;
    private int res = -1;

    private final Coordinate[] checks = {new Coordinate(1, 0), new Coordinate(-1, 0),
            new Coordinate(0, 1), new Coordinate(0, -1)};

    private boolean[] checkedDirections = {false, false, false, false};

    private Coordinate lastGoodShot = new Coordinate();


    GameField(Color color, int Width, int Height, boolean active, MainWindow main, int id) {
        setPreferredSize(new Dimension(Width, Height));
        setBackground(color);

        this.main = main;
        this.active = active;
        this.playerId = id;

        this.hidden = active;

        if (active)
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);

                    if (gameMode != GAME_MODE_STARTED)
                        return;

                    if (currentPlayerTurn != playerId)
                        return;

                    int x = e.getX() / cellWidth;
                    int y = e.getY() / cellHeight;

                    if (x < 0) x = 0;
                    if (y < 0) y = 0;
                    if (x >= FIELD_SIZE) x = FIELD_SIZE - 1;
                    if (y >= FIELD_SIZE) y = FIELD_SIZE - 1;

                    if (e.getButton() == MouseEvent.BUTTON1) {
                        int res = shootAt(x, y);
                        switch (res) {
                            case SHOOTING_RESULT_HIT:
                            case SHOOTING_RESULT_KILLED:
                            case SHOOTING_RESULT_REPEAT:
                                break;
                            case SHOOTING_RESULT_MISSED:
                                nextTurn();
                        }
                    } else {
                        field[x][y][0] ^= FIELD_MARKED;
                    }

                    repaint();

                    if (currentPlayerTurn != playerId)
                        opponent.yourShot();
                }
            });
    }

    private Coordinate getCloseShot(Coordinate shot) {
        Coordinate checking = new Coordinate();
        int val = -1;

        for (int j = 0; j < 4; j++) {
            checking.x = shot.x;
            checking.y = shot.y;

            switch (checkingDirection) {
                case DIRECTION_UNKNOWN:
                    val = rand.nextInt(4);
                    while (checkedDirections[val])
                        val = (val + 1) % 4; //!!! возможен вечный цикл
                    break;
                case DIRECTION_HORIZONTAL:
                    val = rand.nextInt(2);
                    checkedDirections[2] = checkedDirections[3] = true;
                    if (checkedDirections[val])
                        val = (val + 1) % 2;
                    break;
                case DIRECTION_VERTICAL:
                    val = rand.nextInt(2) + 2;
                    checkedDirections[0] = checkedDirections[1] = true;
                    if (checkedDirections[val])
                        val = (val - 2 + 1) % 2 + 2;
                    break;
            }

            dx = 0;
            dy = 0;

            switch (val) {
                case 0:
                    dx = 1;
                    break;
                case 1:
                    dx = -1;
                    break;
                case 2:
                    dy = 1;
                    break;
                case 3:
                    dy = -1;
                    break;
            }

            for (int i = 0; i < 4; i++) {
                checking.x = checking.x + dx;
                checking.y = checking.y + dy;

                if (checking.x < 0 || checking.x >= FIELD_SIZE || checking.y < 0 || checking.y >= FIELD_SIZE) {
                    checkedDirections[val] = true;
                    break;
                }

                if ((field[checking.x][checking.y][0] & FIELD_SHIP_HIT) == FIELD_SHIP_HIT)
                    continue;

                if ((field[checking.x][checking.y][0] & FIELD_SHOT) == FIELD_SHOT) {
                    checkedDirections[val] = true;
                    break;
                }

                if ((field[checking.x][checking.y][0] & FIELD_MARKED) == FIELD_MARKED) {
                    checkedDirections[val] = true;
                    break;
                }

                return checking;
            }
        }

        return checking;
    }

    private void initAI() {
        checkingDirection = DIRECTION_UNKNOWN;
        for (int i = 0; i < checkedDirections.length; i++) {
            checkedDirections[i] = false;
        }
        lastGoodShot.clear();
        dx = dy = 0;
        res = -1;
    }

    public void yourShot() {
        Coordinate coord;

        if (gameMode != GAME_MODE_STARTED)
            return;
        if (active)
            return;
        if (currentPlayerTurn != playerId)
            return;

//        do {
        if (System.nanoTime() - time > 1000000000L) {
            if (res == SHOOTING_RESULT_KILLED) {
                markAI(field[x][y][1]);
                initAI();
            } else if (res == SHOOTING_RESULT_HIT) {
                if (lastGoodShot.x < 0) {
                    lastGoodShot.x = x;
                    lastGoodShot.y = y;
                } else {
                    if (dx != 0)
                        checkingDirection = DIRECTION_HORIZONTAL;
                    else if (dy != 0)
                        checkingDirection = DIRECTION_VERTICAL;
                }
            }

            if (lastGoodShot.x >= 0) {
                coord = getCloseShot(lastGoodShot);
                x = coord.x;
                y = coord.y;
            } else {
                x = rand.nextInt(FIELD_SIZE);
                y = rand.nextInt(FIELD_SIZE);
            }

            if ((field[x][y][0] & FIELD_MARKED) == FIELD_MARKED)
                return;
//                continue;


            res = shootAt(x, y);
            if (res != SHOOTING_RESULT_REPEAT) {
                time = System.nanoTime();
                //repaint();
            }
        }
//        } while (shipsAlive > 0 && res != SHOOTING_RESULT_MISSED);
        if (shipsAlive > 0 && res == SHOOTING_RESULT_MISSED)
            nextTurn();
    }

    private void markAI(int id) {
        Ship ship = ships[id];
        int dx = 0, dy = 0;
        int x0, x1, y0, y1;
        int direction = ship.getDirection();
        int length = ship.getLength();
        int x = ship.getX();
        int y = ship.getY();

        if (direction == DIRECTION_HORIZONTAL) {
            dx = 1;
            x0 = x - 1;
            x1 = x + length;
            y0 = y - 1;
            y1 = y + 1;
        } else {
            dy = 1;
            x0 = x - 1;
            x1 = x + 1;
            y0 = y - 1;
            y1 = y + length;
        }

        if (x0 < 0) x0 = 0;
        if (x1 >= FIELD_SIZE) x1 = FIELD_SIZE - 1;

        if (y0 < 0) y0 = 0;
        if (y1 >= FIELD_SIZE) y1 = FIELD_SIZE - 1;

        for (x = x0; x <= x1; x++) {
            for (y = y0; y <= y1; y++) {
                field[x][y][0] |= FIELD_MARKED;
            }
        }
    }

    private void nextTurn() {
        currentPlayerTurn = (currentPlayerTurn + 1) % 2;
        opponent.time = System.nanoTime();
        opponent.res = -1;
        //opponent.yourShot();
    }

    public int shootAt(int x, int y) {

        if ((field[x][y][0] & FIELD_SHOT) == FIELD_SHOT)
            return SHOOTING_RESULT_REPEAT;

        field[x][y][0] |= FIELD_SHOT;

        main.addText("Игрок " + playerId + " выстрелил по полю (" + x + ";" + y + ")...");


        if ((field[x][y][0] & FIELD_SHIP) == FIELD_SHIP) {
            field[x][y][0] |= FIELD_SHIP_HIT;

            if (ships[field[x][y][1]].hit()) {
                main.addTextLn("и попал в корабль!");
                return SHOOTING_RESULT_HIT;
            } else {
                shipsAlive--;
                main.addTextLn("и потопил корабль!!!");

                if (shipsAlive <= 0) {
                    gameMode = GAME_MODE_FINISHED;
                    opponent.gameMode = GAME_MODE_FINISHED;
                    main.addTextLn(">>>>Игрок " + playerId + " <<<<<, поздравляю, вы победили!");
                }
                return SHOOTING_RESULT_KILLED;
            }
        }
        main.addTextLn("и промахнулся");
        return SHOOTING_RESULT_MISSED;
    }

    public void startGame(GameField opponent) {
        gameMode = GAME_MODE_STARTED;

        cellHeight = getHeight() / FIELD_SIZE;
        cellWidth = getWidth() / FIELD_SIZE;

        this.opponent = opponent;

        field = new int[FIELD_SIZE][FIELD_SIZE][2];
        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                field[i][j][0] = FIELD_CLEAR;
                field[i][j][1] = -1;
            }
        }

        ships = new Ship[shipsTemplate.length];

        boolean result;
        for (int i = 0; i < shipsTemplate.length; i++) {
            ships[i] = new Ship(i);
            do {
                result = ships[i].setShip(rand.nextInt(FIELD_SIZE), rand.nextInt(FIELD_SIZE), shipsTemplate[i], rand.nextInt(2));
            } while (!result);
        }

        shipsAlive = ships.length;

        hidden = active;

        currentPlayerTurn = 0;

        initAI();

        repaint();
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean getHidden() {
        return hidden;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        render(g);

        if(gameMode == GAME_MODE_STARTED) {
            yourShot();
            repaint();
        }
    }

    private void render(Graphics g) {
        if (gameMode == GAME_MODE_INIT)
            return;

        for (int x = 0; x < FIELD_SIZE; x++)
            for (int y = 0; y < FIELD_SIZE; y++) {
                if ((field[x][y][0] & FIELD_SHIP_HIT) == FIELD_SHIP_HIT) {
                    g.setColor(COLOR_SHIP_HIT);
                    g.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                } else if ((field[x][y][0] & FIELD_SHIP) == FIELD_SHIP) {
                    if (!hidden) {
                        g.setColor(COLOR_SHIP);
                        g.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                    }
                } else if ((field[x][y][0] & FIELD_SHOT) == FIELD_SHOT) {
                    g.setColor(COLOR_SHOT);
                    g.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                } else if ((field[x][y][0] & FIELD_MARKED) == FIELD_MARKED) {
                    g.setColor(COLOR_MARKED);
                    g.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }


        g.setColor(COLOR_LINES);

        for (int x = 0; x <= getWidth(); x += cellWidth) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y <= getHeight(); y += cellHeight) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    class Coordinate {
        Coordinate() {
        }

        Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x;
        public int y;

        public void clear() {
            x = -1;
            y = -1;
        }
    }

    class Ship {
        private int x;
        private int y;
        private int length;
        private int hp;
        private int id;

        private int direction;

        Ship(int id) {
            this.id = id;
        }

        private void addShipToField() {
            int dx = 0, dy = 0;
            int x0, x1, y0, y1;

            if (direction == DIRECTION_HORIZONTAL) {
                dx = 1;
                x0 = x - 1;
                x1 = x + length;
                y0 = y - 1;
                y1 = y + 1;
            } else {
                dy = 1;
                x0 = x - 1;
                x1 = x + 1;
                y0 = y - 1;
                y1 = y + length;
            }

            if (x0 < 0) x0 = 0;
            if (x1 >= FIELD_SIZE) x1 = FIELD_SIZE - 1;

            if (y0 < 0) y0 = 0;
            if (y1 >= FIELD_SIZE) y1 = FIELD_SIZE - 1;

            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    field[x][y][0] |= FIELD_BORDER;
                }
            }
            for (int i = 0; i < length; i++) {
                field[x + i * dx][y + i * dy][0] |= FIELD_SHIP;
                field[x + i * dx][y + i * dy][1] = id;
            }
        }

        public boolean setShip(int x, int y, int length, int direction) {
            this.x = x;
            this.y = y;
            this.length = length;
            this.direction = direction;
            this.hp = length;

            if (checkBorder() && checkField()) {
                addShipToField();
            } else
                return false;

//            for (int i = 0; i < ships.length; i++) {
//                if (!checkCollision(ships[i]))
//                    return false;
//            }
            return true;
        }

        public boolean hit() {
            return --hp > 0;
        }

        public boolean isAlive() {
            return hp > 0;
        }

        private boolean checkField() {
            int dx = 0, dy = 0, x = this.x, y = this.y;
            if (direction == DIRECTION_HORIZONTAL)
                dx = 1;
            else
                dy = 1;

            for (int i = 0; i < length; i++) {
                if (field[x + i * dx][y + i * dy][0] != FIELD_CLEAR)
                    return false;
            }
            return true;
        }

        private boolean checkBorder() {
            if (x < 0 || y < 0 || x >= FIELD_SIZE || y >= FIELD_SIZE || getTailY() >= FIELD_SIZE || getTailX() >= FIELD_SIZE)
                return false;

            return true;
        }

        private boolean checkCollision(Ship ship) {
            if (ship == null)
                return true;

            int dx = 0, dy = 0, x = ship.getX(), y = getY();

            if (direction == DIRECTION_HORIZONTAL)
                dx = 1;
            else
                dy = 1;

            for (int i = 0; i < ship.getLength(); i++) {
                if (!checkCollision(x + i * dx, y + i * dy))
                    return false;
            }

            return true;
        }

        private boolean checkCollision(int x, int y) {
            int x0, x1, y0, y1;
            if (direction == DIRECTION_HORIZONTAL) {
                x0 = x - 1;
                x1 = x + 1;
                y0 = y - 1;
                y1 = y + length;
            } else {
                x0 = x - 1;
                x1 = x + length;
                y0 = y - 1;
                y1 = y + 1;
            }

            if (x >= x0 && x <= x1 && y >= y0 && y <= y1)
                return false;

            return true;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getLength() {
            return length;
        }

        public int getDirection() {
            return direction;
        }

        public int getTailX() {
            if (direction == DIRECTION_HORIZONTAL)
                return x + length - 1;
            else
                return x;
        }

        public int getTailY() {
            if (direction == DIRECTION_HORIZONTAL)
                return y;
            else
                return y + length - 1;
        }
    }
}