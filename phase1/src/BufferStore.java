import java.nio.*;
import java.util.*;

public class BufferStore{
	ByteBuffer[] Cache;//visible cache
	static int PageSize;
	private int PageNum;
	
	//page: total pages in memory 
	BufferStore(int SinglePageSize, int TPageNum){
		this.PageSize=512;
		this.PageNum=TPageNum;
		this.Cache= new ByteBuffer[PageNum];
		for(int i=0;i<PageNum;++i){
			Cache[i]=ByteBuffer.allocate(PageSize);
			Cache[i].clear();
		}
	}

	public void WriteIntoCache(Vector<Integer> PageIndex,ArrayList<byte[]> Content){
		for(int i=0;i<PageIndex.size();++i){
			Cache[PageIndex.get(i)].rewind();
			Cache[PageIndex.get(i)].put(Content.get(i));
		}
		return ;
	}

	public boolean WriteIntoCache(int PageIndex,ByteBuffer Content){
		Cache[PageIndex].put(Content.array());
		return true;
	}

	public ArrayList<byte[]> AccessCache(Vector<Integer> PageIndex,ArrayList<byte[]> Content){
		ArrayList<byte[]> ret = new ArrayList<byte[]>();
		return ret;
	}

	//machanism: PageReplace is triggered by when there is no more space.
	//pagelist is the list that you want to make take replace happend.
	public void PageReplace(Vector<Integer> pagelist, _BufferTable PageTableRow) throws Exception{
		Iterator<Integer> iter=pagelist.iterator();
		while(iter.hasNext()){
			int currentpage=iter.next();
			if(!PageTableRow.BufferTable[currentpage].getIdle() || 
			   !PageTableRow.BufferTable[currentpage].getDirtyBit()){
				Cache[currentpage].rewind();
				//if false, means it is read page or idle page, which is not needed to be wapped out
				continue;
			}
			byte[] SpageOutData = new byte[PageSize];
//			Cache[currentpage].flip();
			Cache[currentpage].rewind();
			Cache[currentpage].get(SpageOutData);
			Cache[currentpage].clear();

			//then write all data to the file
			System.out.println("SWAP OUT");
			mPage InsertData = new mPage(SpageOutData,mPage.PAGEROW);
			myTRC.myDisk.InsertRowRecord(InsertData, PageTableRow.BufferTable[currentpage].getTableName());
//			System.out.println("Swap Out Page:");
//			System.out.println("Table Name:"+PageTableRow.BufferTable[currentpage].getTableName());
//			System.out.println("--------------------------------------------------\n");
//			System.out.println("Page:"+currentpage+"\n"+new String(SpageOutData)+"\n");
//			System.out.println("--------------------------------------------------\n");
		}
		return ;
	}




}