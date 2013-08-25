package vic.rpg.editor.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.lang.reflect.Field;
import java.util.Vector;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.table.DefaultTableModel;

import vic.rpg.editor.Editor;
import vic.rpg.editor.listener.Key;
import vic.rpg.editor.listener.Mouse;
import vic.rpg.editor.listener.ZoomListener;
import vic.rpg.level.Editable;
import vic.rpg.level.Entity;
import vic.rpg.level.Level;
import vic.rpg.level.path.Node;
import vic.rpg.level.path.NodeMap;
import vic.rpg.render.DrawUtils;
import vic.rpg.render.TextureLoader;

import com.jogamp.opengl.util.Animator;

public class LabelLevel extends GLJPanel
{
	private int needsUpdate = 0;
	private float scale = 1;
	
	private int offX = 0;
	private int offY = 0;
	private int width = 0;
	private int height = 0;
	
	public LabelLevel(GLCapabilities glCapabilities)
	{
		super(glCapabilities);
		
		this.addGLEventListener(new GLEventListener() 
		{		
			@Override public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
			
			@Override
			public void init(GLAutoDrawable drawable) 
			{
				GL2 gl2 = drawable.getGL().getGL2();
				gl2.glEnable(GL2.GL_ALPHA_TEST);
		    	gl2.glAlphaFunc(GL2.GL_GREATER, 0.1F);
		    	gl2.glEnable(GL2.GL_BLEND);
		    	gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		    	gl2.glDisable(GL2.GL_DEPTH_TEST);
	
				Animator animator = new Animator();
				animator.add(drawable);
				animator.start();
			}
			
			@Override public void dispose(GLAutoDrawable drawable) {}
			
			@Override
			public void display(GLAutoDrawable drawable) 
			{
				GL2 gl2 = drawable.getGL().getGL2();
				gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
				
				if(Editor.instance.level != null)
				{
					gl2.glMatrixMode(GL2.GL_PROJECTION);
			    	gl2.glLoadIdentity();
			    	gl2.glViewport(0, 0, (int)(Editor.instance.level.width * Level.CELL_SIZE * scale), (int)(Editor.instance.level.height * Level.CELL_SIZE * scale));
			    	gl2.glOrtho(0, (int)(Editor.instance.level.width * Level.CELL_SIZE * scale), (int)(Editor.instance.level.height * Level.CELL_SIZE * scale), 0, -1, 1);
			    	gl2.glMatrixMode(GL2.GL_MODELVIEW);
				}
				
				gl2.glPushMatrix();
				gl2.glLoadIdentity();
				gl2.glScalef(scale, scale, scale);
				
		    	DrawUtils.setGL(gl2);
		    	TextureLoader.setupTextures(gl2);
				if(Editor.instance.level != null)
				{
					Editor.instance.level.render(gl2, 0, 0, Editor.instance.level.width * Level.CELL_SIZE, Editor.instance.level.height * Level.CELL_SIZE, 0, 0);
				}
				gl2.glPopMatrix();
				gl2.glFlush();
			}
		});
	}
	
	@Override
	public void paintComponent(Graphics g) 
	{		
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		
		g2d.scale(scale, scale);
		
		if(Editor.instance == null) return;
				
		if(Mouse.selectedEntities != null)
		{
			for(Entity e : Mouse.selectedEntities)
			{
				g2d.setColor(Color.yellow);
				Stroke stroke = g2d.getStroke();
				g2d.setStroke(new BasicStroke(5));
				g2d.drawRect(e.xCoord, e.yCoord, e.getWidth(), e.getHeight());
				g2d.setStroke(stroke);
			}
		}
		if(Mouse.selectedTiles != null)
		{
			for(Point p : Mouse.selectedTiles)
			{
				g2d.setColor(Color.yellow);
				Stroke stroke = g2d.getStroke();
				g2d.setStroke(new BasicStroke(5));
				g2d.drawRect(p.x * Level.CELL_SIZE, p.y * Level.CELL_SIZE, Level.CELL_SIZE, Level.CELL_SIZE);
				g2d.setStroke(stroke);
			}
		}		
		if(Mouse.selection != null)
		{
			g2d.setColor(new Color(0, 100, 255, 100));
			g2d.fill(Mouse.selection);
			
			g2d.setColor(new Color(0, 0, 0, 100));
			Stroke stroke = g2d.getStroke();
			g2d.setStroke(new BasicStroke(1 / getScale() * 2));
			g2d.draw(Mouse.selection);
			g2d.setStroke(stroke);
		}		
		if(Editor.instance.buttonPath.isSelected() || Key.keyListener.button == 3)
		{
			if(Editor.instance.level != null)
			{
				g2d.setColor(new Color(255, 0, 0, 120));
				for(Node[] n2 : Editor.instance.level.nodeMap.nodes)
				{
					for(Node n : n2)
					{
						if(n.isBlocked)
						{
							Point p = n.toPoint();					
							g2d.fillRect(p.x, p.y, Level.CELL_SIZE, Level.CELL_SIZE);
						}
					}
				}
				
				g2d.setColor(new Color(255, 255, 0));
				for(Entity e : Editor.instance.level.entities.values())
				{
					g2d.draw(e.getCollisionBoxes(new Area()));
				}
				
				g2d.setColor(new Color(0, 255, 0, 120));
				if(Mouse.start != null) g2d.fillRect(Mouse.start.x * Level.CELL_SIZE, Mouse.start.y * Level.CELL_SIZE, Level.CELL_SIZE, Level.CELL_SIZE);
				g2d.setColor(new Color(0, 255, 255, 120));
				if(Mouse.end != null) g2d.fillRect(Mouse.end.x * Level.CELL_SIZE, Mouse.end.y * Level.CELL_SIZE, Level.CELL_SIZE, Level.CELL_SIZE);
				g2d.setColor(new Color(0, 0, 255, 120));
				
				if(Mouse.path != null)
				{
					while(Mouse.path.hasNext())
					{
						Node n = Mouse.path.next();
						g2d.fillRect(n.x * Level.CELL_SIZE, n.y * Level.CELL_SIZE, Level.CELL_SIZE, Level.CELL_SIZE);
					}
					Mouse.path.revert();
				}
			}
		}		
		needsUpdate = 0;
	}
	
	public void update(boolean onlySelection)
	{
		this.needsUpdate = onlySelection ? 3 : 1;
		this.updateUI();
	}
	
	public void update(int xOffset, int yOffset, int width, int height)
	{
		this.needsUpdate = 2;
		
		this.offX = xOffset;
		this.offY = yOffset;
		this.width = width;
		this.height = height;
		
		this.updateUI();
	}
	
	public void scale(float scale)
	{
		if(Editor.instance.level != null)
		{
			if(scale >= 0.1 && scale <= 5) this.scale = scale;
			this.setSize((int)(Editor.instance.level.getWidth() * this.scale), (int)(Editor.instance.level.getHeight() * this.scale));
			ZoomListener.setZoom(Editor.instance.dropdownZoom, this.scale);
			this.updateUI();
		}
	}
	
	public float getScale()
	{
		return scale;
	}
	
	public void setLevel(Level level)
	{	
		Editor.instance.level = level;
		
		DefaultTableModel tableModel = (DefaultTableModel) Editor.instance.tableLevel.getModel();
		tableModel.setRowCount(0);
		
		for(Field f : level.getClass().getDeclaredFields())
		{
			if(f.getAnnotation(Editable.class) != null)
			{
				try {
					Vector<Object> v = new Vector<Object>();
					
					v.add(f.getName());
					v.add(f.getGenericType() instanceof Class ? ((Class<?>)f.getGenericType()).getSimpleName() : f.getGenericType());
					v.add(f.get(level));
					
					tableModel.addRow(v);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}				
		
		Mouse.selectedEntities.clear();
		Mouse.selectedTiles.clear();
		Mouse.selection = null;
		
		Editor.instance.level.nodeMap = new NodeMap(Editor.instance.level);
		
		scale(1);
		update(false);
	}
}
