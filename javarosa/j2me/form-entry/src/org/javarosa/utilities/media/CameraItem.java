/**
 *
 */
package org.javarosa.utilities.media;

import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.j2me.view.J2MEDisplay;

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VideoControl;

import de.enough.polish.ui.CustomItem;
import de.enough.polish.ui.Item;
import de.enough.polish.ui.Style;

/**
 * @author ctsims
 *
 */
public class CameraItem extends CustomItem {

    int width, height;
    int pw, ph, cw, ch;
    int vw, vh;
    boolean started = false;

    Reference videoRef;

    VideoControl vc;

       private Player player;
       void defplayer() throws MediaException {
              if (player != null) {
                 if(player.getState() == Player.STARTED) {
                    player.stop();
                 }
                 if(player.getState() == Player.PREFETCHED) {
                    player.deallocate();
                 }
                 if(player.getState() == Player.REALIZED ||
                    player.getState() == Player.UNREALIZED) {
                    player.close();
                 }
              }
              player = null;
           }



    public CameraItem() throws MediaException, IOException, InvalidReferenceException {
        this(null);
    }

    public CameraItem(Style style) throws MediaException, IOException, InvalidReferenceException {
        super(null, style);

        this.appearanceMode = Item.PLAIN;

        defplayer();
        // create a player instance
        try {
            player = Manager.createPlayer("capture://image");
        } catch(MediaException me) {
            player = Manager.createPlayer("capture://video");
        }

        player.addPlayerListener(new PlayerListener() {

            public void playerUpdate(Player arg0, String event, Object arg2) {
                  if(event == PlayerListener.END_OF_MEDIA) {
                      //Anything?
                      }

                  boolean invalidate = false;
                  if(event == PlayerListener.STOPPED) {
                      if(playing) {
                          invalidate = true;
                      }
                      playing = false;
                      if(vc != null) {
                          vc.setVisible(false);
                      }
                  } else if (event== PlayerListener.STARTED) {
                      if(!playing) {
                          invalidate = true;
                      }
                      playing = true;
                      if(vc != null) {
                          vc.setVisible(true);
                      }
                  }
                  if(invalidate) {
                      CameraItem.this.repaintFully();
                  }
            }

        });

        // realize the player
        player.realize();
        vc = (VideoControl)player.getControl("VideoControl");

        if(vc == null) {
            //ERROR!
            throw new RuntimeException("NO Video!");
        }
            vc.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, J2MEDisplay.getDisplay());

            pw = vc.getSourceWidth();
            ph = vc.getSourceHeight();

            vw = vc.getSourceWidth();
            vh = vc.getSourceHeight();

            //The formats involved in video scale somewhat oddly (3gp specifically), and don't actually give
            //the right values here. We really want to scale if possible.

            int optimal = J2MEDisplay.getScreenWidth(240) * 95 / 100;

            //We'd optimally like to be around 3/4 of the screen's width, if available.

            //if(optimal > pw) {
                //we can only scale up to 2x

                double scale = Math.min(2.0, (optimal * 1.0 / pw));

                //228 186
                pw = (int)Math.floor(pw * scale);
                ph = (int)Math.floor(ph * scale);
            //}

        player.prefetch();

    }


    boolean playing = false;
    protected void paintContent(int x, int y, int leftBorder, int rightBorder, Graphics g) {
            if(player != null) {
                //center stuff
                width = rightBorder - leftBorder;
                int offsetX = (width - cw) / 2;
                vc.setDisplayLocation(x + offsetX, y);
                if(!playing) {
                    vc.setVisible(false);

                    Font f = g.getFont();
                    int fnth = f.getHeight();


                    //Calculate margins and locations
                    int mx = x + offsetX;
                    int my = y;

                    int mw = vc.getDisplayWidth();
                    int mh = vc.getDisplayHeight();


                    int hi = Math.max((int)Math.floor(.2 * mh), fnth);
                    int fh = mh - hi * 2;

                    //int fw = mw  - wi * 2;
                    //int wi = (int)Math.floor(.2 * mw);
                    int fw = (int)Math.floor(.9 * fh);

                    int wi = (mw - fw)/2;


                    int wu = (int)Math.floor(fw / 5.0);
                    int hu = (int)Math.floor(fh / 7.0);

                    //Draw us a big 'ol hash
                    g.setColor(0, 0, 0);

                    g.fillRect(mx + wi + wu, my + hi, wu, fh);
                    g.fillRect(mx + wi + 3 * wu, my + hi, wu, fh);

                    g.fillRect(mx + wi, my + hi + 2 * hu, fw, hu);
                    g.fillRect(mx + wi, my + hi + 4 * hu, fw, hu);


                    String top = Localization.get("video.playback.top");
                    //String top = "lw: " + vc.getSourceWidth() + " lh: " + vc.getSourceHeight();
                    String bottom = Localization.get("video.playback.bottom");
                    //String bottom = "cw: " + cw + " ch: " + ch;


                    int tw = f.stringWidth(top);
                    int bw = f.stringWidth(bottom);

                    int tx = (mw - tw)/2 + mx;
                    int tyo = (hi - fnth) /2;
                    int ty = my + tyo;


                    g.drawString(top, tx, ty, Graphics.TOP | Graphics.LEFT);

                    int bx = (mw - bw)/2 + mx;
                    int by = (my + mh - hi) + tyo;

                    g.drawString(bottom, bx, by, Graphics.TOP | Graphics.LEFT);
                }
            }
    }

    public int getPreferredWidth() {
        return pw;
    }

    public int getPreferredHeight() {
        return ph;
    }


    protected int getMinContentWidth() {
        return 0;
    }

    protected void start() {
        try {
            player.start();
        } catch (MediaException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        }
    }


    protected int getMinContentHeight() {
        return 0;
    }



    private int availHeight;
    protected int getPrefContentWidth(int height) {
        availHeight = ph;
        return pw;
    }

    int aw;
    int ah;

    protected int getPrefContentHeight(int availWidth) {
        int h = ph;
        int w = pw;

        aw = availWidth;
        ah = availHeight;

            if(h > availHeight) {
                double ratio = availHeight  / (h * 1.0);
                h = availHeight;
                w = (int)Math.floor(w * ratio);
            }

            if(w > availWidth) {
                double ratio = availWidth  / (w * 1.0);
              w = availWidth;
              h = (int)Math.floor(h * ratio);
            }

          //try to get a clean scale if you can
          int numTries = 10;
          int curPw = w % 2 == 1 ? w + 1: w;
          for(int i = 0; i < numTries ; ++i) {
                double curScale = Math.min(2.0, (curPw*1.0) / vw );

                double resultHeight = vh * curScale;
                double floored = Math.floor(resultHeight);
                if(resultHeight == floored && (int)floored % 2 == 0 && (int)floored <= h) {
                    w  = curPw;
                    h = (int)floored;
                    break;
                }

                curPw -= 2;

                i++;
          }

            cw = w;
            ch = h;

            try {
            vc.setDisplaySize(cw, ch);
        } catch (MediaException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


            vc.setVisible(true);

        return ph;
    }



    protected void paint(Graphics g, int w, int h) {
        width = w;
        height = h;
    }

    public Player getPlayer() {
        return player;
    }

    public void releaseResources() {
        super.releaseResources();
        try {
            defplayer();
        } catch (MediaException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public IDataPointer snap() {
        try {
            //byte[] imageData = vc.getSnapshot("encoding=jpeg&quality=100&width=" + width + "&height=" + height);
            byte[] imageData = vc.getSnapshot(null);
            player.stop();

            String fileName = "test" + System.currentTimeMillis() + ".jpg";

            Reference destination = ReferenceManager._().DeriveReference("jr://file/" + fileName);
            OutputStream os = destination.getOutputStream();
            try {
                StreamsUtil.writeToOutput(imageData, os);
            } finally {
                try {
                    os.close();
                } catch(IOException e){

                }
            }
            ReferenceDataPointer fdp = new ReferenceDataPointer(destination.getURI());

        return fdp;
        } catch(SecurityException se){
            return null;
        } catch(Exception e) {
            Logger.die("Camera", e);
            return null;
        }
    }

}
