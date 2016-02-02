import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class myTRC{
	static int pages;
	public static RowBufferControl RowMemory = new RowBufferControl(512*pages);
	public static ColBufferControl ColMemory = new ColBufferControl(512*pages);
	public static _BufferTable BufferTable = new _BufferTable(pages*2);
	public static BufferStore BufferMemory = new BufferStore(512,2*pages);
	public static Disk myDisk;
	public static void main(String[] args) throws Exception {
		String Directory;
		if(args.length<1){
			System.out.println("Error: paramater required");
			return ;
		}
		pages = Integer.parseInt(args[0]);
		RowMemory = new RowBufferControl(512*pages);
		ColMemory = new ColBufferControl(512*pages);
		BufferTable = new _BufferTable(pages*2);
		BufferMemory = new BufferStore(512,2*pages);
		Directory = System.getProperty("user.dir")+"/data/";
		File dir = new File(Directory);
		if (dir.exists()) {
			try{
	               delete(dir);
	           }catch(IOException e){
	               e.printStackTrace();
	               System.exit(0);
	           }
		}
		myDisk = new Disk();
		mPage rowPage = new mPage(mPage.PAGEROW);
		byte[] src= new byte[512];
		int ID;
		String sName;
		String sPhone;
		
		int counter = 0;
		
		ByteBuffer  Name= ByteBuffer.allocate(16);
		ByteBuffer  Phone= ByteBuffer.allocate(12);
		System.out.println(Paths.get(System.getProperty("user.dir")+"/Y.txt"));
		for (String line : Files.readAllLines(Paths.get("Y.txt"))) {
				if(counter == 16){
					myDisk.InsertRowRecord(rowPage, "Y");
					rowPage= new mPage(mPage.PAGEROW);
					counter = 0;
				}
				Name = ByteBuffer.allocate(16);
				Phone = ByteBuffer.allocate(12);
				String[] temp = line.split(",");
				ID = Integer.parseInt(temp[0]);
				Name.put(temp[1].getBytes());//check if there are one more byte at the end
				Phone.put(temp[2].getBytes());
				rowPage.data.putInt(ID);
				rowPage.data.put(Name.array());
				rowPage.data.put(Phone.array());
				counter++;
			
		}

		
		rowPage = new mPage(mPage.PAGEROW);
		counter = 0;
		Name= ByteBuffer.allocate(16);
		Phone= ByteBuffer.allocate(12);
		//System.out.println(Paths.get(System.getProperty("user.dir")+"/Y.txt"));
		for (String line : Files.readAllLines(Paths.get("X.txt"))) {
		if(counter == 16){
		myDisk.InsertRowRecord(rowPage, "X");
		rowPage= new mPage(mPage.PAGEROW);
		counter = 0;
		}
		Name = ByteBuffer.allocate(16);
		Phone = ByteBuffer.allocate(12);
		String[] temp = line.split(",");
		ID = Integer.parseInt(temp[0]);
		Name.put(temp[1].getBytes());//check if there are one more byte at the end
		Phone.put(temp[2].getBytes());
		if(ID == 6){
			System.out.println();
		}
		rowPage.data.putInt(ID);
		rowPage.data.put(Name.array());
		rowPage.data.put(Phone.array());
		counter++;
		}
		rowPage = new mPage(mPage.PAGEROW);
		counter = 0;
		Name= ByteBuffer.allocate(16);
		Phone= ByteBuffer.allocate(12);
		//System.out.println(Paths.get(System.getProperty("user.dir")+"/Y.txt"));
		for (String line : Files.readAllLines(Paths.get("Z.txt"))) {
		if(counter == 16){
		myDisk.InsertRowRecord(rowPage, "Z");
		rowPage= new mPage(mPage.PAGEROW);
		counter = 0;
		}
		Name = ByteBuffer.allocate(16);
		Phone = ByteBuffer.allocate(12);
		String[] temp = line.split(",");
		ID = Integer.parseInt(temp[0]);
		Name.put(temp[1].getBytes());//check if there are one more byte at the end
		Phone.put(temp[2].getBytes());
		rowPage.data.putInt(ID);
		rowPage.data.put(Name.array());
		rowPage.data.put(Phone.array());
		counter++;
		}
		
		
		String readbuffer;
		BufferedReader br = new BufferedReader(new FileReader("s1.txt"));
		System.out.println("Script Started:");
		int k=1;
		String lastCommandType="N";
		while( (readbuffer=br.readLine() )!=null ){
		 	TRCengin cmd = new TRCengin(readbuffer);
		 	System.out.println(k+" "+readbuffer);
		 	if(k==37){
		 		System.out.println("I am Here");
		 	}
		 	if(cmd.CmdType.equals("I")&&!cmd.illegal){
		 		cmd.ExecutecmdI();
		 		lastCommandType="I";
		 		System.out.println("Inserted: "+cmd.Identifier+", "+cmd.PeopleName+", "+cmd.Telephone);
			}else{
				if(lastCommandType.equals("I"))
					RowMemory.FleshMemory();
				if(cmd.CmdType.equals("R") ){
					cmd.ExecutecmdR();
				}else{
					if(cmd.CmdType.equals("M") ){
						cmd.ExecutecmdM();
					}else{
						if(cmd.CmdType.equals("D") ){
							System.out.println("Deleted: "+cmd.TableTarget);
							cmd.ExecutecmdD();
						}else{
							cmd.ExecutecmdG();
						}
					}
				}
				lastCommandType="N";
			}
		 	k++;
		 }
		 br.close();
	}
	
	public static void delete(File file)
	    	throws IOException{
	 
	    	if(file.isDirectory()){
	 
	    		//directory is empty, then delete it
	    		if(file.list().length==0){
	 
	    		   file.delete();
	    		   System.out.println("Directory is deleted : " 
	                                                 + file.getAbsolutePath());
	 
	    		}else{
	 
	    		   //list all the directory contents
	        	   String files[] = file.list();
	 
	        	   for (String temp : files) {
	        	      //construct the file structure
	        	      File fileDelete = new File(file, temp);
	 
	        	      //recursive delete
	        	     delete(fileDelete);
	        	   }
	 
	        	   //check the directory again, if empty then delete it
	        	   if(file.list().length==0){
	           	     file.delete();
	        	     System.out.println("Directory is deleted : " 
	                                                  + file.getAbsolutePath());
	        	   }
	    		}
	 
	    	}else{
	    		//if file, then delete it
	    		file.delete();
	    		System.out.println("File is deleted : " + file.getAbsolutePath());
	    	}
	    }
	
}