import java.nio.*;
import java.io.*;
import java.util.*;

public class ColBufferControl{
	_BufferTable PageTableCol;
	BufferStore MemoryCol;

	static int PageSize=512;
	static int RecordSize=32;
	static int IDSize=4;
	static int PeopleNameSize=16;
	static int PhoneSize=12;
	private int PageNum;
	private int HashFactor=16;

	//the paramater capacity means size of buffer, containing row and col,
	//which counts as ABSOLUTE value, not page
	public ColBufferControl(int capacity){
		this.PageNum=capacity/(PageSize);
		this.PageTableCol = new _BufferTable(PageNum);
		this.MemoryCol = new BufferStore(PageSize,PageNum);
	}

	//public Col read: read from disc and find required col val
	//This time, there is no memory, just read disc
	//otherwise, connect to disc and load page, do query
	public void GroupQueryInCol(String TableName, String AreaMach) throws Exception{
		//call disc to get page data, the size of pages should be limited
		//Vector<Integer> ResultCol = new Vector<Integer>();
		for(int i=0;i<HashFactor;++i){
			ArrayList<byte[]> DataBlock;
			for(int j=0;(DataBlock=LoadfromDisc(TableName, i, j))!=null;++j){
				int TotalPages=DataBlock.size();
				int IdPages=1;
				int AttrPage=(TotalPages-IdPages)/2;
				
				if(DataBlock.size()>PageNum){
					System.out.println("Not enough pages in memory");
					return ;
				}
				Vector<Integer> freepages = new Vector<Integer>(AllocNewPagefoCol(DataBlock.size()));	
				
				PageTableCol.RegisterBuffer(TableName, freepages);
				MemoryCol.WriteIntoCache(freepages,DataBlock);

				Vector<Integer> ResultColIndex = new Vector<Integer>(FindRecordinColMemoryPh(TableName,AreaMach,freepages));
				if(ResultColIndex.isEmpty()){
					continue;
				}
				//Chagne page: freepages shorter
				//Vector<String> ResultColPhone = new Vector<String>(FindRecordinColMemoryGen(TableName,ResultColIndex,freepages));
				//Change Page:?
	//			Vector<String> ResultColName = new Vector<String>(FindRecordinColMemoryGen(TableName,ResultColIndex,freepages));
				//Change Page:?
	//			Vector<String> ResultColNo = new Vector<String>(FindRecordinColMemoryGen(TableName,ResultColIndex,freepages));
	//			for(int p=0;0<ResultColIndex.size();++p){
	//				System.out.println(ResultColNo.get(p)+" "+ResultColNo.get(p)+" "+ResultColNo.get(p));
				System.out.println("Pattern: "+AreaMach +" "+ResultColIndex.size());
//				}
			}
		}
		return ;
	}

	public void CountQueryInCol(String TableName, String AreaMach) throws Exception{
		int countall=0;
		for(int i=0;i<HashFactor;++i){
			ArrayList<byte[]> DataBlock;
			for(int j=0;(DataBlock=LoadfromDisc(TableName, i, j))!=null;++j){
				int TotalPages=DataBlock.size();
				int IdPages=1;
				int AttrPage=(TotalPages-IdPages)/2;
				
				if(DataBlock.size()>PageNum){
					System.out.println("Not enough pages in memory");
					return ;
				}
				Vector<Integer> freepages = new Vector<Integer>(AllocNewPagefoCol(DataBlock.size()));	
				
				PageTableCol.RegisterBuffer(TableName, freepages);
				MemoryCol.WriteIntoCache(freepages,DataBlock);

				Vector<Integer> ResultColIndex = new Vector<Integer>(FindRecordinColMemoryPh(TableName,AreaMach,freepages));
				if(ResultColIndex.isEmpty()){
					continue;
				}else{
					countall+=ResultColIndex.size();
				}
			}
		}
		System.out.println("GCount: "+countall);
		return;
	}


	private Vector<String> FindRecordinColMemoryGen(String TableName, Vector<Integer> ResultColIndex,Vector<Integer> OpPage){
		int k=0;
		Vector<String> Result_Name=new Vector<String>();
		byte[] NamePeople = new byte[PeopleNameSize];

		for(int i=0;i<ResultColIndex.size();++i){
			if(ResultColIndex.get(i)>(k+1)*PageSize/PeopleNameSize){
				++k;				
			}
			MemoryCol.Cache[OpPage.get(k)].get(NamePeople,(ResultColIndex.get(i)-(k*PageSize))*PeopleNameSize,PeopleNameSize);
			Result_Name.add(new String(NamePeople));
		}
		return Result_Name;

	}

	//this function is used for find the particually mach pattern in the memory
	private Vector<Integer> FindRecordinColMemoryPh(String TableName, String AreaMach, Vector<Integer> freepages){
		int TotalPages=freepages.size();
		int IdPages=1;
		int AttrPage=(TotalPages-IdPages)/2;

		//put into memory
		Vector<Integer> Result_Index=new Vector<Integer>();
		for(int i=IdPages+AttrPage;i<freepages.size();++i){
			int inputk=freepages.get(i);
			int offset = (i-IdPages+AttrPage)*PageSize/PeopleNameSize;
			Vector<Integer> tmp = FindRecordinColPh(inputk,AreaMach,offset);
			if(tmp==null){
				continue;
			}else{
				Result_Index.addAll(tmp);
			}
			PageTableCol.AccessPage(freepages.get(i));
		}
		return Result_Index;
	}

	private Vector<Integer> FindRecordinColPh(int k, String AreaMach,int pageoffset){
		Vector<Integer> ret=new Vector<Integer>();
		byte[] tTelphone = new byte[PhoneSize];
		byte[] TmpBuffer = new byte[PageSize];
		short index;
		MemoryCol.Cache[k].rewind();
		MemoryCol.Cache[k].get(TmpBuffer);
		int i=0,count=1;
		
		while(true){
			MemoryCol.Cache[k].rewind();
			MemoryCol.Cache[k].position(PageSize-4-2*count);
			index=MemoryCol.Cache[k].getShort();
			if(index==-1){
				break;
			}
			++count;
		}
		count--;
		for(i=0;i<count;++i){
			MemoryCol.Cache[k].rewind();
			MemoryCol.Cache[k].get(tTelphone,0,PhoneSize);
			String tmp=new String(tTelphone);
			String pat=tmp.substring(0, 3);
			
			if(pat.equals(AreaMach)){
				ret.add(i+pageoffset);
			}
		}
		if(ret.isEmpty()){
			return null;
		}else{
			return ret;
		}
	}

	private ArrayList<byte[]> LoadfromDisc(String TableName, int HashNumber, int PageNumber){
		//return null when get no value
		mPage[] LoadDataBlock = myTRC.myDisk.getColumnPage(TableName, HashNumber, PageNumber);
		if(LoadDataBlock==null){
			return null;
		}
		ArrayList<byte[]> DatafromDisc = new ArrayList<byte[]>();
		byte[] RDatafromDisc = new byte[PageSize];
		for(int i=0;i<DatafromDisc.size();++i){
			LoadDataBlock[i].data.rewind();
			LoadDataBlock[i].data.get(RDatafromDisc);
			DatafromDisc.add(RDatafromDisc);
		}
		return DatafromDisc;
	}


	//Basiclly to say, everytime you try to write, firstly, you have to call this function, then do write thing
	//allocate new page for row: everytime when you try to write, you write in a dirty one: for insert
	//what it returns: a new page number.
	private int AllocNewPagefoCol() throws Exception{
		Vector<Integer> freepage = new Vector<Integer>(PageTableCol.SwapOutIndex(1));
		//freepage is the page that should be swap out,
		//and the page could be read page, or write page.
		MemoryCol.PageReplace(freepage,PageTableCol);
		PageTableCol.ClearBuffer(freepage);
		return freepage.get(0);
	}

	private Vector<Integer> AllocNewPagefoCol(int k) throws Exception{
		Vector<Integer> freepage = new Vector<Integer>(PageTableCol.SwapOutIndex(k));
		//freepage is the page that should be swap out,
		//and the page could be read page, or write page.
		//swap out
		MemoryCol.PageReplace(freepage,PageTableCol);
		PageTableCol.ClearBuffer(freepage);
		return freepage;
	}

	private Vector<Integer> AllocNewPagefoCol(Vector<Integer> freepage) throws Exception{
		MemoryCol.PageReplace(freepage,PageTableCol);
		PageTableCol.ClearBuffer(freepage);
		return freepage;
	}

}