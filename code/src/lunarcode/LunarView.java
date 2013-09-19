package lunarcode;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by Samuel Roberts, 2013
 */
public class LunarView extends JComponent {

    private LunarGame game;
    private LunarShip ship;

    private Font statFont = new Font("sans serif", Font.BOLD, 16);

    public LunarView(LunarGame game, LunarShip ship) {
        this.game = game;
        this.ship = ship;
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, LunarParams.worldWidth, LunarParams.worldHeight);

        // draw terrain
        game.terrain.draw(g2d);
            
        // draw ship
        synchronized(LunarGame.class) {
            ship.draw(g2d);
        }
        g2d.setColor(Color.WHITE);
        g2d.setFont(statFont);
        g2d.drawString("Ticks: " + game.getTicks(), 10, 20);
    }

    public Dimension getPreferredSize() {
        return new Dimension(LunarParams.worldWidth, LunarParams.worldHeight);
    }

}
