import java.nio.ByteBuffer;

public class mPage {
	public ByteBuffer data;
	public static final int PAGEROW = 0;
	public static final int PAGEID = 1;
	public static final int PAGENAME = 2;
	public static final int PAGEPHONE = 3;
	public static final int PAGEHEADER = 4;
	int pageType; //0 as row page 1 as ID page 2 as name 3 as phone
	public mPage(int pageType){
		data = ByteBuffer.allocate(512);
		this.pageType = pageType;
		
	}
	public mPage(String tableName, int attribute){
		data = ByteBuffer.allocate(512);
		this.pageType = PAGEHEADER;
		data.rewind();
		for(int i = 0; i < 16;i++){
			data.putInt(-1);
		}
		//data.position(16*Integer.SIZE);
		data.putInt(attribute); 
		data.putInt(tableName.length());
		data.put(tableName.getBytes());
		data.rewind();
	}
	public String getTableName(){
		data.rewind();
		data.position(17*Integer.SIZE/8);  //17 = 16 + 1 attribute number
		int nameLength = data.getInt();
		byte[] temp = new byte[nameLength];
		data.get(temp);
		return new String(temp);
	}
	public int getAttributeName(){
		data.rewind();
		data.position(16*Integer.SIZE/8);
		return data.getInt();
	}
	public mPage(byte[] data, int pageType){
		this.data = ByteBuffer.allocate(512);
		this.data.put(data);
		this.pageType = pageType;
	}
	
}
