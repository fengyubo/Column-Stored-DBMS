import java.util.*;
import java.lang.*;

public class _BufferTable{
	BufferTableRowEntry[] BufferTable;
	int BufferCapacity;//unit of one page
	
	public _BufferTable(int page_size){
		BufferTable = new BufferTableRowEntry[page_size];
		for(int i=0;i< page_size; i++){
			BufferTable[i] = new  BufferTableRowEntry();
		}
		BufferCapacity=page_size;
	}

	public void AccessPage(int npage){
		//renew  pages access times
		for(int i=0;i<BufferCapacity;++i){
			if( !(i!=npage && BufferTable[i].getIdle())){
				//if it is not the access page and it is in use
				BufferTable[i].IncreaseUsageTimes();
				BufferTable[i].incLast();
			}else{
				//otherwise, page is idle or the pages that is "the" page
				BufferTable[i].clearUsageTimes();
			}
		}
		return ;
	}
	
	//return ArrayList that contains kickpagenum #pages in  Table that could be swap out
	private ArrayList<Integer> LRUKickOutPage(int kickpagenum){
		ArrayList<Integer> ret = new ArrayList<Integer>(kickpagenum);
		ArrayList<Integer> index = new ArrayList<Integer>(BufferCapacity);
		for(int i=0;i<BufferCapacity;++i){
			index.add(BufferTable[i].getUsageTimes());
		}
		Collections.sort(index, Collections.reverseOrder());

			for(int j=0;j<kickpagenum;++j){
				for(int i=0;i<BufferCapacity;++i){
				if(BufferTable[i].getUsageTimes()==index.get(j)){
					ret.add(i);
				}
			}
		}
		return ret;
	}

	private static Vector<Integer> removeDuplicateValue(Vector<Integer>source) {
		Vector<Integer>newVector = new Vector<Integer>();
		int size = source.size();
		for (int i = 0; i < size; i++) {
			int value = source.get(i);
			if (!newVector.contains(value)) {
				newVector.add(value);
			}
		}
		return newVector;
	}
	
	private Vector<Integer> getIdlePage(){
		Vector<Integer> ret = new Vector<Integer>();
		for(int i=0;i<BufferCapacity;++i){
			if(!BufferTable[i].getIdle())
				ret.add(i);
		}
		return ret;
	}

	//the vector gives out the indexs that could be used for swapping,in other words, it is aviliable pages.
	//to be more specifically, SwapOutIndex is used for swapping out, Buffer->Disc
	public Vector<Integer> SwapOutIndex(int swapinPagesNum){
		Vector<Integer> idlepageindex = new Vector<Integer>(getIdlePage());
		if(idlepageindex.size()<swapinPagesNum){//idle pages is less than comming in pages
		idlepageindex.addAll(LRUKickOutPage(swapinPagesNum-idlepageindex.size()));
		idlepageindex=removeDuplicateValue(idlepageindex);
		if(idlepageindex.size()>swapinPagesNum){
			int tmp=idlepageindex.size();
			while(tmp!=swapinPagesNum){
				idlepageindex.remove(0);
				tmp=idlepageindex.size();
			}
		}
		}else{
			int tmp=idlepageindex.size();
			while(tmp!=swapinPagesNum){
				idlepageindex.remove(0);
				tmp=idlepageindex.size();
			}
			
		}
		for(int i=0;i<idlepageindex.size();++i){
			System.out.println("SWAP OUT"+"T-"+BufferTable[i].getTableName()+"#Page-"+idlepageindex.get(i));
		}
		return idlepageindex;
	}

	public void ClearBuffer(){
		for(int i=0;i<BufferCapacity;++i){
			BufferTable[i].refleshEntry("");
		}
		return ;
	}

	public void ClearBuffer(Vector<Integer> Index){
		if(Index.isEmpty()) return ;
		for(int i=0;i<Index.size();++i){
			BufferTable[Index.get(i)].refleshEntry("");
		}
		return ;
	}


	public void RegisterBuffer(String TableName, Vector<Integer> Index){
		if(Index.isEmpty()) return ;
		for(int i=0;i<Index.size();++i){
			BufferTable[Index.get(i)].refleshEntry(TableName);
		}
		for(int i=0;i<Index.size();++i){
			System.out.println("SWAP IN"+"T-"+TableName+"#Page-"+Index.get(i));
		}

		return ;
	}

	public void RegisterBuffer(String TableName, int Index){
		System.out.println("SWAP IN"+"T-"+TableName+"#Page-"+Index);
		BufferTable[Index].refleshEntry(TableName);
		BufferTable[Index].setUnIdle();
		BufferTable[Index].setDirtyBit();
		return ;
	}

	//search for all tables that is used by Table for reading
	public Vector<Integer> TableMapRPage(String TableName){
		Vector<Integer> ret = new Vector<Integer>();
		for(int i=0;i<BufferCapacity;++i)
		{
			if(BufferTable[i].getDirtyBit()||(BufferTable[i].getTableName()!=TableName)){
				continue;
			}else{
				ret.add(i);
			}
		}
		return ret;
	}

	//search for all tables that is used by Table for writing
	public Vector<Integer> TableMapWPage(String TableName){
		Vector<Integer> ret = new Vector<Integer>();
		for(int i=0;i<BufferCapacity;++i)
		if( BufferTable[i].getDirtyBit() && (BufferTable[i].getTableName().equals(TableName))){
			ret.add(i);
		}else{
			continue;
		}
		return ret;
	}
}
