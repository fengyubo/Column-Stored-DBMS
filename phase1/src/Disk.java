import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.omg.PortableServer.ID_ASSIGNMENT_POLICY_ID;

public class Disk implements DiskInterface {
	private ArrayList<Table> tables;
	private String Directory;
	private File[] files;	
	private int fileNumber;
	
	@SuppressWarnings("unchecked")
	public Disk() throws Exception{ 
		Directory = System.getProperty("user.dir")+"/data/";
		File dir = new File(Directory);
		if (!dir.exists()) {
			dir.mkdir();
		}
		tables = new ArrayList<Table>();
		files = new File(Directory).listFiles();
		if(files == null){
			fileNumber = 0;
		} else {
			fileNumber = files.length;
		}
		RandomAccessFile tID;
		RandomAccessFile tName;
		RandomAccessFile tPhone;
		//ArrayList<mPage> temp;
		String tableName;
		for(int i = 0; i < fileNumber; i+=3){
			tID = new RandomAccessFile(files[i], "rw");
			tName = new RandomAccessFile(files[i+1], "rw");
			tPhone = new RandomAccessFile(files[i+2], "rw");
			byte[] firstPage = new byte[512];
			tID.read(firstPage);
			tableName = new mPage(firstPage, mPage.PAGEHEADER).getTableName();
			Table tableTemp = new Table(tableName, tID, tName, tPhone, files[i], files[i+1], files[i+2]);
			tables.add(tableTemp);
		}
	}
	
	
	private class Table{
		private String tableName;
		private RandomAccessFile ID;
		private File IDF;
		private RandomAccessFile Name;
		private File NameF;
		private RandomAccessFile Phone;
		private File PhoneF;
		
		
		@SuppressWarnings("unused")
		public Table(String tableName) throws Exception{
			this.tableName = tableName;
			IDF = new File(Directory + (++fileNumber) + "_" + tableName + ".DATA");
			ID = new RandomAccessFile(IDF, "rw");
			NameF = new File(Directory + (++fileNumber) + "_" + tableName + ".DATA");
			Name = new RandomAccessFile(NameF, "rw");
			PhoneF = new File(Directory + (++fileNumber) + "_" + tableName + ".DATA");
			Phone = new RandomAccessFile(PhoneF, "rw");
			
			byte[] tempbytes = new byte[512];
			mPage tempPage = new mPage(tableName, mPage.PAGEID);
			tempPage.data.get(tempbytes);
			ID.write(tempbytes, 0, 512);
			new mPage(tableName, mPage.PAGENAME).data.get(tempbytes);
			Name.write(tempbytes, 0, 512);
			new mPage(tableName, mPage.PAGEPHONE).data.get(tempbytes);
			Phone.write(tempbytes, 0, 512);
		}
		public Table(String tableName, RandomAccessFile ID, RandomAccessFile Name, RandomAccessFile Phone, File IDF, File NameF, File PhoneF){
			this.tableName = tableName;
			this.ID = ID;
			this.Name = Name;
			this.Phone = Phone;
			this.IDF = IDF;
			this.NameF = NameF;
			this.PhoneF = PhoneF;
		}
		
	}

	@Override
	public mPage[] getRecord(int ID, String tableName, int pageNumber) {
		Table tempTable = getTable(tableName);
		if(tempTable == null){
			return null;
		}
		ArrayList<mPage> tempPages = new ArrayList<mPage>();
		byte[] tempPage = new byte[512];
		int hashVal = ID % 16;
		int lastIndex;
		int lastNameIndex;
		int lastPhoneIndex;
		int offset = 508;
		int tempID;
		byte[] tempName = new byte[16];
		byte[] tempPhone = new byte[12];
 		short itemIndex;
		short itemNameIndex;
		short itemPhoneIndex;
		int counter = 0;
		try {
			tempTable.ID.seek(hashVal*Integer.SIZE/8);
			tempTable.Name.seek(hashVal*Integer.SIZE/8);
			tempTable.Phone.seek(hashVal*Integer.SIZE/8);
			int tempPageNumber = tempTable.ID.readInt();
			int tempNamePageNumber = tempTable.Name.readInt();
			int tempPhonePageNumber = tempTable.Phone.readInt();
			int countRow = 0;
			if(tempPageNumber == -1){
				return null;
			}
			do {
				tempTable.ID.seek(tempPageNumber*512 + offset);
				tempTable.Name.seek(tempNamePageNumber*512 + offset);
				tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
				
				lastIndex = tempTable.ID.readInt();//find last index
				lastNameIndex = tempTable.Name.readInt();
				lastPhoneIndex = tempTable.Phone.readInt();
				if(counter == pageNumber){ //get the needed page
					int inneroffset = 2;
					int innerNameoffset = 2;
					int innerPhoneoffset = 2;
					tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset);
					tempTable.Name.seek(tempNamePageNumber*512 + offset - innerNameoffset);
					tempTable.Phone.seek(tempPhonePageNumber*512 + offset-innerPhoneoffset);
					itemIndex = tempTable.ID.readShort();
					itemNameIndex = tempTable.Name.readShort();
					itemPhoneIndex = tempTable.Phone.readShort();
					
					mPage tempMPage = new mPage(mPage.PAGEROW); 
					tempMPage.data.rewind();
					while(itemIndex != -1){
						tempTable.ID.seek(tempPageNumber*512 + itemIndex);
						tempTable.Name.seek(tempNamePageNumber*512 + itemNameIndex);
						tempTable.Phone.seek(tempPhonePageNumber*512 + itemPhoneIndex);
						tempID = tempTable.ID.readInt();
						if(tempID == 6){
							System.out.println();
						}
						tempTable.Name.read(tempName);
						tempTable.Phone.read(tempPhone);
						tempMPage.data.put(ByteBuffer.allocate(4).putInt(tempID).array());
						tempMPage.data.put(tempName);
						tempMPage.data.put(tempPhone);
						
						if(tempMPage.data.position()>=511){
							tempPages.add(tempMPage);
							tempMPage = new mPage(mPage.PAGEROW); 
							tempMPage.data.rewind();
						}
						inneroffset+=2;
						innerNameoffset += 2;
						innerPhoneoffset += 2;
						tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset);
						tempTable.Name.seek(tempNamePageNumber*512 + offset - innerNameoffset);
						tempTable.Phone.seek(tempPhonePageNumber*512 + offset-innerPhoneoffset);
						itemIndex = tempTable.ID.readShort();
						itemNameIndex = tempTable.Name.readShort();
						itemPhoneIndex = tempTable.Phone.readShort();
						if((itemNameIndex== -1) && (itemIndex != -1)){ //change file
							tempNamePageNumber = lastNameIndex;
							tempPhonePageNumber = lastPhoneIndex;
							
							tempTable.Name.seek(tempNamePageNumber*512 + offset);
							tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
							lastNameIndex = tempTable.Name.readInt();
							lastPhoneIndex = tempTable.Phone.readInt();
							innerNameoffset = 2;
							innerPhoneoffset = 2;
							tempTable.Name.seek(tempNamePageNumber*512 + offset - innerNameoffset);
							tempTable.Phone.seek(tempPhonePageNumber*512 + offset-innerPhoneoffset);
							itemNameIndex = tempTable.Name.readShort();
							itemPhoneIndex = tempTable.Phone.readShort();	
						}
						if (itemIndex == -1) {
							tempPages.add(tempMPage);
						}
						
					}
					return (mPage[]) tempPages.toArray(new mPage[tempPages.size()]);
				}
				counter++;
				if(lastIndex == -1){
					return null;
				}
				tempTable.Name.seek(tempNamePageNumber*512 + offset);
				tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
				for(int i= 0; i < 2; i++){ //get over to corresponding pages
					tempNamePageNumber = tempTable.Name.readInt();
					tempTable.Name.seek(tempNamePageNumber*512 + offset);
					tempPhonePageNumber = tempTable.Phone.readInt();
					tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
				}
				
				tempPageNumber = lastIndex;
				
			}while(lastIndex != -1); //if there are no page left out of loop
			return null;
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public Boolean InsertRowRecord(mPage rowPage, String tableName) throws Exception {
		// TODO Auto-generated method stub
		byte[] tempByte = rowPage.data.array();
		tempByte=trim(tempByte);
		int offset = 508;
		ByteBuffer tempByteBuffer = ByteBuffer.wrap(tempByte);
		Table tempTable = getTable(tableName);
		int hashVal;
		int tempPageNumber;
		if (tempTable == null){
			try {
				tempTable = new Table(tableName);
				System.out.println("CREATE:" + tableName+" Table");
				tables.add(tempTable);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for(int i = 0; i < tempByte.length; i+=32){
			tempByteBuffer.position(i);
			int tID = tempByteBuffer.getInt();
			byte[] tName = new byte[16];
			byte[] tPhone = new byte[12];
			int lastIndex;
			int lastNameIndex;
			int lastPhoneIndex;
			tempByteBuffer.get(tName, 0, 16);
			tName = trim(tName);
			tempByteBuffer.get(tPhone, 0, 12);
			hashVal = tID%16;
			tempTable.ID.seek(hashVal*Integer.SIZE/8);
			tempTable.Name.seek(hashVal*Integer.SIZE/8);
			tempTable.Phone.seek(hashVal*Integer.SIZE/8);
			tempPageNumber = tempTable.ID.readInt();
			if(tempPageNumber == -1){

			}
			int tempNamePageNumber = tempTable.Name.readInt();
			int tempPhonePageNumber = tempTable.Phone.readInt();
			if(tempPageNumber == -1){
				tempTable.ID.seek(hashVal*Integer.SIZE/8);
				tempTable.Name.seek(hashVal*Integer.SIZE/8);
				tempTable.Phone.seek(hashVal*Integer.SIZE/8);

				tempTable.ID.writeInt((int)(tempTable.ID.length()/512));
				tempTable.Name.writeInt((int)(tempTable.Name.length()/512));
				tempTable.Phone.writeInt((int)(tempTable.Phone.length()/512));
				tempTable.ID.seek(tempTable.ID.length());
				tempTable.Name.seek(tempTable.Name.length());
				tempTable.Phone.seek(tempTable.Phone.length());
				tempTable.ID.write(new byte[512]);
				tempTable.Name.write(new byte[512]);
				tempTable.Phone.write(new byte[512]);
				tempTable.ID.seek(tempTable.ID.length()-8);
				tempTable.Name.seek(tempTable.Name.length()-8);
				tempTable.Phone.seek(tempTable.Phone.length()-8);
				tempTable.ID.writeShort(-1);
				tempTable.Name.writeShort(-1);
				tempTable.Phone.writeShort(-1);
				tempTable.ID.writeShort(0);
				tempTable.Name.writeShort(0);
				tempTable.Phone.writeShort(0);
				tempTable.ID.writeInt(-1);
				tempTable.Name.writeInt(-1);
				tempTable.Phone.writeInt(-1);
				tempTable.ID.seek(tempTable.ID.length()-512);
				tempTable.Name.seek(tempTable.Name.length()-512);
				tempTable.Phone.seek(tempTable.Phone.length()-512);
				tempTable.ID.writeInt(tID);
				tempTable.Name.write(tName);
				tempTable.Phone.write(tPhone);
			} else {
				do {
					tempTable.ID.seek(tempPageNumber*512 + offset);
					
					lastIndex = tempTable.ID.readInt();//find last index
					if(lastIndex == -1){
						break;///problem
					}
					
					if(lastIndex != -1){
						tempPageNumber = lastIndex;
					}
				}while(lastIndex != -1); //find the end of in all the three files
				
				do {
					tempTable.Name.seek(tempNamePageNumber*512 + offset);
					tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
					
					lastNameIndex = tempTable.Name.readInt();
					lastPhoneIndex = tempTable.Phone.readInt();
					if(lastNameIndex == -1){
						break;///problem
					}
					
					if(lastNameIndex != -1){
						tempNamePageNumber = lastNameIndex;
						tempPhonePageNumber = lastPhoneIndex;
					}
					
					
				}while(lastNameIndex != -1); //find the end of in all the three files
				
				
				int inneroffset = 2;
				
				tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset);
				short itemIndex = tempTable.ID.readShort();
				while(itemIndex != -1) {
					inneroffset+=2;
					tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset);
					itemIndex = tempTable.ID.readShort();	
				}
				int countRow = (inneroffset-2)/2;
				if(countRow == (512-2*Integer.SIZE/8)/(Integer.SIZE/8+Short.SIZE/8)){
					
					tempTable.ID.seek(tempPageNumber*512 + offset);
					tempTable.Name.seek(tempNamePageNumber*512 + offset);
					tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
					
					tempTable.ID.writeInt((int)(tempTable.ID.length()/512));
					tempTable.Name.writeInt((int)(tempTable.Name.length()/512));
					tempTable.Phone.writeInt((int)(tempTable.Phone.length()/512));
					
					tempTable.ID.seek(tempTable.ID.length());
					tempTable.Name.seek(tempTable.Name.length());
					tempTable.Phone.seek(tempTable.Phone.length());
					
					tempTable.ID.write(new byte[512]);
					tempTable.Name.write(new byte[512]);
					tempTable.Phone.write(new byte[512]);
					
					tempTable.ID.seek(tempTable.ID.length()-8);
					tempTable.Name.seek(tempTable.Name.length()-8);
					tempTable.Phone.seek(tempTable.Phone.length()-8);
					
					tempTable.ID.writeShort(-1);
					tempTable.Name.writeShort(-1);
					tempTable.Phone.writeShort(-1);
					
					tempTable.ID.writeShort(0);
					tempTable.Name.writeShort(0);
					tempTable.Phone.writeShort(0);
					
					tempTable.ID.writeInt(-1);
					tempTable.Name.writeInt(-1);
					tempTable.Phone.writeInt(-1);
					
					tempTable.ID.seek(tempTable.ID.length()-512);
					tempTable.Name.seek(tempTable.Name.length()-512);
					tempTable.Phone.seek(tempTable.Phone.length()-512);
					
					tempTable.ID.writeInt(tID);
					tempTable.Name.write(tName);
					tempTable.Phone.write(tPhone);
					//return true;
				} else {
					int innerNameoffset = 2;
					
					tempTable.Name.seek(tempNamePageNumber*512 + offset - innerNameoffset);
					itemIndex = tempTable.Name.readShort();
					while(itemIndex != -1) {
						innerNameoffset+=2;
						tempTable.Name.seek(tempNamePageNumber*512 + offset - innerNameoffset);
						itemIndex = tempTable.Name.readShort();	
					}
					countRow = (innerNameoffset-2)/2;
					if(countRow == (512-2*Integer.SIZE/8)/(4*Integer.SIZE/8+Short.SIZE/8)){
						
						tempTable.Name.seek(tempNamePageNumber*512 + offset);
						tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
						
						tempTable.Name.writeInt((int)(tempTable.Name.length()/512));
						tempTable.Phone.writeInt((int)(tempTable.Phone.length()/512));
						
						tempTable.Name.seek(tempTable.Name.length());
						tempTable.Phone.seek(tempTable.Phone.length());
						
						tempTable.Name.write(new byte[512]);
						tempTable.Phone.write(new byte[512]);
						
						tempTable.Name.seek(tempTable.Name.length()-8);
						tempTable.Phone.seek(tempTable.Phone.length()-8);
						
						tempTable.Name.writeShort(-1);
						tempTable.Phone.writeShort(-1);
						
						tempTable.Name.writeShort(0);
						tempTable.Phone.writeShort(0);
						
						tempTable.Name.writeInt(-1);
						tempTable.Phone.writeInt(-1);
						
						tempTable.Name.seek(tempTable.Name.length()-512);
						tempTable.Phone.seek(tempTable.Phone.length()-512);
						
						tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset + Short.SIZE/8); //ID table manipulating
						short IDIndex = tempTable.ID.readShort();
						tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset);
						tempTable.ID.writeShort(IDIndex+(short)Integer.SIZE/8);
						tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset - Short.SIZE/8);
						tempTable.ID.writeShort(-1);
						tempTable.ID.seek(tempPageNumber*512 + IDIndex+Integer.SIZE/8);
						
						tempTable.ID.writeInt(tID);
						tempTable.Name.write(tName);
						tempTable.Phone.write(tPhone);
						//return true;
					} else {
						tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset + Short.SIZE/8); //ID table manipulating
						short IDIndex = tempTable.ID.readShort();
						tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset);
						tempTable.ID.writeShort(IDIndex+Integer.SIZE/8);
						tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset - Short.SIZE/8);
						tempTable.ID.writeShort(-1);
						tempTable.ID.seek(tempPageNumber*512 + IDIndex+Integer.SIZE/8);
						
						tempTable.Name.seek(tempNamePageNumber*512 + offset - innerNameoffset + Short.SIZE/8); //Name table manipulating
						short NameIndex = tempTable.Name.readShort();
						tempTable.Name.seek(tempNamePageNumber*512 + offset - innerNameoffset);
						tempTable.Name.writeShort(NameIndex+(short)4*Integer.SIZE/8);
						tempTable.Name.seek(tempNamePageNumber*512 + offset - innerNameoffset - Short.SIZE/8);
						tempTable.Name.writeShort(-1);
						tempTable.Name.seek(tempNamePageNumber*512 + NameIndex+4*Integer.SIZE/8);
						
						tempTable.Phone.seek(tempPhonePageNumber*512 + offset - innerNameoffset + Short.SIZE/8); //Name table manipulating
						NameIndex = tempTable.Phone.readShort();
						tempTable.Phone.seek(tempPhonePageNumber*512 + offset - innerNameoffset);
						tempTable.Phone.writeShort(NameIndex+(short)4*Integer.SIZE/8);
						tempTable.Phone.seek(tempPhonePageNumber*512 + offset - innerNameoffset - Short.SIZE/8);
						tempTable.Phone.writeShort(-1);
						tempTable.Phone.seek(tempPhonePageNumber*512 + NameIndex+4*Integer.SIZE/8);
						
						tempTable.ID.writeInt(tID);
						tempTable.Name.write(tName);
						tempTable.Phone.write(tPhone);
						
					}
				}
			}
			
			
		}
		return null;
	}

	@Override
	public mPage[] getColumnPage(String tableName, int hashNumber, int pageNumber) {
		Table tempTable = getTable(tableName);
		if(tempTable == null){
			return null;
		}
		int offset = 508;
		int ID;
		ArrayList<mPage> tempPages = new ArrayList<mPage>();
		
		
		int hashVal = hashNumber;
		int lastIndex;
		int lastNameIndex;
		int lastPhoneIndex;
		
		int tempID;
		byte[] tempName = new byte[16];
		byte[] tempPhone = new byte[12];
 		short itemIndex;
		short itemNameIndex;
		short itemPhoneIndex;
		int counter = 0;
		try {
			tempTable.ID.seek(hashVal*Integer.SIZE/8);
			tempTable.Name.seek(hashVal*Integer.SIZE/8);
			tempTable.Phone.seek(hashVal*Integer.SIZE/8);
			int tempPageNumber = tempTable.ID.readInt();
			int tempNamePageNumber = tempTable.Name.readInt();
			int tempPhonePageNumber = tempTable.Phone.readInt();
			int countRow = 0;
			int countExtraPage;
			if(tempPageNumber == -1){
				return null;
			}
			do {
				tempTable.ID.seek(tempPageNumber*512 + offset);
				tempTable.Name.seek(tempNamePageNumber*512 + offset);
				tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
				
				lastIndex = tempTable.ID.readInt();//find last index
				lastNameIndex = tempTable.Name.readInt();
				lastPhoneIndex = tempTable.Phone.readInt();
				if(counter == pageNumber){ //get the needed page
					int inneroffset = 2;
					 
					tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset);
					itemIndex = tempTable.ID.readShort();
					while(itemIndex != -1) {
						inneroffset+=2;
						tempTable.ID.seek(tempPageNumber*512 + offset - inneroffset);
						itemIndex = tempTable.ID.readShort();
						
					}
					countRow = inneroffset-2;
					countExtraPage = (countRow/2)/28;//a page of name and phone can store 28 records
					if((countRow/2)%28 != 0){
						countExtraPage++;
					}
					mPage tempMPage = new mPage(mPage.PAGEID); 
					tempMPage.data.rewind();
					tempTable.ID.seek(tempPageNumber*512);
					byte[] tempPage = new byte[512];
					tempTable.ID.read(tempPage);
					tempMPage.data.put(tempPage);
					tempPages.add(tempMPage);
					for (int i = 0; i < countExtraPage; i++) {
						tempTable.Name.seek(tempNamePageNumber*512 + offset);
						lastNameIndex = tempTable.Name.readInt();
						tempMPage = new mPage(mPage.PAGENAME); 
						tempMPage.data.rewind();
						tempTable.Name.seek(tempNamePageNumber*512);
						tempTable.Name.read(tempPage);
						tempMPage.data.put(tempPage);
						tempPages.add(tempMPage);
						tempNamePageNumber = lastNameIndex;	
					}
					for (int i = 0; i < countExtraPage; i++) {
						tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
						lastPhoneIndex = tempTable.Phone.readInt();
						tempMPage = new mPage(mPage.PAGEPHONE); 
						tempMPage.data.rewind();
						tempTable.Phone.seek(tempPhonePageNumber*512);
						tempTable.Phone.read(tempPage);
						tempMPage.data.put(tempPage);
						tempPages.add(tempMPage);
						tempNamePageNumber = lastNameIndex;	
					}
					return (mPage[])tempPages.toArray(new mPage[tempPages.size()]);
					
				}
				counter++;
				if(lastIndex == -1){
					return null;
				}
				tempTable.Name.seek(tempNamePageNumber*512 + offset);
				tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
				for(int i= 0; i < 2; i++){ //get over to corresponding pages
					tempNamePageNumber = tempTable.Name.readInt();
					tempTable.Name.seek(tempNamePageNumber*512 + offset);
					tempPhonePageNumber = tempTable.Phone.readInt();
					tempTable.Phone.seek(tempPhonePageNumber*512 + offset);
				}
				
				tempPageNumber = lastIndex;
				
			}while(lastIndex != -1); //if there are no page left out of loop
			return null;
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public mPage getPageCount(String tableName, String attributeName, int pageNumber) {
		Table tempTable = getTable(tableName);
		int pageType = -1;
		byte[] tempBytes = new byte[512];
		try{
			if(attributeName.equals("ID")){
				pageType = mPage.PAGEID;
				tempTable.ID.seek(pageNumber*512);
				tempTable.ID.read(tempBytes);
			} else if(attributeName.equals("Name")){
				pageType = mPage.PAGENAME;
				tempTable.Name.seek(pageNumber*512);
				tempTable.Name.read(tempBytes);
			} else if(attributeName.equals("Phone")){
				pageType = mPage.PAGEPHONE;
				tempTable.Phone.seek(pageNumber*512);
				tempTable.Phone.read(tempBytes);
			} 
			mPage temp = new mPage(pageType);
			temp.data.put(tempBytes);
			return temp;
		}catch(Exception e){
			return null;
		}
	}

	@Override
	public boolean deleteTable(String tableName) {
		Table temp = getTable(tableName);
		try {
			temp.ID.close();
			temp.Name.close();
			temp.Phone.close();
			temp.IDF.delete();
			temp.NameF.delete();
			temp.PhoneF.delete();
			tables.remove(temp);
		} catch (Exception e) {
		}
		
		
		return false;
	}

	@Override
	public boolean closeFiles() throws Exception {
		for(int i = 0; i < tables.size();i++){
			tables.get(i).ID.close();
			tables.get(i).Name.close();
			tables.get(i).Phone.close();
		}
		return true;
	}
	public Table getTable(String tableName){
		for(int i = 0; i < tables.size();i++){
			if(tables.get(i).tableName.equals(tableName)){
				return tables.get(i);
			}
		}
		return null;
	}
	static byte[] trim(byte[] bytes)
	{
	    int i = bytes.length - 1;
	    while (i >= 0 && bytes[i] == 0)
	    {
	        --i;
	    }

	    return Arrays.copyOf(bytes, i + 1);
	}
	
}

