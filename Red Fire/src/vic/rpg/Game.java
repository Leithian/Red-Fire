package vic.rpg;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.UIManager;

import vic.rpg.client.net.NetHandler;
import vic.rpg.client.packet.PacketHandlerSP;
import vic.rpg.config.Options;
import vic.rpg.gui.Gui;
import vic.rpg.gui.GuiIngame;
import vic.rpg.gui.GuiMain;
import vic.rpg.level.Level;
import vic.rpg.level.entity.living.EntityPlayer;
import vic.rpg.registry.GameRegistry;
import vic.rpg.registry.RenderRegistry;
import vic.rpg.render.DrawUtils;
import vic.rpg.render.Screen;
import vic.rpg.render.TextureLoader;
import vic.rpg.server.Server;
import vic.rpg.utils.Utils;
import vic.rpg.utils.Utils.Side;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.Screenshot;

public class Game extends GLCanvas implements Runnable 
{
	public static String USERNAME = "victorious3";
	
	public static JFrame frame = new JFrame();

	public static final int HEIGHT = 600;
	public static final int WIDTH = 800;
	
	public static int RES_HEIGHT = 600;
	public static int RES_WIDTH = 800;

	public static GLProfile GL_PROFILE;
    public static NetHandler netHandler = new NetHandler();
	public static PacketHandlerSP packetHandler = new PacketHandlerSP();
	public static Game game;	
	public Screen screen;
	
	private Thread thread;
	public Animator GL_ANIMATOR;
	public boolean isRunning = false;
	
	public static boolean TAKE_SCREENSHOT = false;
	private static boolean isUpdating = false;
	
	//Game Objects
	public static String playerUUID;
	public static Level level;
	
	public static EntityPlayer getPlayer()
	{
		if(Game.level != null && playerUUID != null)
		{
			return (EntityPlayer) Game.level.entityMap.get(playerUUID);
		}
		return null;
	}
	
    public Game(GLCapabilities glcapabilities)
    {
    	super(glcapabilities);
    	RenderRegistry.bufferImages();
        RenderRegistry.setup();  
        Options.load();	 	
    }
    
    public synchronized void stopGame()
    {
    	System.out.println("Stopping client...");
    	Options.safe();
    	
    	if(Utils.getSide() == Side.CLIENT)
		{
			RenderRegistry.stop();
		}
    	
    	System.out.println("Client Stopped! Thanks for using our software! (V3.inc)");
    	System.exit(0);
    }
    
    public static void init()
    {   	
		List<Class<?>> cls;
		try {
			cls = ClassFinder.getClasses("vic.rpg", (String)null);
			for(Class<?> c : cls)
			{
				for(Method m : c.getDeclaredMethods())
				{
					if(m.getAnnotation(Init.class) != null && Modifier.isStatic(m.getModifiers()))
					{
						Init init = m.getAnnotation(Init.class);
						if(init.side() == Side.CLIENT || init.side() == Side.BOTH)
						{
							m.setAccessible(true);
							try {
								m.invoke(null, (Object[])null);
								System.out.println("init: " + c.getName() + "." + m.getName() + "()");
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			for(Class<?> c : cls)
			{
				for(Method m : c.getDeclaredMethods())
				{
					if(m.getAnnotation(PostInit.class) != null && Modifier.isStatic(m.getModifiers()))
					{
						PostInit init = m.getAnnotation(PostInit.class);
						if(init.side() == Side.CLIENT || init.side() == Side.BOTH)
						{
							m.setAccessible(true);
							try {
								m.invoke(null, (Object[])null);
								System.out.println("postinit: " + c.getName() + "." + m.getName() + "()");
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}	
    }
  
    private void init(GL2 gl2)
    {
    	init();
    	
    	gl2.glEnable(GL2.GL_ALPHA_TEST);
    	gl2.glAlphaFunc(GL2.GL_GREATER, 0.1F);
    	gl2.glEnable(GL2.GL_BLEND);
    	gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
    	gl2.glDisable(GL2.GL_DEPTH_TEST);
    	
    	gl2.glMatrixMode(GL2.GL_PROJECTION);
    	gl2.glLoadIdentity();
    	gl2.glViewport(0, 0, WIDTH, HEIGHT);
    	gl2.glOrtho(0, WIDTH, HEIGHT, 0, -1, 1);
    	gl2.glMatrixMode(GL2.GL_MODELVIEW);
    	
    	screen = new Screen(WIDTH, HEIGHT);
    	screen.init(gl2);
		Gui.setGui(new GuiMain());
		start();
		
		GL_ANIMATOR = new Animator();
		GL_ANIMATOR.add(this);
		GL_ANIMATOR.setUpdateFPSFrames(10, null);
		GL_ANIMATOR.start();
    }
    
    private synchronized void render(GL2 gl2)
    {
    	if(isUpdating)
    	{
    		try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	
    	gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    	DrawUtils.setGL(gl2);
    	screen.render(gl2);
    	screen.postRender(gl2);
    	gl2.glFlush();
    	
    	notify();
    }
    
	private synchronized void tick() 
	{
		isUpdating = true;
		screen.tick();
		
		if(Gui.currentGui != null)
        {
			Gui.currentGui.updateGui();
        }
		if(level != null)
		{
			level.tick();
		}	
		isUpdating = false;
	}
	
    public void start()
	{
		if(isRunning) return;
		isRunning = true;
		thread = new Thread(this);
		thread.setName("Game");
		thread.start();
	}
	
	public void stop()
	{
		if(!isRunning) return;
		isRunning = false;
		try {
			thread.join();		
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static void main(String[] args)
	{		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.setOut(new PrintStream(System.out)
		{
			@Override public void println(){ super.println(Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:");}
			@Override public void println(boolean x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(char x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(char[] x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x.toString());}
			@Override public void println(double x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(float x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(int x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(long x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(Object x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(String x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}	
		});
		
		//TODO destroys error messages :3
		/*System.setErr(new PrintStream(System.err)
		{
			@Override public void println(){ super.println(Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:");}
			@Override public void println(boolean x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(char x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(char[] x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x.toString());}
			@Override public void println(double x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(float x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(int x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(long x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(Object x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}
			@Override public void println(String x){ super.println((Utils.getSide() == Side.CLIENT ? "[CLIENT]:" : "[SERVER]:") + x);}	
		});*/
		
		System.out.println("Welcome to the RedFire alpha! Feel free to redistribute this software in binary form.");
		System.out.println("Please report bugs to our gitHub page at https://github.com/Victorious3/Red-Fire/");
		System.out.println("WARNING: Threre is no guaranty that this software will run as intended on your PC. Use it on your own risk!");
		
		GL_PROFILE = GLProfile.getDefault();
        GLCapabilities glcapabilities = new GLCapabilities(GL_PROFILE);
		
        game = new Game(glcapabilities);
        System.out.println(glcapabilities.toString());
        game.setSize(WIDTH, HEIGHT);
        game.addGLEventListener(new GLEventListener() 
        {		
			@Override
			public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
			
			@Override public void init(GLAutoDrawable drawable) 
			{
				game.init(drawable.getGL().getGL2());
			}	
			
			@Override public void dispose(GLAutoDrawable drawable) {}
			
			@Override
			public void display(GLAutoDrawable drawable) 
			{
				TextureLoader.setupTextures(drawable.getGL().getGL2());
				if(TAKE_SCREENSHOT)
				{
					BufferedImage screenshot = Screenshot.readToBufferedImage(Game.RES_WIDTH, Game.RES_HEIGHT);
					Date date = new Date();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-d_HH.mm.ss");
					File file = Utils.getOrCreateFile(Utils.getAppdata() + "/screenshots/" + df.format(date) + ".jpg");
					
					try {
						ImageIO.write(screenshot, "jpg", file);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					if(Gui.currentGui instanceof GuiIngame)
					{
						((GuiIngame) Gui.currentGui).addChatMessage("Screenshot saved to " + file.getName(), "CLIENT");
					}
					TAKE_SCREENSHOT = false;
				}
				game.render(drawable.getGL().getGL2());
			}
		});
		game.addMouseListener(GameRegistry.mouse);
		game.addMouseMotionListener(GameRegistry.mouse);
		game.addMouseWheelListener(GameRegistry.mouse);
		game.addKeyListener(GameRegistry.key);
		game.addComponentListener(GameRegistry.window);
		
		frame.add(game);
		frame.setTitle("Red Fire Alpha " + GameRegistry.VERSION);
		
		frame.addWindowListener(new WindowListener() 
		{
			@Override
			public void windowActivated(WindowEvent e) {}

			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) 
			{
				if(netHandler.IS_SINGLEPLAYER) Server.server.inputHandler.handleCommand("stop", null, Server.server);				
				Game.game.stopGame();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {}

			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}

			@Override
			public void windowOpened(WindowEvent e) {}	      
			
		});
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);		
		game.requestFocus();
		
		if(args.length > 0 && args[0] != null)
		{
			Game.USERNAME = args[0];
		}
		else Game.USERNAME = String.valueOf(new Random().nextLong());	
	}

	@Override
	public void run() 
	{	
		double unprocessedSeconds = 0;
		long previousTime = System.nanoTime();
		double secondsPerTick = 1 / 60.0;
		int tickCount = 0;
		
		while(isRunning)
		{
			long currentTime = System.nanoTime();
			long passedTime = currentTime - previousTime;
			previousTime = currentTime;
			unprocessedSeconds += passedTime / 1000000000.0;
			
			while(unprocessedSeconds > secondsPerTick)
			{
				tick();
				unprocessedSeconds -= secondsPerTick;
				tickCount++;
				if(tickCount % 60 == 0)
				{
					previousTime += 1000;
				}
			}
		}		
	}
}