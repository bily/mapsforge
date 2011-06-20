package org.mapsforge.core.graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Stack;

import org.mapsforge.core.graphics.Paint.Align;
import org.mapsforge.core.graphics.Paint.FontInfo;
import org.mapsforge.core.graphics.Paint.Style;

public class Canvas extends java.awt.Canvas {

	BufferedImage mBufferedImage;
	Stack<Graphics2D> graphics = new Stack<Graphics2D>();
	
	private static final long serialVersionUID = 5085355825188623626L;

	
    public Canvas() {
    	super();
    }


	public Canvas(Bitmap bitmap) {
		super();
		mBufferedImage = bitmap.getImage();
		graphics.push(mBufferedImage.createGraphics());
	}


	public void drawText(String text, float x, float y, Paint paint) {
		Graphics2D g = getGraphics2D();
		g = (Graphics2D) g.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(new Color(paint.getColor()));
		int alpha = paint.getAlpha();
		float falpha = alpha / 255.f;
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, falpha));
		if(paint.getTextAlign() != Align.LEFT) {
			float m = paint.measureText(text.toCharArray(), 0, text.length());
			if(paint.getTextAlign() == Align.CENTER) {
				x -= m / 2;
			} else if(paint.getTextAlign() == Align.RIGHT) {
				x -= m;
			}
			List<FontInfo> fonts = paint.getFonts();
			if(fonts.size() > 0) {
				FontInfo mainFont = fonts.get(0);
				int i = 0;
				int lastIndex = 0 + text.length();
				while(i < lastIndex) {
					int upTo = mainFont.mFont.canDisplayUpTo(text.toCharArray(), i, lastIndex);
					if(upTo == -1) {
						g.setFont(mainFont.mFont);
						g.drawChars(text.toCharArray(), i, lastIndex - i, (int) x, (int) y);
						return;
					} else if(upTo > 0) {
						g.setFont(mainFont.mFont);
						g.drawChars(text.toCharArray(), i, upTo - i, (int) x, (int) y);
						x += mainFont.mMetrics.charsWidth(text.toCharArray(), i, upTo - i);
						i = upTo;
					}
					boolean foundFont = false;
					for(int f = 1; f < fonts.size();f++) {
						FontInfo fontInfo = fonts.get(f);
						int charCount = Character.isHighSurrogate(text.toCharArray()[i]) ? 2 : 1;
						upTo = fontInfo.mFont.canDisplayUpTo(text.toCharArray(), i, i + charCount);
						if(upTo == -1) {
							g.setFont(fontInfo.mFont);
							g.drawChars(text.toCharArray(), i, charCount, (int) x, (int) y);
							x += fontInfo.mMetrics.charsWidth(text.toCharArray(), i, charCount);
							i += charCount;
							foundFont = true;
							break;
						}
					}
					if(!foundFont) {
						int charCount = Character.isHighSurrogate(text.toCharArray()[i]) ? 2 : 1;
						g.setFont(mainFont.mFont);
						g.drawChars(text.toCharArray(), i, charCount, (int) x, (int) y);
						x += mainFont.mMetrics.charsWidth(text.toCharArray(), i, charCount);
						i += charCount;
					}
				}
			}
			
		} 
		g.dispose();
	}
	
	/* (non-Javadoc)
     * @see android.graphics.Canvas#drawBitmap(android.graphics.Bitmap, android.graphics.Matrix, android.graphics.Paint)
     */
    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
        boolean needsRestore = false;
        if (matrix.isIdentity() == false) {
            // create a new graphics and apply the matrix to it
            save(); // this creates a new Graphics2D, and stores it for children call to use
            needsRestore = true;
            Graphics2D g = getGraphics2D(); // get the newly create Graphics2D

            // get the Graphics2D current matrix
            AffineTransform currentTx = g.getTransform();
            // get the AffineTransform from the matrix
            AffineTransform matrixTx = matrix.getTransform();

            // combine them so that the matrix is applied after.
            currentTx.preConcatenate(matrixTx);

            // give it to the graphics as a new matrix replacing all previous transform
            g.setTransform(currentTx);
        }

        // draw the bitmap
        drawBitmap(bitmap, 0, 0, paint);

        if (needsRestore) {
            // remove the new graphics
            restore();
        }
    }

	public void drawTextOnPath(String text, Path path, int i, int j, Paint paint) {
		// TODO Auto-generated method stub		
	}
	
	/* (non-Javadoc)
     * @see android.graphics.Canvas#save()
     */
    public int save() {
        // get the current save count
        int count = graphics.size();

        // create a new graphics and add it to the stack
        Graphics2D g = (Graphics2D)getGraphics2D().create();
        graphics.push(g);
        
        // return the old save count
        return count;
    }
    
    /* (non-Javadoc)
     * @see android.graphics.Canvas#restore()
     */
    public void restore() {
        graphics.pop();
    }


	public void drawLines(float[] pts, Paint paint) {
		Graphics2D g = getCustomGraphics(paint);
		for(int i = 0; i < pts.length; i += 4) {
			g.drawLine((int) pts[i + 0], (int) pts[i + 0 + 1], (int) pts[i + 0 + 2], (int) pts[i + 0 + 3]);
		}
		g.dispose();		
	}

	/* (non-Javadoc)
     * @see android.graphics.Canvas#drawPath(android.graphics.Path, android.graphics.Paint)
     */
    public void drawPath(Path path, Paint paint) {
        // get a Graphics2D object configured with the drawing parameters.
        Graphics2D g = getCustomGraphics(paint);

        Style style = paint.getStyle();

        // draw
        if (style == Style.FILL || style == Style.FILL_AND_STROKE) {
            g.fill(path.getAwtShape());
        }

        if (style == Style.STROKE || style == Style.FILL_AND_STROKE) {
            g.draw(path.getAwtShape());
        }

        // dispose Graphics2D object
        g.dispose();
    }

    /* (non-Javadoc)
     * @see android.graphics.Canvas#drawLine(float, float, float, float, android.graphics.Paint)
     */
    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        // get a Graphics2D object configured with the drawing parameters.
        Graphics2D g = getCustomGraphics(paint);

        g.drawLine((int)startX, (int)startY, (int)stopX, (int)stopY);

        // dispose Graphics2D object
        g.dispose();
    }


	public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
		BufferedImage image = bitmap.getImage();

        Graphics2D g = getGraphics2D();

        Composite c = null;

        if (paint != null) {
            if (paint.isFilterBitmap()) {
                g = (Graphics2D)g.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }

            if (paint.getAlpha() != 0xFF) {
                c = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        paint.getAlpha()/255.f));
            }
        }

        g.drawImage(image, (int) left, (int) top, (int) left+bitmap.getWidth(), (int) top+bitmap.getHeight(),
                0, 0, bitmap.getWidth(), bitmap.getHeight(), null);

        if (paint != null) {
            if (paint.isFilterBitmap()) {
                g.dispose();
            }
            if (c != null) {
                g.setComposite(c);
            }
        }		
	}

	public void setBitmap(Bitmap bitmap) {
		mBufferedImage = bitmap.getImage();
		graphics.push(mBufferedImage.createGraphics());
	}
	
	public Graphics2D getGraphics2D() {
		return graphics.peek();
	}
    
	/**
     * Creates a new {@link Graphics2D} based on the {@link Paint} parameters.
     * <p/>The object must be disposed ({@link Graphics2D#dispose()}) after being used.
     */
    private Graphics2D getCustomGraphics(Paint paint) {
        // make new one
        Graphics2D g = getGraphics2D();
        g = (Graphics2D)g.create();

        // configure it
        g.setColor(new Color(paint.getColor()));
        int alpha = paint.getAlpha();
        float falpha = alpha / 255.f;

        Style style = paint.getStyle();
        if (style == Style.STROKE || style == Style.FILL_AND_STROKE) {
            PathEffect e = paint.getPathEffect();
            if (e instanceof DashPathEffect) {
                DashPathEffect dpe = (DashPathEffect)e;
                g.setStroke(new BasicStroke(
                        paint.getStrokeWidth(),
                        paint.getStrokeCap().getJavaCap(),
                        paint.getStrokeJoin().getJavaJoin(),
                        paint.getStrokeMiter(),
                        dpe.getIntervals(),
                        dpe.getPhase()));
            } else {
                g.setStroke(new BasicStroke(
                        paint.getStrokeWidth(),
                        paint.getStrokeCap().getJavaCap(),
                        paint.getStrokeJoin().getJavaJoin(),
                        paint.getStrokeMiter()));
            }
        }

        //Xfermode xfermode = paint.getXfermode();
        //if (xfermode instanceof PorterDuffXfermode) {
           // PorterDuff.Mode mode = ((PorterDuffXfermode)xfermode).getMode();

            //setModeInGraphics(mode, g, falpha);
        //} else {
            /*if (mLogger != null && xfermode != null) {
                mLogger.warning(String.format(
                        "Xfermode '%1$s' is not supported in the Layout Editor.",
                        xfermode.getClass().getCanonicalName()));
            }*/
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, falpha));
        //}

        Shader shader = paint.getShader();
        if (shader != null) {
            java.awt.Paint shaderPaint = shader.getJavaPaint();
            if (shaderPaint != null) {
                g.setPaint(shaderPaint);
            } else {
                /*if (mLogger != null) {
                    mLogger.warning(String.format(
                            "Shader '%1$s' is not supported in the Layout Editor.",
                            shader.getClass().getCanonicalName()));
                }*/
            }
        }

        return g;
    }
	
}
