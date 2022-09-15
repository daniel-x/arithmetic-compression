package arithmetic_compression.ui.desktop;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

@SuppressWarnings("serial")
public class FullScreenWindow extends Window implements KeyListener, WindowListener, MouseListener, ComponentListener {

	/**
	 * Dummy; Only for convincing the jdk-class-lib to allow creating windows
	 * without a parent window or parent frame.
	 *
	 * @since 2000-07-19
	 */
	protected static Frame helperFrame = new Frame();

	public FullScreenWindow() {
		super(helperFrame);

		addWindowListener(this);
		addKeyListener(this);
		helperFrame.addKeyListener(this);
		addMouseListener(this);
		addComponentListener(this);

		ensureBounds();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println(KeyEvent.VK_ESCAPE);
		System.out.println(e.getKeyCode());
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		System.out.println(KeyEvent.VK_ESCAPE);
		System.out.println(e.getKeyCode());

		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			windowClosing(null);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		windowClosing(null);
	}

	@Override
	public void windowClosing(WindowEvent e) {
		System.exit(0);
	}

	/**
	 * Corrects this <code>FullScreenWindow</code>'s bounds if necessary.
	 *
	 * @see #ensureSize
	 * @see #ensureLocation
	 *
	 * @since 2000-07-19
	 */
	public void ensureBounds() {
		ensureLocation();
		ensureSize();
	}

	/**
	 * Corrects this <code>FullScreenWindow</code>'s location if necessary.
	 *
	 * @see #ensureBounds
	 * @see #ensureSize
	 *
	 * @since 2000-07-19
	 */
	public void ensureLocation() {
		if (getX() != 0 || getY() != 0)
			setLocation(0, 0);
	}

	/**
	 * Corrects this <code>FullScreenWindow</code>'s size if necessary.
	 *
	 * @see #ensureBounds
	 * @see #ensureLocation
	 *
	 * @since 2000-07-19
	 */
	public void ensureSize() {
		Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
		// Dimension scrSize = new Dimension(720, 405);

		if (getWidth() != scrSize.width || getHeight() != scrSize.height) {
			setSize(scrSize);
		}
	}

	/**
	 * Shows or hides this window, depending on the specified boolean parameter.
	 *
	 * @param visibility
	 *            If this is true, this window will be set visible, moved to
	 *            front, and activated. Otherwise (i.e. if this parameter if
	 *            false), this window will simply be set invisible.
	 *
	 * @see petunotop.ui.util.Misc#setVisibleWindow
	 * @see java.awt.Window#setVisible(boolean)
	 *
	 * @since 2000-07-19
	 */
	public void setVisible(boolean visibility) {
		if (visibility) {
			helperFrame.toFront();
			helperFrame.requestFocus();

			toFront();
			requestFocus();
		}

		super.setVisible(visibility);
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		ensureLocation();
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		ensureSize();
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
	}
}
