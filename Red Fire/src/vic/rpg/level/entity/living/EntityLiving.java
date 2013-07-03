package vic.rpg.level.entity.living;

import java.awt.Image;
import java.awt.geom.Area;
import java.util.HashMap;
import java.util.Map;

import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.Tag;

import vic.rpg.gui.GuiIngame;
import vic.rpg.level.Editable;
import vic.rpg.level.Entity;
import vic.rpg.server.Server;
import vic.rpg.server.packet.Packet9EntityMoving;
import vic.rpg.utils.Utils;

public class EntityLiving extends Entity
{
	public static Image sprite_unknown = Utils.readImageFromJar("/vic/rpg/resources/character/unknown.png");
	
	public Image sprite;
	@Editable public int rotation = 0;
	public Image[] sprites;
	
	protected boolean isWalking = false;
	protected float speed = 2;
	
	private int nextX;
	private int nextY;
	private boolean walk = false;
	
	protected Inventory inventory = new Inventory();
	
	public EntityLiving(int width, int height) 
	{
		super(width, height);
	}

	public void setX(int x)
	{
		this.xCoord = x;
	}
	
	@Override
	public void onMouseClicked(int x, int y, EntityPlayer entity, int mouseEvent) 
	{
		GuiIngame.focusedEntity = this;
	}

	public void walkTo(int x, int y) 
	{
		nextX = x; nextY = y;
		walk = true;
	}
	
	public void setWalking(boolean isWalking)
	{
		if(Utils.getSide().equals(Utils.SIDE_SERVER))
		{		
			Server.server.broadcast(new Packet9EntityMoving(this));
		}
		this.isWalking = isWalking;
	}
	
	public boolean isWalking()
	{
		return isWalking;
	}
	
	public Inventory getInventory()
	{
		return inventory;
	}
	
	@Override
	public void readFromNBT(CompoundTag tag) 
	{
		super.readFromNBT(tag);
		Map<String, Tag> map = tag.getValue();
		this.rotation = (int) map.get("rotation").getValue();
		inventory.readFromNBT(tag);
	}

	@Override
	public CompoundTag writeToNBT(CompoundTag tag) 
	{
		tag = super.writeToNBT(tag);
		IntTag rotation = new IntTag("rotation", this.rotation);
		Map<String, Tag> map = tag.getValue();
		Map<String, Tag> map2 = new HashMap<String, Tag>();
		map2.putAll(map);		
		map2.put("rotation", rotation);
		
		tag = new CompoundTag(tag.getName(), map2);
		tag = inventory.writeToNBT(tag);
		
		return tag;
	}

	public void setY(int y)
	{
		this.yCoord = y;
	}
	
	public void setSprite(Image image)
	{
		if(!sprite.equals(image))
		{
			setImage(sprite);
			sprite = image;
		}
	}
	
	public void setRotation(int rotation)
	{
		if(Utils.getSide().equals(Utils.SIDE_CLIENT)) this.setSprite(sprites[rotation]);
		this.rotation = rotation;
	}
	
	public void tick()
	{
		if(walk && Utils.getSide().equals(Utils.SIDE_SERVER))
		{	
			isWalking = true;
			
			if(nextX < xCoord) setX((int)(xCoord - speed));
			if(nextX > xCoord) setX((int)(xCoord + speed));
			if(nextY < yCoord) setY((int)(yCoord - speed));
			if(nextY > yCoord) setY((int)(yCoord + speed));
			
			if(nextX < xCoord) setRotation(1);
			else if(nextX > xCoord) setRotation(2);
			else if(nextY < yCoord) setRotation(3);
			else if(nextY > yCoord) setRotation(0);

			if(nextX > xCoord - speed && nextX < xCoord + speed && nextY > yCoord - speed && nextY < yCoord + speed)
			{
				xCoord = nextX; yCoord = nextY;
				walk = false; isWalking = false;
			}
			
			Server.server.broadcast(new Packet9EntityMoving(this));
		}
	}
	
	@Override
	public Area getCollisionBoxes(Area area) 
	{
		return area;
	}
	
	public Image getShortcutImage()
	{
		return sprite_unknown;
	}
}
