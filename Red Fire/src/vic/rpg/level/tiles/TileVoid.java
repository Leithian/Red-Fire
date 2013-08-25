package vic.rpg.level.tiles;

import java.awt.Point;

import vic.rpg.level.Tile;

public class TileVoid extends Tile
{	
	@Override
	public String getDescription() 
	{
		return "The impassable void. It's black. And dark.";
	}

	@Override
	public boolean isWalkingPermitted() 
	{
		return false;
	}

	@Override
	public Point getTextureCoord(int x, int y, int data) 
	{	
		return new Point(0, 15);
	}
}
