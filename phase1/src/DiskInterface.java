public interface DiskInterface {
	//disk_file Class
	//Row
	public mPage[] getRecord(int ID, String tableName, int pageNumber);
	
	public Boolean InsertRowRecord(mPage rowPage, String tableName) throws Exception;
	
	//Column
	public mPage[] getColumnPage(String tableName, int hashNumber, int pageNumber);
	
	public mPage getPageCount(String tableName, String attributeName, int pageNumber);
	
	//Table
	public boolean deleteTable(String tableName);
	
	public boolean closeFiles() throws Exception;
}
