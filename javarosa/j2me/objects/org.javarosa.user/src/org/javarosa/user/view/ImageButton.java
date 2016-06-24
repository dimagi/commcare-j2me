package org.javarosa.user.view;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.enough.polish.ui.CustomItem;
import de.enough.polish.ui.Style;

class ImageButton extends CustomItem {

    private Image _image = null;
    private boolean _down = false;
    private int _clicks = 0;

    public ImageButton(Image image) {
        this(image, null);
    }

    public ImageButton(Image image, Style s) {
        super("", s);
        _image = image;
    }

    public Image getImage() {
        return _image;
    }

    // Is the button currently down?
    public boolean isDown() {
        return _down;
    }

    // Minimal button size = image size
    protected int getMinContentHeight() {
        return getImage().getHeight();
    }
    protected int getMinContentWidth() {
        return getImage().getWidth();
    }
    // Preferred button size = image size + borders
    protected int getPrefContentHeight(int width) {
        return getImage().getHeight()+2;
    }
    protected int getPrefContentWidth(int height) {
        return getImage().getWidth()+2;
    }

    // Button painting procedure
    protected void paint(Graphics g, int w, int h) {
        // Fill the button with grey color - background
        g.setColor(0, 0, 0);
        g.fillRect(0, 0, w, h);
        // Draw the image in the center of the button
        g.drawImage(getImage(), w/2, h/2, Graphics.HCENTER|Graphics.VCENTER);
        // Draw the borders
        g.setColor(0x000000);
        g.drawLine(0, 0, w, 0);
        g.drawLine(0, 0, 0, h);
        g.drawLine(0, h-1, w, h-1);
        g.drawLine(w-1, 0, w-1, h);
    }
}