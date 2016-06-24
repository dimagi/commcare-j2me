package org.javarosa.j2me.util.media;

import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class ImageUtils {


    private static boolean IMAGE_DEBUG_MODE = false;
    public static Image getImage(String URI){

        //We need to make sure we have memory available if it exists, because we're going to be allocating huuuge
        //chunks, and that might fail even if those chunks would be available if we collected.
        Runtime.getRuntime().gc();
        if(URI != null && !URI.equals("")){
            InputStream is = null;
            try {
                Reference ref = ReferenceManager._().DeriveReference(URI);
                is = ref.getStream();
                Image i = Image.createImage(is);
                return i;
            } catch (IOException e) {
                System.out.println("IOException for URI:"+URI);
                e.printStackTrace();
                if(IMAGE_DEBUG_MODE) throw new RuntimeException("ERROR! Cant find image at URI: "+URI);
                return null;
            } catch (InvalidReferenceException ire){
                System.out.println("Invalid Reference Exception for URI:"+URI);
                ire.printStackTrace();
                if(IMAGE_DEBUG_MODE) throw new RuntimeException("Invalid Reference for image at: " +URI);
                return null;
            } catch(OutOfMemoryError oome) {
                if(IMAGE_DEBUG_MODE) { throw new RuntimeException("ERROR! Not enough memory to load image: "+URI); }
                else {
                    Logger.log("OOM", "Loading image: " + URI );
                    return null;
                }
            } finally {
                try{
                    if(is != null) {
                        is.close();
                    }
                } catch(IOException e) {
                    //These are the dumbest blocks...
                }
            }
        } else{
            return null;
        }
    }


    /**
      * This methog resizes an image by resampling its pixels
      * @param src The image to be resized
      * @return The resized image
      */

      public static Image resizeImage(Image src, int newWidth, int newHeight) {
            //We need to make sure we have memory available if it exists, because we're going to be allocating huuuge
           //chunks, and that might fail even if those chunks would be available if we collected.
          Runtime.getRuntime().gc();

          int srcWidth = src.getWidth();
          int srcHeight = src.getHeight();
          Image tmp = Image.createImage(newWidth, srcHeight);
          Graphics g = tmp.getGraphics();
          int ratio = (srcWidth << 16) / newWidth;
          int pos = ratio/2;

          //Horizontal Resize

          for (int x = 0; x < newWidth; x++) {
              g.setClip(x, 0, 1, srcHeight);
              g.drawImage(src, x - (pos >> 16), 0, Graphics.LEFT | Graphics.TOP);
              pos += ratio;
          }

          Image resizedImage = Image.createImage(newWidth, newHeight);
          g = resizedImage.getGraphics();
          ratio = (srcHeight << 16) / newHeight;
          pos = ratio/2;

          //Vertical resize

          for (int y = 0; y < newHeight; y++) {
              g.setClip(0, y, newWidth, 1);
              g.drawImage(tmp, 0, y - (pos >> 16), Graphics.LEFT | Graphics.TOP);
              pos += ratio;
          }
          return resizedImage;

      }//resize image

      /**
       * Used for scaling an image.  Checks to see if an image is bigger than the
       * provided dimensions, and provides new dimensions such that the image
       * scales to fit within the dimensions given. If the image is smaller (in both width and height)
       * than the given dimensions, returns the original image dimensions.
       * @param source image
       * @return int array [height, width]
       */
      public static int[] getNewDimensions(Image im, int height, int width){
        double scalef = im.getHeight()*1.0/im.getWidth();
        int w = 1;
        int h = 1;
        if(im.getHeight() > height && im.getWidth() <= width){ //height is overbounds
            h = height;
            w = (int)Math.floor(h/scalef);
        }else if (im.getHeight() <= height && im.getWidth() > width){  //width is overbouds
            w = width;
            h = (int)Math.floor(w*scalef);
        }else if (im.getHeight() > height && im.getWidth() > width){ //both are overbounds
            if(height > width){    //screen width is smaller dimension, so reduce im width and scale height
                w = width;
                h = (int)Math.floor(w*scalef);
            }else if(height <= width){ //reduce height and scale width
                h = height;
                w = (int)Math.floor(h/scalef);
            }
        }else{
            h = im.getHeight();
            w = im.getWidth();
        }
            int[] dim = {h,w};
            return dim;
      }


}
