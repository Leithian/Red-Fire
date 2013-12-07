package vic.rpg.item;

public class ItemPeer extends Item {

	public ItemPeer() 
	{
		super("/vic/rpg/resources/item/peer.png", 2);
	}
	
	@Override
	public String getItemName() 
	{
		return "Peer";
	}
}
