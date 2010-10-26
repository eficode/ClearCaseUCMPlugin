package net.praqma.clearcase.objects;

import net.praqma.debug.Debug;

class View extends ClearBase
{
	public static enum ViewType
	{
		NONE,
		SNAPVIEW,
		DYNAMIC;
	}
	
	protected ViewType viewType = ViewType.NONE;
	
	public ViewType GetViewType()
	{
		return viewType;
	}
}