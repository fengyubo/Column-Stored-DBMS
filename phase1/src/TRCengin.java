import java.nio.ByteBuffer;
import java.util.*;

public class TRCengin{
	String CmdType;
	String TableTarget;
	int Identifier;
	String PeopleName;
	String Telephone;
	boolean illegal=false;

	public TRCengin(String inputcmd){
		inputcmd=inputcmd.replaceAll("[()]","");
		inputcmd=inputcmd.replaceAll(","," ");
		String[] cmdcpnt=inputcmd.split(" ");
		CmdType=cmdcpnt[0];
		TableTarget=cmdcpnt[1];
		switch(CmdType){
			case("R"):
						if(cmdcpnt.length==3){
							this.Identifier=Integer.parseInt(cmdcpnt[2]);
						}else{
							this.illegal=true;
						}
						break;
			case("M"):	
			case("G"):	
						this.Telephone=cmdcpnt[2];	
						break;
			case("I"):
						this.Identifier=Integer.parseInt(cmdcpnt[2]);
						this.PeopleName=cmdcpnt[3];
						this.Telephone=cmdcpnt[4];
			case("D"):	break;
			default:
						System.out.println("Input Error");
						break;
		}
	}

	public void ExecutecmdI() throws Exception{
		ByteBuffer Name = ByteBuffer.allocate(16);
		ByteBuffer Phone = ByteBuffer.allocate(12);
		int ID = Identifier;
		Name.put(PeopleName.getBytes());//check if there are one more byte at the end
		Phone.put(Telephone.getBytes());
		ByteBuffer tmp = ByteBuffer.allocate(32);
		tmp.putInt(ID);
		tmp.put(Name.array());
		tmp.put(Phone.array());

		myTRC.RowMemory.InsertInRow(TableTarget, tmp);
		return ;
	}

	public void ExecutecmdR() throws Exception{
		myTRC.RowMemory.QueryInRow(TableTarget,Identifier);
		return ;
	}
	
	public void ExecutecmdM() throws Exception{
		myTRC.ColMemory.GroupQueryInCol(TableTarget,Telephone);
	}
	
	public void ExecutecmdG() throws Exception{
		myTRC.ColMemory.CountQueryInCol(TableTarget, Telephone);
	}
	
	public void ExecutecmdD() throws Exception{
		myTRC.myDisk.deleteTable(TableTarget);
	}
	
	public void print(String s){
		System.out.println(s);
	}

}