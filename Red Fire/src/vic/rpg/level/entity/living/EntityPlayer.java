package vic.rpg.level.entity.living;

import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

import org.jnbt.CompoundTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import vic.rpg.item.ItemApple;
import vic.rpg.item.ItemSword;
import vic.rpg.level.Editable;
import vic.rpg.render.ImageBuffer;
import vic.rpg.utils.Utils;

public class EntityPlayer extends EntityLiving 
{	
	public int step = 0;
	@Editable public String username = "NO_USERNAME";
	public boolean isWalkingBlocked = false;
	
	public static Image[][] steps = new Image[][]{ImageBuffer.getAnimatedImageData("/vic/rpg/resources/character/player_main_1.gif"), ImageBuffer.getAnimatedImageData("/vic/rpg/resources/character/player_main_2.gif"), ImageBuffer.getAnimatedImageData("/vic/rpg/resources/character/player_main_3.gif")};
	
	public EntityPlayer() 
	{
		super(33, 32);
		this.zLevel = -1;
		
		inventory.add(0, 12, 8);
		inventory.add(1);
		inventory.add(2);
		inventory.add(3);
		inventory.add(4);
		inventory.add(5);
		inventory.add(6);
		inventory.add(7);
		inventory.add(8);
		
		inventory.setStack(1, new ItemApple());
		inventory.setStack(0, new ItemSword(), 0, 0);
	}
	
	public static Image portrait = Utils.readImageFromJar("/vic/rpg/resources/character/portrait.png");
	
	@Override
	public Image getShortcutImage() 
	{
		return portrait;
	}
	
	@Override
	public String getName() 
	{
		return username;
	}

	public void reRender()
	{		
		setImage(sprite);
	}

	public int tickCounter = 0;	
	
	public void tick() 
	{	
		if(this.isWalking)
		{
			tickCounter++;
			if(tickCounter == 10)
			{				
				step++;
				if(step == 3) step = 0;
				this.sprites = steps[step];
				this.sprite = sprites[this.rotation];
				this.reRender();
				
				tickCounter = 0;	
			}
		}
	}

	@Override
	public void readFromNBT(CompoundTag tag) 
	{
		super.readFromNBT(tag);
		Map<String, Tag> map = tag.getValue();
		this.username = (String) map.get("username").getValue();
		inventory.readFromNBT(tag);
	}

	@Override
	public void initRender() 
	{
		this.sprites = steps[0];
		this.sprite = sprites[0];

		this.reRender();
	}

	@Override
	public CompoundTag writeToNBT(CompoundTag tag) 
	{
		tag = super.writeToNBT(tag);
		
		StringTag username = new StringTag("username", this.username);
		Map<String, Tag> map = tag.getValue();
		Map<String, Tag> map2 = new HashMap<String, Tag>();
		map2.putAll(map);		
		map2.put("username", username);
		
		tag = new CompoundTag(tag.getName(), map2);	
		tag = this.inventory.writeToNBT(tag);
		
		return tag;
	}	
}
