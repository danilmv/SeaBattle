import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainWindow extends JFrame {
    private final int WIDTH = 600;
    private final int HEIGHT = 440;

//    private final GameField humanField = new GameField(Color.BLUE, 400, 400, true, this, 0);
    private final GameField humanField = new GameField(Color.BLUE, 400, 400, false, this, 0);
    private final GameField aiField = new GameField(Color.GREEN, 200, 200, false, this, 1);

    private JTextArea txtArea;

    MainWindow() {
        setLocationRelativeTo(null);
        setSize(WIDTH, HEIGHT);
        setTitle("Морской бой");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

        add(createLeftPanel());
        add(createRigthPanel());

        pack();
        setVisible(true);

    }

    private JPanel createLeftPanel() {
        JPanel pnlLeft = new JPanel();
        pnlLeft.setLayout(new BorderLayout());
        pnlLeft.add(humanField);

        JPanel pnlButtonsLeft = new JPanel();

        JButton btnNewGame = new JButton("Новая игра");
        btnNewGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                humanField.startGame(aiField);
                aiField.startGame(humanField);

                addTextLn("\n\n\n\nНовая игра");

                humanField.yourShot();
            }
        });
        pnlButtonsLeft.add(btnNewGame);
        JButton btnExit = new JButton("Выйти");
        btnExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        pnlButtonsLeft.add(btnExit);

        pnlLeft.add(pnlButtonsLeft, BorderLayout.SOUTH);

        return pnlLeft;
    }

    private JPanel createRigthPanel() {
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BorderLayout());

        pnlRight.add(aiField, BorderLayout.NORTH);

        txtArea = new JTextArea();
        txtArea.setEditable(false);
        txtArea.setLineWrap(true);
        JScrollPane scrPane = new JScrollPane(txtArea);
        pnlRight.add(scrPane, BorderLayout.CENTER);

        JPanel pnlButtonsRight = new JPanel();

        JButton btnShow = new JButton("Показать поле");
        btnShow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                humanField.setHidden(!humanField.getHidden());
                humanField.repaint();
            }
        });
        pnlButtonsRight.add(btnShow);

        pnlRight.add(pnlButtonsRight, BorderLayout.SOUTH);

        return pnlRight;
    }

    public void addTextLn(String str) {
        if (txtArea != null)
            txtArea.append(str + "\n");
    }
    public void addText(String str) {
        if (txtArea != null)
            txtArea.append(str);
    }
}
