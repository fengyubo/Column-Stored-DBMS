
import java.nio.*;
import java.util.*;

public class RowBufferControl{
	_BufferTable PageTableRow;
	BufferStore MemoryRow;

	static int PageSize=512;
	static int RecordSize=32;
	static int IDSize=4;
	static int PeopleNameSize=16;
	static int PhoneSize=12;
	private int PageNum;

	//the paramater capacity means size of buffer, containing row and col,
	//which counts as ABSOLUTE value, not page
	public RowBufferControl(int capacity){
		this.PageNum=capacity/(PageSize);
		this.PageTableRow = new _BufferTable(PageNum);
		this.MemoryRow = new BufferStore(PageSize,PageNum);
	}

	// Principle: 
	// 	check if the table is in the buffer
	// 		if in the buffer, then try to find any one that is not full
	// 			if 		can find, then write into it, nothing happend
	// 			else 	allocate new page and write it to the new page
	// 		else not in the buffer
	// 			allocate new page, register in the table and write
	
	//though the string size is not limited, but infact it is JUST for one record. PLEASE NOTICE THIS!!!
	//*****/The length of the String should be 32bytes, also if there is hollow, then padding it by space/***********
	public void InsertInRow(String TableName, ByteBuffer target) throws Exception{
		Vector<Integer> wPageofT = new Vector<Integer>(PageTableRow.TableMapWPage(TableName));
		int PageIndex=0;
		if(wPageofT.isEmpty()){
			PageIndex = AllocNewPageforRow();
			PageTableRow.RegisterBuffer(TableName,PageIndex);
		}else{//not empty
			int i=0;
			while( i<wPageofT.size() && PageTableRow.BufferTable[wPageofT.get(i)].checkFull(RecordSize,PageSize)) i++;
			if(i==wPageofT.size()){
				PageIndex=AllocNewPageforRow();
				PageTableRow.RegisterBuffer(TableName,PageIndex);
			}else{
				PageIndex=wPageofT.get(i);
			}
		}
		//process to write into page
		MemoryRow.WriteIntoCache(PageIndex,target);
		PageTableRow.AccessPage(PageIndex);
		return ;
	}

	// Principle:
	// Check memory first, if the record is in memory, then just give it out.
	// otherwise, connect to disc and load page, do query
	public void QueryInRow(String TableName, int Identify) throws Exception{
		boolean flagfind=false;
		//case: if there is already buffered in the memory, then just use it;
		Vector<Integer> ExisitPage = new Vector<Integer>(PageTableRow.TableMapRPage(TableName));	
		if(!ExisitPage.isEmpty()){//not empty
			for(int i=0;i<ExisitPage.size();++i){
				String ret = FindRecordinMemory(ExisitPage.get(i),Identify);
				if(ret!=null){
					System.out.println(ret+"\n");
					flagfind=true;
					break;
				}
			}
			if(flagfind){
				return ;
			}else{
				//just flush this parts memory, optimization
				//AllocNewPageforRow(ExisitPage);
			}
		}
		//call disk to get data, store into a buffer between disc and memory
		//Noticed onething is that probably memory is quite small, then there will be many times to get result
		ArrayList<byte[]> DiscData = new ArrayList<byte[]>(LoadfromDisc(TableName,Identify));
		int LastBlock=0;
		
		for(int step=Math.min(PageNum,DiscData.size());step!=0;step-=Math.min(PageNum,DiscData.size())){
			//since ArrayList will be short when try to remove
			Vector<Integer> freepages = new Vector<Integer>(AllocNewPageforRow(step));
			PageTableRow.RegisterBuffer(TableName,freepages);
			ArrayList<byte[]> PartDiscData=new ArrayList<byte[]>();
			for(int i=0;i<step;++i){
				PartDiscData.add(DiscData.get(LastBlock));
				LastBlock++;
				//just copy some element from Dick data to paramater
			}
			MemoryRow.WriteIntoCache(freepages, PartDiscData);
			for(int i=0;i<freepages.size();++i){
				String ret = FindRecordinMemory(freepages.get(i),Identify);
				PageTableRow.AccessPage(freepages.get(i));
				if(ret!=null){
					System.out.println("MRead"+ret);
					flagfind=true;
					break;
				}
			}
			if(flagfind) break;
		}
		if(!flagfind){
			System.out.println("No such record"+"\n");
		}
		return ;
	}

	//Find a particually record in the memory, page k, by identify
	//if could find, print and return this tuple
	//otherwise, return null
	private String FindRecordinMemory(int k, int Identify){
		String ret = new String("");
		int tIdentifier;
		byte[] tPeopleName = new byte[PeopleNameSize];
		byte[] tTelphone = new byte[PhoneSize];
		MemoryRow.Cache[k].rewind();
		for(int i=0;i<PageSize/RecordSize;++i){//How many record perpage
			tIdentifier=MemoryRow.Cache[k].getInt();
			MemoryRow.Cache[k].get(tPeopleName,0,PeopleNameSize);
			MemoryRow.Cache[k].get(tTelphone,0,PhoneSize);
			if(tIdentifier==Identify){
				ret=Integer.toString(Identify)+new String(tPeopleName)+new String(tTelphone);
				MemoryRow.Cache[k].rewind();
				PageTableRow.AccessPage(k);
				return ret;
			}
		}
		PageTableRow.AccessPage(k);
		MemoryRow.Cache[k].rewind();
		return null;
	}

	//Load data from Disc to Memory
	//in this process, the file format is fixed. careful
	private ArrayList<byte[]> LoadfromDisc(String TableName, int Identify){
		ArrayList<byte[]> DatafromDisc = new ArrayList<byte[]>();
		byte[] SingleDataBlock = new byte[PageSize];
		mPage[] RetriveItem;
		for(int i=0;;++i){
			RetriveItem=myTRC.myDisk.getRecord(Identify, TableName, i);
			if(RetriveItem == null){
				break;
			}
				for(int j=0;j<RetriveItem.length;++j){
					SingleDataBlock = new byte[512];
					RetriveItem[j].data.rewind();
					RetriveItem[j].data.get(SingleDataBlock);
					DatafromDisc.add(SingleDataBlock);
				}
		}
		return DatafromDisc;
	}

	//Basiclly to say, everytime you try to write, firstly, you have to call this function, then do write thing
	//allocate new page for row: everytime when you try to write, you write in a dirty one: for insert
	//what it returns: a new page number.
	private int AllocNewPageforRow() throws Exception{
		Vector<Integer> freepage = new Vector<Integer>(PageTableRow.SwapOutIndex(1));
		//freepage is the page that should be swap out,
		//and the page could be read page, or write page.
		MemoryRow.PageReplace(freepage,PageTableRow);
		PageTableRow.ClearBuffer(freepage);
		return freepage.get(0);
	}

	private Vector<Integer> AllocNewPageforRow(int k) throws Exception{
		Vector<Integer> freepage = new Vector<Integer>(PageTableRow.SwapOutIndex(k));
		//freepage is the page that should be swap out,
		//and the page could be read page, or write page.
		//swap out
		MemoryRow.PageReplace(freepage,PageTableRow);
		PageTableRow.ClearBuffer(freepage);
		return freepage;
	}

	private Vector<Integer> AllocNewPageforRow(Vector<Integer> freepage) throws Exception{
		MemoryRow.PageReplace(freepage,PageTableRow);
		PageTableRow.ClearBuffer(freepage);
		return freepage;
	}
	
	public void FleshMemory() throws Exception{
		AllocNewPageforRow(PageNum);
	}

}