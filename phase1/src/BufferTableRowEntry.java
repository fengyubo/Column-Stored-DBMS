import java.util.*;
import java.io.*;

public class BufferTableRowEntry{
	private String NameTable;
	private int UsageTimes;
	private boolean DirtyBit;//if write, if the page is used for just read, this bit is false
	private boolean Idle;//if use? : false: it is idle now; true: being in use.
	private int PLast;//the last one record, just for write page/row store/

	public BufferTableRowEntry(){
		NameTable="";
		UsageTimes=0;
		boolean DirtyBit=false;
		Idle=false;
		PLast=0;
	}

	public void setTableEntry(String INameTable, int IUsageTimes, boolean IDitryBit, boolean IIdle){
		this.NameTable=INameTable;
		this.UsageTimes=IUsageTimes;
		this.DirtyBit=IDitryBit;
		this.Idle=IIdle;	
		return ;
	}

	public void refleshEntry(String TableName){
		setTableEntry(TableName, 0, false, true);//%
		PLast=0;
		return ;
	}

	public void setDirtyBit(){
		this.DirtyBit=true;
		return ;
	}

	public void incLast(){
		++PLast;
		return ;
	}

	//record length should be counted as byte
	public boolean checkFull(int recordlength,int pagesize){
		if(PLast < pagesize/recordlength)
			return false;
		else
			return true;//notice length as bytes
	}

	public void clearLast(){
		PLast=0;
		return ;
	}

	public void setIdle(){
		this.Idle=false;
		return ;
	}
	public void setUnIdle(){
		this.Idle=true;
		return ;
	}

	public void clearUsageTimes(){
		this.UsageTimes=0;
	}

	public void IncreaseUsageTimes(){
		++UsageTimes;
		return ;
	}

	public boolean getDirtyBit(){
		return DirtyBit;
	}

	public String getTableName(){
		return NameTable;
	}

	public int getUsageTimes(){
		return UsageTimes;
	}

	public boolean getIdle(){
		return Idle;
	}

}