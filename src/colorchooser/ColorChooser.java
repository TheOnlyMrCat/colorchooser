package colorchooser;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

public final class ColorChooser {

	/**
	 * Whether the mouse has moved since the last color update
	 */
	private static volatile boolean mouseHasMoved;

	/**
	 * The mouse's x and y positions relative to the frame
	 */
	private static volatile int mouseX, mouseY;

	/**
	 * The current color chosen
	 */
	private static volatile Color colour = Color.BLACK;

	/**
	 * The color currently displayed on the JPanel
	 */
	private static Color fadeFrom = Color.BLACK;

	/**
	 * The color of the text
	 */
	private static Color textColour = Color.WHITE;

	private static String asHex() {
		return String.format("#%06x", colour.getRGB() & 0xffffff);
	}
	private static String asRGB() {
		return String.format("%03d, %03d, %03d", colour.getRed(), colour.getGreen(), colour.getBlue());
	}
	private static String asHSV() {
		float[] hsb = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);
		return String.format("%03d, %03d, %03d", Math.round(hsb[0] * 360), Math.round(hsb[1] * 100), Math.round(hsb[2] * 100));
	}
	private static String asHSL() {
		float[] rgb = colour.getRGBColorComponents( null );
		float r = rgb[0];
		float g = rgb[1];
		float b = rgb[2];

		//	Minimum and Maximum RGB values are used in the HSL calculations

		float min = Math.min(r, Math.min(g, b));
		float max = Math.max(r, Math.max(g, b));

		//  Calculate the Hue

		float h = 0;

		if (max == min)
			h = 0;
		else if (max == r)
			h = ((60 * (g - b) / (max - min)) + 360) % 360;
		else if (max == g)
			h = (60 * (b - r) / (max - min)) + 120;
		else if (max == b)
			h = (60 * (r - g) / (max - min)) + 240;

		//  Calculate the Luminance

		float l = (max + min) / 2;
		//System.out.println(max + " : " + min + " : " + l);

		//  Calculate the Saturation

		float s;

		if (max == min)
			s = 0;
		else if (l <= .5f)
			s = (max - min) / (max + min);
		else
			s = (max - min) / (2 - max - min);

		return String.format("%03d, %03d, %03d", Math.round(h), Math.round(s * 100), Math.round(l * 100));
	}

	private static class Listener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1) return;

			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			switch (copyAs) {
				case HEX:
					clipboard.setContents(new StringSelection(
							asHex()
					), null);
					break;
				case RGB:
					clipboard.setContents(new StringSelection(
							asRGB()
					), null);
					break;
				case HSV:
					clipboard.setContents(new StringSelection(
							asHSV()
					), null);
					break;
				case HSL:
					clipboard.setContents(new StringSelection(
							asHSL()
					), null);
					break;
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			mouseHasMoved = true;
			mouseX = e.getX();
			mouseY = e.getY();
		}
	}

	/**
	 * The axes to check when changing the color by the mouse position
	 */
	private static Axes axes = Axes.X;
	private enum Axes {
		X, Y, BOTH
	}

	private static DisplayMode displayMode = DisplayMode.HEX;
	private enum DisplayMode {
		HEX, RGB, HSV, HSL
	}

	private static CopyAs copyAs = CopyAs.HEX;
	private enum CopyAs {
		HEX, RGB, HSV, HSL
	}

	private static JFrame frame;
	private static JLabel colourText;
	private static JMenuBar settings;

	public static void main(String[] argv) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}

			frame = new JFrame("Color chooser");
			frame.setLayout(new GridBagLayout());
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().setBackground(Color.BLACK);

			Listener l = new Listener();
			frame.addMouseListener(l);
			frame.addMouseMotionListener(l);

			colourText = new JLabel("#000000");
			colourText.setForeground(textColour);
			colourText.setFont(new Font("Andale Mono", Font.PLAIN, 40));

			settings = new JMenuBar();

			JMenu axesMenu = new JMenu("Axes");
			axesMenu.add(new JMenuItem("X")).addActionListener(e -> axes = Axes.X);
			axesMenu.add(new JMenuItem("Y")).addActionListener(e -> axes = Axes.Y);
			axesMenu.add(new JMenuItem("Both")).addActionListener(e -> axes = Axes.BOTH);
			settings.add(axesMenu);

			JMenu displayModeMenu = new JMenu("Display style");
			displayModeMenu.add(new JMenuItem("HEX")).addActionListener(e -> {
				displayMode = DisplayMode.HEX;
				colourText.setText(asHex());
			});
			displayModeMenu.add(new JMenuItem("RGB")).addActionListener(e -> {
				displayMode = DisplayMode.RGB;
				colourText.setText(asRGB());
			});
			displayModeMenu.add(new JMenuItem("HSB/HSV")).addActionListener(e -> {
				displayMode = DisplayMode.HSV;
				colourText.setText(asHSV());
			});
			displayModeMenu.add(new JMenuItem("HSL")).addActionListener(e -> {
				displayMode = DisplayMode.HSL;
				colourText.setText(asHSL());
			});
			settings.add(displayModeMenu);

			JMenu copyAsMenu = new JMenu("Copy style");
			copyAsMenu.add(new JMenuItem("HEX")).addActionListener(e -> copyAs = CopyAs.HEX);
			copyAsMenu.add(new JMenuItem("RGB")).addActionListener(e -> copyAs = CopyAs.RGB);
			copyAsMenu.add(new JMenuItem("HSB/HSV")).addActionListener(e -> copyAs = CopyAs.HSV);
			copyAsMenu.add(new JMenuItem("HSL")).addActionListener(e -> copyAs = CopyAs.HSL);
			settings.add(copyAsMenu);

			frame.add(colourText);
			frame.setJMenuBar(settings);

			frame.setResizable(true);
			frame.pack();
			frame.setVisible(true);
		});

		while (true) {
			if (mouseHasMoved) {
				double mousePos;
				switch (axes) {
					case X:
						mousePos = (double) mouseX / frame.getWidth();
						break;
					case BOTH:
						mousePos = (double) mouseX / (frame.getWidth() * frame.getHeight());
						mousePos += (double) mouseY / frame.getHeight(); // (Simplified from (Y * width) / (width * height))
						break;
					case Y:
						mousePos = (double) mouseY / frame.getHeight();
						break;
					default:
						throw new Error("Get your java fixed");
				}
				colour = new Color((int) (mousePos * 0xffffff));

				switch (displayMode) {
					case HEX:
						colourText.setText(asHex());
						break;
					case RGB:
						colourText.setText(asRGB());
						break;
					case HSV:
						colourText.setText(asHSV());
						break;
					case HSL:
						colourText.setText(asHSL());
						break;
				}

				mouseHasMoved = false;
			}

			float[] fromRGB = fadeFrom.getRGBColorComponents(null);
			float[] toRGB = colour.getRGBColorComponents(null);

			fadeFrom = new Color((fromRGB[0] * 49 + toRGB[0]) / 50, (fromRGB[1] * 49 + toRGB[1]) / 50, (fromRGB[2] * 49 + toRGB[2]) / 50);
			frame.getContentPane().setBackground(fadeFrom);

			TimeUnit.MILLISECONDS.sleep(10);
		}
	}
}
