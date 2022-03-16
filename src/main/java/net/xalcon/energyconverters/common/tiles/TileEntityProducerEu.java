package net.xalcon.energyconverters.common.tiles;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.info.Info;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.xalcon.energyconverters.EnergyConverters;
import net.xalcon.energyconverters.common.EnergyConvertersConfig;

@Optional.Interface(iface="ic2.api.energy.tile.IEnergySource", modid="ic2", striprefs=true)
public class TileEntityProducerEu extends TileEntityEnergyConvertersProducer implements ITickable, IEnergySource
{
	private double internalBuffer;
	private double maxEnergyUnits;
	private boolean addedToNet;
	private int tier;

	public TileEntityProducerEu()
	{
		this.addedToNet = false;
	}

	public TileEntityProducerEu(int tier) { super(); this.tier = tier; }

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		super.readFromNBT(compound);
		this.tier = compound.getInteger("tier");
		this.internalBuffer = compound.getDouble("buffer");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound)
	{
		compound.setInteger("tier", this.tier);
		compound.setDouble("buffer", this.internalBuffer);
		return super.writeToNBT(compound);
	}

	private void onLoaded()
	{
		if(this.addedToNet || FMLCommonHandler.instance().getEffectiveSide().isClient() || !Info.isIc2Available()) return;
		MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
		this.addedToNet = true;
		this.maxEnergyUnits = EnergyNet.instance.getPowerFromTier(this.getSourceTier());
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		onChunkUnload();
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		if (this.addedToNet && Info.isIc2Available())
		{
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			this.addedToNet = false;
		}
	}

	@Override
	public void update()
	{
		if(this.getWorld().isRemote) return;
		if(!addedToNet) onLoaded();

		if(this.internalBuffer < this.maxEnergyUnits && this.getBridgeEnergyStored() > 0)
		{
			double delta = (this.maxEnergyUnits - this.internalBuffer) * EnergyConvertersConfig.ic2Conversion;
			double received = this.retrieveEnergyFromBridge(delta, false);
			this.internalBuffer += received / EnergyConvertersConfig.ic2Conversion;
		}
	}

	@Optional.Method(modid = "ic2")
	@Override
	public double getOfferedEnergy()
	{
		return this.internalBuffer;
	}

	@Optional.Method(modid = "ic2")
	@Override
	public void drawEnergy(double v)
	{
		this.internalBuffer -= v;
		if(this.internalBuffer < 0) this.internalBuffer = 0;
		else if(this.internalBuffer > this.maxEnergyUnits)
			this.internalBuffer = this.maxEnergyUnits;
	}

	@Optional.Method(modid = "ic2")
	@Override
	public int getSourceTier()
	{
		return this.tier;
	}

	@Optional.Method(modid = "ic2")
	@Override
	public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing enumFacing)
	{
		return true;
	}
}
