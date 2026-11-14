package tree;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

public class TreeRenderer {
    private static final int DX = 60, DY = 80, R = 16, MARGIN = 40;

    public static void savePng(Node root, LevelPrinter.Result r, String path) {
        System.setProperty("java.awt.headless", "true");

        Map<Node, Double> xs = new IdentityHashMap<>();
        Map<Node, Integer> lv = new IdentityHashMap<>();
        int[] leaves = {0};
        layout(root, xs, lv, leaves);

        int height = lv.get(root);
        int w = Math.max(1, leaves[0]) * DX + 2 * MARGIN;
        int h = height * DY + 2 * MARGIN + 30;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        drawEdges(g, root, xs, lv, height);
        drawNodes(g, root, xs, lv, height, r.names());
        g.dispose();

        try {
            File f = new File(path);
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            ImageIO.write(img, "png", f);
            System.out.println("Tree image: " + f.getPath());
        } catch (IOException e) {
            System.out.println("Could not save image: " + e.getMessage());
        }
    }

    private static void layout(Node n, Map<Node, Double> xs, Map<Node, Integer> lv, int[] leaves) {
        if (n instanceof BinOp b) {
            layout(b.left(), xs, lv, leaves);
            layout(b.right(), xs, lv, leaves);
            xs.put(n, (xs.get(b.left()) + xs.get(b.right())) / 2.0);
            lv.put(n, 1 + Math.max(lv.get(b.left()), lv.get(b.right())));
        } else if (n instanceof Call c) {
            layout(c.arg(), xs, lv, leaves);
            xs.put(n, xs.get(c.arg()));
            lv.put(n, 1 + lv.get(c.arg()));
        } else {
            xs.put(n, (double) leaves[0]++);
            lv.put(n, 0);
        }
    }

    private static int px(Node n, Map<Node, Double> xs) {
        return (int) (MARGIN + xs.get(n) * DX + DX / 2.0);
    }

    private static int py(Node n, Map<Node, Integer> lv, int height) {
        return MARGIN + (height - lv.get(n)) * DY;
    }

    private static Node[] children(Node n) {
        if (n instanceof BinOp b) return new Node[]{b.left(), b.right()};
        if (n instanceof Call c) return new Node[]{c.arg()};
        return new Node[0];
    }

    private static void drawEdges(Graphics2D g, Node n, Map<Node, Double> xs,
                                  Map<Node, Integer> lv, int height) {
        g.setColor(Color.BLACK);
        for (Node c : children(n)) {
            g.drawLine(px(n, xs), py(n, lv, height), px(c, xs), py(c, lv, height));
            drawEdges(g, c, xs, lv, height);
        }
    }

    private static void drawNodes(Graphics2D g, Node n, Map<Node, Double> xs,
                                  Map<Node, Integer> lv, int height, Map<Node, String> names) {
        int x = px(n, xs), y = py(n, lv, height);
        FontMetrics fm = g.getFontMetrics();
        for (Node c : children(n)) drawNodes(g, c, xs, lv, height, names);

        if (n instanceof BinOp b) {
            Ellipse2D circle = new Ellipse2D.Double(x - R, y - R, 2 * R, 2 * R);
            g.setColor(Color.WHITE);
            g.fill(circle);
            g.setColor(Color.BLACK);
            g.draw(circle);
            String op = String.valueOf(b.op());
            g.drawString(op, x - fm.stringWidth(op) / 2, y + fm.getAscent() / 2 - 1);
            g.drawString(names.get(n).substring(1), x + R + 3, y - R / 2);
        } else if (n instanceof Call c) {
            int bw = Math.max(2 * R, fm.stringWidth(c.name()) + 12);
            RoundRectangle2D box = new RoundRectangle2D.Double(x - bw / 2.0, y - R, bw, 2 * R, 12, 12);
            g.setColor(Color.WHITE);
            g.fill(box);
            g.setColor(Color.BLACK);
            g.draw(box);
            g.drawString(c.name(), x - fm.stringWidth(c.name()) / 2, y + fm.getAscent() / 2 - 1);
            g.drawString(names.get(n).substring(1), x + bw / 2 + 3, y - R / 2);
        } else {
            String name = ((Leaf) n).name();
            g.setColor(Color.BLACK);
            g.drawString(name, x - fm.stringWidth(name) / 2, y + 20);
        }
    }
}