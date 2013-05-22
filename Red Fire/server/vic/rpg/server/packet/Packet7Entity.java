package vic.rpg.server.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;

import vic.rpg.level.Entity;
import vic.rpg.registry.LevelRegistry;

public class Packet7Entity extends Packet 
{
	public static final int MODE_UPDATE = 0;
	public static final int MODE_DELETE = 1;
	public static final int MODE_CREATE = 2;
	
	public int mode;
	public Entity[] entities;
	
	protected Packet7Entity(Entity[] entities, int mode, int i)
	{
		super(i);
		
		this.entities = entities;
		this.mode = mode;
	}
	
	public Packet7Entity(Entity[] entities, int mode) 
	{		
		this(entities, mode, 7);
	}
	
	public Packet7Entity()
	{
		super(7);
	}
	
	public Packet7Entity(int id)
	{
		super(id);
	}
	
	public Packet7Entity(Entity entity, int mode) 
	{
		this(new Entity[]{entity}, mode);
	}

	@Override
	public void readData(DataInputStream stream) 
	{
		try {
			mode = stream.readInt();
			
			byte[] b = new byte[stream.available()];
			stream.readFully(b);
			
			NBTInputStream in;
			ArrayList<Entity> list = new ArrayList<Entity>();	
		
			byte[] b2 = new byte[b.length - 1];
			System.arraycopy(b, 1, b2, 0, b2.length);
			in = new NBTInputStream(new DataInputStream(new ByteArrayInputStream(b2)));
			CompoundTag tag = (CompoundTag)in.readTag();
			in.close();
			Map<String, Tag> map = tag.getValue();
			
			for(Tag t : map.values())
			{
				Entity e = LevelRegistry.readEntityFromNBT((CompoundTag) t);
				list.add(e);
			}
			
			Entity[] entities = new Entity[list.size()];
			entities = list.toArray(entities);
			
			this.entities = entities;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeData(DataOutputStream stream) 
	{
		try {
			stream.writeInt(mode);

			Map<String, Tag> map = new HashMap<String, Tag>();
			for(Entity e : entities)
			{
				map.put("entity", LevelRegistry.writeEntityToNBT(e));
			}
			
			CompoundTag tag = new CompoundTag("entities", map);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			NBTOutputStream out;
			try {
				out = new NBTOutputStream(new DataOutputStream(baos));
				out.writeTag(tag);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}				
			byte[] data = baos.toByteArray();
			byte[] data2 = new byte[data.length + 1];
			System.arraycopy(data, 0, data2, 1, data.length);
			data2[0] = (byte)mode;
			
			stream.write(data2);
		
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
