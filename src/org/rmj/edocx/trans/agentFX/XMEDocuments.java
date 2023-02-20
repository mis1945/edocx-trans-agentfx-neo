package org.rmj.edocx.trans.agentFX;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;
import org.rmj.edocx.trans.EDocuments;
import org.rmj.edocx.trans.pojo.UnitEDocuments;
import org.rmj.parameters.agent.XMBranch;
import org.rmj.parameters.agent.XMDepartment;
import org.rmj.client.agent.XMEmployee;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import java.text.SimpleDateFormat;
import org.rmj.appdriver.agentfx.StringHelper;
import org.rmj.parameters.agent.XMModule;
import com.fujitsu.pfu.fiscn.sdk.FiscnApp;
import com.fujitsu.pfu.fiscn.sdk.FiscnException;
import java.io.File;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.edocx.trans.pojo.UnitMasterFile;
import org.rmj.parameter.agentfx.XMBarcode;

/**
 *
 * @author jef
 */
public class XMEDocuments {
    public XMEDocuments(GRider foGRider, String fsBranchCd, boolean fbWithParent) throws FiscnException{
        this.poGRider = foGRider;
        if(foGRider != null){
            this.pbWithParnt = fbWithParent;
            this.psBranchCd = fsBranchCd;
            poCtrl = new EDocuments();
            try{
                poScan = new FiscnApp();
            }catch(FiscnException e){
                e.printStackTrace();
            
            }            
            poCtrl.setGRider(foGRider);
            poCtrl.setBranch(psBranchCd);
            poCtrl.setOrigin(psOriginxx);
            poCtrl.setDepartment(psDeptIDxx);
            poCtrl.setEmployee(psEmployID);
            poCtrl.setModule(psModuleCd);
            poCtrl.setFileCd(psFileCode);
            poCtrl.setWithParent(true);
            pnEditMode = EditMode.UNKNOWN;
        }
    }
    
    public boolean start_scan(){
        String lsFileName;
        Integer lnTotalPage;
        Integer lnB2BPagexx = 0;
        Integer lnEntryNox = 1;
        Integer lnComparex = 0;
        String lsValue = "";
        
//        if (poScan.gets == true){
//            TRANSNOX-FILECODE-0001 = XXXXXXXXXXXX-XXXX-XXXX-????.JPG
            lsFileName = getMaster("sTransNox").toString() + '-' + psFileCode + '-';
            lsValue = (String) getFileCode().getMaster("sB2BPagex");
            String [] arr = lsValue.split("»");
            
            for(int lnCtr = 0; lnCtr< arr.length ; lnCtr++){
                lnB2BPagexx=lnB2BPagexx+Integer.parseInt(arr[lnCtr]);
            }
        
            lnTotalPage = ((int) getFileCode().getMaster("nNoPagesx") *(int) getFileCode().getMaster("nNoCopies"))+lnB2BPagexx;
            if(poScan.scan(getDefaultPath(), lsFileName,poData.MasterFileItemCount()+1)==true && poScan.getScanStat()== true){
                poData.getMasterFile().clear();
                if((lnTotalPage * ItemCount()) != poScan.imageCount){
                    if(ShowMessageFX.YesNo("Do you want to continue?", "Confirmation", "Total number of scanned pages mismatch! Docs scanned: "+poScan.imageCount+"\n Supposed Value: "+lnTotalPage * ItemCount())== false){
                         for(int lnCtr=0;lnCtr<= poScan.imageCount;lnCtr++) {
                             deleteImage(lsFileName+(String) StringHelper.prepad(String.valueOf(lnCtr+1), 4, '0'));
                         }
                        return false;
                    }
                }
                
                for(int lnCtr=0;lnCtr<= poScan.imageCount -1;lnCtr++) {
                    addMasterFile();
                    
                    setMasterFile(lnCtr,"sTransNox", getMaster("sTransNox"));
                    setMasterFile(lnCtr,"sFileName", lsFileName+ (String) StringHelper.prepad(String.valueOf(lnCtr+1), 4, '0') );
                    
                    if(lnEntryNox <= poData.ItemCount()){
                        if(lnComparex==lnTotalPage){
                            lnEntryNox=lnEntryNox+1;
                            lnComparex=0;
                        }
                    }
                    setMasterFile(lnCtr,"nEntryNox",lnEntryNox);
                    lnComparex=lnComparex+1;
                }
//                 System.out.println((int) poData.MasterFileItemCount());
            }else{
                setMessage(poScan.getErrMessage());
                return false;
            }
//        }
        return true;
    }
    
    public boolean start_scanNeo(){
        String lsFileName;
        lsFileName = getMaster("sTransNox").toString() + '-';
        
        if(poScan.scan(getDefaultPath(), lsFileName,poData.MasterFileItemCount()+1)==true && poScan.getScanStat()== true){
            int lnRow = poData.MasterFileItemCount();
            for(int lnCtr=0;lnCtr<= poScan.imageCount -1;lnCtr++) {
                addMasterFile();
                    
                setMasterFile(lnCtr+lnRow,"sTransNox", getMaster("sTransNox"));
                setMasterFile(lnCtr+lnRow,"sFileName", lsFileName+ (String) StringHelper.prepad(String.valueOf((lnCtr+lnRow)+1), 4, '0') );
            }
            validate_ScanFile();
        }else{
            setMessage(poScan.getErrMessage());
            return false;
        }
        return true;
    }
    
    public boolean validate_ScanFile(){
        Integer lnTotalPage;
        Integer lnB2BPagexx = 0;
        Integer lnEntryNox = 1;
        Integer lnComparex = 0;
        String lsValue = "";
        
        lsValue = (String) getFileCode().getMaster("sB2BPagex");
        String [] arr = lsValue.split("»");
            
        for(int lnCtr = 0; lnCtr< arr.length ; lnCtr++){
            lnB2BPagexx=lnB2BPagexx+Integer.parseInt(arr[lnCtr]);
        }
        
        lnTotalPage = ((int) getFileCode().getMaster("nNoPagesx") *(int) getFileCode().getMaster("nNoCopies"))+lnB2BPagexx;
        for(int lnCtr=0;lnCtr<= poData.MasterFileItemCount()-1;lnCtr++) {
            if(lnEntryNox <= poData.ItemCount()){
                if(lnComparex==lnTotalPage){
                    lnEntryNox=lnEntryNox+1;
                    lnComparex=0;
                }
            }
            setMasterFile(lnCtr,"nEntryNox",lnEntryNox);
            lnComparex=lnComparex+1;
        }
        for(int x = 0; x<=MasterFileCount() -1; x++){
            System.out.println(x+""+getMasterFile(x, "sFileName")+getMasterFile(x, "nEntryNox"));
        }
        return true;
    }
    
    public void sample_fileSingle(){
        int nEntryNox =0;
        for(int lnCtr=0;lnCtr<= 6 -1;lnCtr++) {
            addMasterFile();

//            ShowMessageFX.Warning(null, getDetail(0, "sSourceCd").toString(), null);
            setMasterFile(lnCtr,"sTransNox", "MX00000001");
            if(lnCtr % 2 == 0){
                nEntryNox= nEntryNox+ 1;
            }
            
            setMasterFile(lnCtr,"nEntryNox", nEntryNox);
            setMasterFile(lnCtr,"sFileName", "SAMPLE"+ (String) StringHelper.prepad(String.valueOf(lnCtr+1), 4, '0') );
            
        }
        
        for(int x= 0; x <= MasterFileCount() -1; x++){
            System.out.println(getMasterFile(x, "sTransNox")+" "+ getMasterFile(x, "nEntryNox") + " " + getMasterFile(x, "sFileName"));
        }
        
    }
    
    public void sample_fileMultiple(){
        int nEntryNox =0;
        for(int lnCtr=0;lnCtr<= 30 -1;lnCtr++) {
            addMasterFile();

            setMasterFile(lnCtr,"sTransNox", "MX00000001");
//            if(lnCtr % 2 == 0){
//                nEntryNox= nEntryNox+ 1;
//            }
//            if(nEntryNox <= ItemCount()){
            setMasterFile(lnCtr,"nEntryNox", 1);
            setMasterFile(lnCtr,"sFileName", "SAMPLE"+ (String) StringHelper.prepad(String.valueOf(lnCtr+1), 4, '0') );
//            }
            
            
        }
    }
    
    
    
    
    public void reAllignImage(){
        Integer lnTotalPage;
        Integer lnB2BPagexx = 0;
        Integer lnEntryNox = 1;
        Integer lnComparex = 1;
        String lsValue = "";
                    
        lsValue = (String) getFileCode().getMaster("sB2BPagex");
        String [] arr = lsValue.split("»");

        for(int lnCtr = 0; lnCtr< arr.length ; lnCtr++){
            lnB2BPagexx=lnB2BPagexx+Integer.parseInt(arr[lnCtr]);
        }
        
        lnTotalPage = ((int) getMaster("nNoPagesx") *(int) getMaster("nNoCopies"))+lnB2BPagexx;
        for(int lnCtr=0;lnCtr<= poScan.imageCount-1;lnCtr++) {
            if(lnComparex==lnTotalPage){
                lnEntryNox=lnEntryNox+1;
                lnComparex=1;
            }
            setMasterFile(poData.MasterFileItemCount()-1,"nEntryNox",lnEntryNox);
            lnComparex=lnComparex+1;
        }
    }

    public void setMaster(String fsCol, Object foData){
        setMaster(poData.getMaster().getColumn(fsCol), foData);
    }

    public void setMaster(int fnCol, Object foData) {
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
          // Don't allow update for sTransNox, cTranStat, sModified, and dModified
            if(!(fnCol == poData.getMaster().getColumn("sTransNox") ||
                fnCol == poData.getMaster().getColumn("cTranStat") ||
                fnCol == poData.getMaster().getColumn("sModified") ||
                fnCol == poData.getMaster().getColumn("dModified"))){

                if(fnCol == poData.getMaster().getColumn("dDateFrom") || 
                    fnCol == poData.getMaster().getColumn("dDateThru")){
                    if(foData instanceof Date)
                        poData.getMaster().setValue(fnCol, foData);
                    else
                    poData.getMaster().setValue(fnCol, null);
                }
                else{
                    poData.getMaster().setValue(fnCol, foData);
                }
            }
        }
    }

    public Object getMaster(String fsCol){
        return getMaster(poData.getMaster().getColumn(fsCol));
    }
   
    public Object getMaster(int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN || poCtrl == null)
            return null;
        else{
            return poData.getMaster().getValue(fnCol);
        }
    }

    public Object getDetail(int row, String fsCol){
        return getDetail(row, poData.getDetail().get(row).getColumn(fsCol));
    }

    public Object getDetail(int row, int col){
        if(pnEditMode == EditMode.UNKNOWN || poCtrl == null)
            return null;
        else if(row < 0 || row >= poData.getDetail().size())
            return null;
      
        return poData.getDetail().get(row).getValue(col);
    }
    
    public Object getMasterFile(int row, String fsCol){
        return getMasterFile(row, poData.getMasterFile().get(row).getColumn(fsCol));
    }

    public Object getMasterFile(int row, int col){
        if(pnEditMode == EditMode.UNKNOWN || poCtrl == null)
            return null;
        else if(row < 0 || row >= poData.getMasterFile().size())
            return null;
      
        return poData.getMasterFile().get(row).getValue(col);
    }
   
    public void setDetail(int row, String fsCol, Object value){
        setDetail(row, poData.getDetail().get(row).getColumn(fsCol), value);
    }

    public void setDetail(int row, int col, Object value){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(row >= 0 && row <= poData.getDetail().size()){
                if(row == poData.getDetail().size()){
//                    poData.getDetail().add(new UnitEDocxDetail());
                }
                
                poData.getDetail().get(row).setValue(col, value);
            }
        }
    }
    
    public void setMasterFile(int row, String fsCol, Object value){
        setMasterFile(row, poData.getMasterFile().get(row).getColumn(fsCol), value);
    }

    public void setMasterFile(int row, int col, Object value){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(row >= 0 && row <= poData.getMasterFile().size()){
                if(row == poData.getMasterFile().size()){
                    poData.getMasterFile().add(new UnitMasterFile());
                }
                poData.getMasterFile().get(row).setValue(col, value);
            }
        }
    }
    
    
    public void addDetail(){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            poData.addDetail();
//            poData.getDetail().add(new UnitEDocxDetail());
        }
    }
    
    public void addMasterFile(){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            poData.getMasterFile().add(new UnitMasterFile());
        }
    }
    
    public void deleteDetail(int row){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(!(row < 0 || row >= poData.getDetail().size())){
                System.out.println("deleted:"+row);
                poData.getDetail().remove(row);
            }
        }
    }
    
    public void deleteDetailS(String rows){
        String lsValue = rows;
        String [] arr = lsValue.split("»");
        int lsDataSize = arr.length;
        
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(lsDataSize==0){
                deleteDetail(Integer.valueOf(rows));
            }else{
                for(int lnCtr = 0; lnCtr<= lsDataSize-1; lnCtr++){
                    if(lnCtr==0){
                        deleteDetail(Integer.valueOf(arr[lnCtr])-1);
                    }else{
                        deleteDetail((Integer.valueOf(arr[lnCtr])-lnCtr)-1);
                    }
                  
                }
                
            }
        }
    }
    
    public void deleteMasterFile(int row){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(!(row < 0 || row >= poData.getMasterFile().size())){
                poData.getMasterFile().remove(row);
            }
        }
    }
    
    public boolean deleteImage(int row){
        try  
        {         
            File f= new File(getDefaultPath()+getMasterFile(row, "sFileName")+".jpg");
            if(f.delete())                   
            {  
                return true;
            }  
            else  
            {  
                return false;
            }  
        }  
        catch(Exception e)  
        {  
            return false;
        }  
    }
    
    public boolean deleteImage(String fsValue){
        try  
        {         
            File f= new File(getDefaultPath()+fsValue+".jpg");
            if(f.delete())                   
            { 
                System.out.println(f);
                return true;
            }  
            else  
            {  
                System.out.println(f);
                return false;
            }  
        }  
        catch(Exception e)  
        {  
            return false;
        }  
    }
    
    
    public boolean deleteAllImage(){
        try  
        {    
            for(int lnCtr=0;lnCtr<=MasterFileCount()-1;lnCtr++) {
                if(getMasterFile(lnCtr, "sFileName") != ""){
                    File f= new File(getDefaultPath()+getMasterFile(lnCtr, "sFileName")+".jpg");
                    System.err.println(f);
                    if(!f.delete())  return false;
                }
            }
        }  
        catch(Exception e)  
        {  
            return false;
        }
            poData.getMasterFile().clear();
        return true;
    }
   
//    public void resetDetail(){
//        //mac 2019.11.27
//        //reset filename counter
//        for(int lnCtr = 0; lnCtr <= poData.ItemCount()-1; lnCtr ++){
//            setDetail(lnCtr,"sFileName", getMaster("sTransNox")+
//                        psFileCode + 
//                        (String) StringHelper.prepad(String.valueOf(lnCtr + 1), 4, '0')+".jpg" );
//        }
//    }
   
    public boolean newTransaction() {
        if(poCtrl == null){
            return false;
        }
        
        poData = (UnitEDocuments) poCtrl.newTransaction();

        if(poData == null){
            return false;
        }
        else{
            //set the values of foreign key(object) to null
            poBranch = null;
            addDetail();
            pnEditMode = EditMode.ADDNEW;
            
            return true;
        }
    }

    public boolean loadTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }

        poData = (UnitEDocuments) poCtrl.loadTransaction(fsTransNox);

        if(poData.getMaster().getTransNox()== null){
            return false;
        }
        else{
            //set the values of foreign key(object) to null
            poBranch = null;
                      
            for(int lnCtr=0;lnCtr<=poData.getDetail().size();lnCtr++){}
            
            psOriginxx = poData.getMaster().getBranchCd();
            psDeptIDxx = poData.getMaster().getDeptIDxx();
            psEmployID = poData.getMaster().getEmployID();
            psModuleCd = poData.getMaster().getModuleCd();
            
            String lsSQL = "SELECT" +
                                "  sFileCode" +
                            " FROM EDocSys_Detail" +
                            " WHERE sTransNox =  " + SQLUtil.toSQL(fsTransNox) +
                            " GROUP BY sFileCode";
             
             //Create the connection object
            Connection loCon = poGRider.getConnection();

            if(loCon == null){
                setMessage("Invalid connection!");
            }

            Statement loStmt = null;
            ResultSet loRS = null;
        
            try {
                loStmt = loCon.createStatement();
                loRS = loStmt.executeQuery(lsSQL);
                
                    if(MiscUtil.RecordCount(loRS)==0){
                    setMessage("Empty record retrieved!!!");
                }else{
                    while(loRS.next())
                    psFileCode = loRS.getString("sFileCode");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                setMessage(ex.getMessage());
            }
            finally{
                MiscUtil.close(loRS);
                MiscUtil.close(loStmt);
            }
            
            pnEditMode = EditMode.READY;
            return true;
        }
    }

    public boolean saveUpdate() {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode == EditMode.UNKNOWN){
            return false;
        }
        else{
            System.out.println(pbWithParnt);
            if(!pbWithParnt) poGRider.beginTrans();
             
            UnitEDocuments loResult=null;
            setMaster("nEntryNox", poData.MasterFileItemCount());
            if(pnEditMode == EditMode.ADDNEW)
                loResult = (UnitEDocuments) poCtrl.saveUpdate(poData, "");
            else
                loResult = (UnitEDocuments) poCtrl.saveUpdate(poData, (String) poData.getMaster().getValue(1));
            
            if(loResult == null){
                if(!pbWithParnt) poGRider.rollbackTrans();
                return false;
            }
            else{
                if(pnEditMode == EditMode.ADDNEW) {
//                    updateMCAR();
                }
                
                pnEditMode = EditMode.READY;
                poData = loResult;
                if(!pbWithParnt) poGRider.commitTrans();
                
                setMessage("Transaction saved successfully...");
                return true;
            }
        }
    }

    public boolean deleteTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            return false;
        }
        else{
            if(!pbWithParnt) poGRider.beginTrans();
 
            boolean lbResult = poCtrl.deleteTransaction(fsTransNox);
            if(lbResult){
                pnEditMode = EditMode.UNKNOWN;
                if(!pbWithParnt) poGRider.commitTrans();
            }
            else
            if(!pbWithParnt) poGRider.rollbackTrans();

            return lbResult;
        }
    }

    public boolean closeTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            setMessage("Edit mode does not allow verification of transaction!");         
            return false;
        }
        else{
            boolean lbResult = poCtrl.closeTransaction(fsTransNox);
            if(lbResult){
                setMessage("Transaction verified successfully!");
                pnEditMode = EditMode.UNKNOWN;
            }
            else
                setMessage(poCtrl.getErrMsg() + poCtrl.getMessage());

            return lbResult;
        }
    }

    public boolean postTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            return false;
        }
        else{
            if(!pbWithParnt) poGRider.beginTrans();

            boolean lbResult = poCtrl.postTransaction(fsTransNox);
            if(lbResult){
                pnEditMode = EditMode.UNKNOWN;
                if(!pbWithParnt) poGRider.commitTrans();
            }
            else
                if(!pbWithParnt) poGRider.rollbackTrans();
             
            return lbResult;
        }
    }
    
    public boolean cancelTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.READY){
            return false;
        }
        else{
            if(!pbWithParnt) poGRider.beginTrans();

            boolean lbResult = poCtrl.cancelTransaction(fsTransNox);
            if(lbResult){
                deleteAllImage();
                pnEditMode = EditMode.UNKNOWN;
                if(!pbWithParnt) poGRider.rollbackTrans();
            }
            else
                if(!pbWithParnt) poGRider.rollbackTrans();
             
            return lbResult;
        }
    }

    public boolean searchMaster(String field, String value){
        if(field.equalsIgnoreCase("sBranchCd")){
            return searchOrigin(field, value);
        }
        else if(field.equalsIgnoreCase("sBranchNm")){
            return searchOrigin(field, value);
        }
        else if(field.equalsIgnoreCase("sDeptIDxx")){
            return searchDepartment(field, value);
        }
        else if(field.equalsIgnoreCase("sDeptName")){
            return searchDepartment(field, value);
        }
        else if(field.equalsIgnoreCase("sEmployID")){
            return searchEmployee(field, value);
        }
        else if(field.equalsIgnoreCase("sCompnyNm")){
            return searchEmployee(field, value);
        }
        else if(field.equalsIgnoreCase("sModuleCd")){
            return searchModule(field, value);
        }
        else if(field.equalsIgnoreCase("sModuleDs")){
            return searchModule(field, value);
        }
        else if(field.equalsIgnoreCase("sFileCode")){
            return searchEDocSysFile(field, value);
        }
        else if(field.equalsIgnoreCase("sBarrcode")){
            return searchEDocSysFile(field, value);
        }
        else{
            setMessage("Invalid search field [" + field + "]  detected!");
            return false;
        }
    }
    
    public boolean searchWithCondition(String fsFieldNm, String fsValue, String fsFilter){
        System.out.println("Inside SearchWithCondition");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }//end: if(fsValue.trim().length() == 0)

        String lsSQL = getSQL_Master();
        if(fsFieldNm.equalsIgnoreCase("sTransNox")){
            String lsPrefix = "";
            if(fsValue.trim().length() <= 0 || fsValue.contains("%"))
                lsPrefix = "";
            else if(fsValue.length() <= 6)
                lsPrefix = psBranchCd + SQLUtil.dateFormat(poGRider.getSysDate(), "yy");
            else if(fsValue.length() <= 8)
                lsPrefix = psBranchCd;

            if(pnEditMode != EditMode.UNKNOWN){
                if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                    setMessage("The same transaction code!");
                    return false;
                }
            }//end: if(pnEditMode != EditMode.UNKNOWN)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox LIKE " + SQLUtil.toSQL(lsPrefix + "%" + fsValue));
        }// end: if(fsFieldNm.equalsIgnoreCase("sTransNox"))
        else if(fsFieldNm.equalsIgnoreCase("sBranchNm")){
            if(pnEditMode != EditMode.UNKNOWN){
                if(fsValue.trim().equalsIgnoreCase((String)getMaster("sBranchNm"))){
                    setMessage("The same branch name!");
                    return false;
                }
            }
                   
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }//end: if(fsFieldNm.equalsIgnoreCase("sTransNox")) - else if(fsFieldNm.equalsIgnoreCase("sClientNm"))=
        
        if(!fsFilter.isEmpty()){
           lsSQL = MiscUtil.addCondition(lsSQL, fsFilter);
        }
        if(psSourceCd != "") lsSQL = MiscUtil.addCondition(lsSQL, "f.sSourceCD = " + SQLUtil.toSQL(psSourceCd));

        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            loRS = loStmt.executeQuery(lsSQL);

            if(!loRS.next())
                setMessage("No Record Found!");
            else{
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "TransNo»Branch»Date»Department»Employee»Module", 
                                                                "sTransNox»sBranchNm»dTransact»sDeptName»sCompnyNm»sModuleDs");
              
                if (loValue != null){
                    lbHasRec = loadTransaction((String) loValue.get("sTransNox"));
                }
            }

            System.out.println("After Execute");

        }//end: try {
        catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
     
    public boolean getMCAccount(String fsBranchCd, String fsDateFrom, String fsDateThru){
        int lnCtr;
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.ADDNEW){
            return false;
        }
        else{
            String lsSQL = "SELECT" +
                                "  a.sAcctNmbr" +
                                ", a.dPurchase" +
                                ", b.sCompnyNm" +
                                ", c.sBranchNm" +
                            " FROM MC_AR_Master a" +
                                ", Client_Master b" +
                                ", Branch c" +
                                ", MC_SO_Master d" +
                                ", MC_SO_Detail e" +
                            " WHERE a.sClientID = b.sClientID" +
                                " AND a.sBranchCd = c.sBranchCd" +
                                " AND LEFT(d.sTransNox, 4) LIKE" + SQLUtil.toSQL(fsBranchCd + '%') +
                                " AND a.dPurchase BETWEEN " + SQLUtil.toSQL(CommonUtils.toDate(fsDateFrom)) +  " AND " + SQLUtil.toSQL(CommonUtils.toDate(fsDateThru)) +
                                " AND a.sEdocScan = '0'" +
                                " AND a.sApplicNo <> ''" +
                                " AND d.sTransNox = e.sTransNox" +
                                " AND d.cTranStat <> '3'" +
                                " AND a.sClientID = d.sClientID" +
                                " AND a.sSerialID = e.sSerialID" +
                            " ORDER BY a.sAcctNmbr";
                                    
            //Create the connection object
            Connection loCon = poGRider.getConnection();

            if(loCon == null){
                setMessage("Invalid connection!");
                return false;
            }

            boolean lbHasRec = false;
            Statement loStmt = null;
            ResultSet loRS = null;

            try {
                System.out.println("Before Execute");

                loStmt = loCon.createStatement();
                System.out.println(lsSQL);
                
                loRS = loStmt.executeQuery(lsSQL);
                
                if(MiscUtil.RecordCount(loRS)==0){
                    setMessage("Empty record retrieved!!! Please input other search critera....");
                    return false;
                }else{
                    poData.getDetail().clear();
                    addDetail();
                    
                    while (loRS.next()) {
                        setDetail(poData.ItemCount() -1,"sAcctNmbr", loRS.getString("sAcctNmbr"));
                        setDetail(poData.ItemCount() -1,"sCompnyNm", loRS.getString("sCompnyNm"));
                        setDetail(poData.ItemCount() -1,"dPurchase", loRS.getDate("dPurchase"));
                        setDetail(poData.ItemCount() -1,"sSourceCd", "MCAR");
                        setDetail(poData.ItemCount() -1,"sSourceNo", loRS.getString("sAcctNmbr"));
                        if(!loRS.isLast()){
                            addDetail();
                        }
                    }
                }
                                           
                System.out.println("After Execute");

            } catch (SQLException ex) {
                ex.printStackTrace();
                setMessage(ex.getMessage());
            }
            finally{
                MiscUtil.close(loRS);
                MiscUtil.close(loStmt);
            }

            return true;
        }
    }
    
    public boolean getCRInventory(String fsBranchCd, String fsNameFrom, String fsNameThru, String fsYear) {
        int lnCtr;
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.ADDNEW){
            return false;
        }
        else{
            String lsSQL = "SELECT" +
                                "  c.sBranchNm" +
                                ", d.sCompnyNm" +
                                ", a.sCRNoxxxx" +
                                ", b.sEngineNo" +
                                ", e.dPurchase" +
                                ", a.nYearModl" +
                                ", e.sSerialID" +
                            " FROM MC_Serial_Registration a" +
                                ", MC_Serial b" +
                                ", Branch c" +
                                ", Client_Master d" +
                                ", (SELECT * FROM" +
                                    " (SELECT * FROM MC_Registration aa" +
                                        " WHERE YEAR(aa.dPurchase) = " + SQLUtil.toSQL(fsYear) +
                                        " ORDER BY aa.dPurchase) bb" +
                                        " GROUP BY bb.sSerialID) e" +
                            " WHERE a.sSerialID = b.sSerialID" +
                                " AND a.sCRNoxxxx <> ''" +
                                " AND b.sBranchCd = c.sBranchCd" +
                                " AND b.sClientID = d.sClientID" +
                                " AND YEAR(e.dPurchase) = " + SQLUtil.toSQL(fsYear) +
                                " AND d.sCompnyNm BETWEEN " + SQLUtil.toSQL(fsNameFrom) +  
                                    " AND " + SQLUtil.toSQL(fsNameThru+"_") +  
                                " AND b.sBranchCd = " + SQLUtil.toSQL(fsBranchCd) +
                                " AND a.sSerialID = e.sSerialID" +
                                " AND a.cScannedx IN ('0')" +
                                " AND a.sLocatnCR = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                            " ORDER BY b.sBranchCd" +
                                ", d.sCompnyNm" +
                                ", b.sEngineNo";
                                    
            //Create the connection object
            Connection loCon = poGRider.getConnection();

            if(loCon == null){
                setMessage("Invalid connection!");
                return false;
            }

            boolean lbHasRec = false;
            Statement loStmt = null;
            ResultSet loRS = null;

            try {
                System.out.println("Before Execute");

                loStmt = loCon.createStatement();
                System.out.println(lsSQL);
                
                loRS = loStmt.executeQuery(lsSQL);
                
                if(MiscUtil.RecordCount(loRS)==0){
                    setMessage("Empty record retrieved!!! Please input other search critera....");
                    return false;
                }else{
                    poData.getDetail().clear();
                    addDetail();
                    
                    while (loRS.next()) {
                        setDetail(poData.ItemCount() -1,"sCompnyNm", loRS.getString("sCompnyNm"));
                        setDetail(poData.ItemCount() -1,"sEngineNo", loRS.getString("sEngineNo"));
                        setDetail(poData.ItemCount() -1,"sCRNoxxxx", loRS.getString("sCRNoxxxx"));
                        setDetail(poData.ItemCount() -1,"sSourceCd", "MCRg");
                        setDetail(poData.ItemCount() -1,"sSourceNo", loRS.getString("sSerialID"));
                        if(!loRS.isLast()){
                            addDetail();
                        }
                    }
                }
                                           
                System.out.println("After Execute");

            } catch (SQLException ex) {
                ex.printStackTrace();
                setMessage(ex.getMessage());
            }
            finally{
                MiscUtil.close(loRS);
                MiscUtil.close(loStmt);
            }

            return true;
        }
    }
    
    public String getDefaultPath() {
        if(poCtrl == null){
            return "";
        }
        else{
            String lsSQL = "SELECT" +
                                "  sValuexxx" +
                            " FROM xxxOtherConfig" +
                            " WHERE sProdctID = " + SQLUtil.toSQL(poGRider.getProductID()) +
                                " AND sConfigID = " + SQLUtil.toSQL("EDocx");
                                    
            System.out.println(lsSQL);
            //Create the connection object
            Connection loCon = poGRider.getConnection();

            if(loCon == null){
                setMessage("Invalid connection!");
                return "";
            }

            boolean lbHasRec = false;
            Statement loStmt = null;
            ResultSet loRS = null;

            try {
                System.out.println("Before Execute");

                loStmt = loCon.createStatement();
                System.out.println(lsSQL);
                
                loRS = loStmt.executeQuery(lsSQL);
                
                if(MiscUtil.RecordCount(loRS)==0){
                    return "";
                }else{    
                    loRS.first();
                    return loRS.getString("sValuexxx");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                setMessage(ex.getMessage());
            }
            finally{
                MiscUtil.close(loRS);
                MiscUtil.close(loStmt);
            }

            return "";
        }
    }
    
    private boolean searchBranch(String fsFieldNm, String fsValue){
        System.out.println("Inside searchBranch");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Branch();
        if(fsFieldNm.equalsIgnoreCase("sBranchNm")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Branch Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sBranchCd"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Branch Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd LIKE " + SQLUtil.toSQL(fsValue));
        }
         
        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
                
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS,
                                                                       "Code»Description",
                                                                       "sBranchCd»sBranchNm");
              
                if (loValue != null){
                    setBranch((String) loValue.get("sBranchCd"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    private boolean searchModule(String fsFieldNm, String fsValue){
        System.out.println("Inside searchModule");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Module();
        if(fsFieldNm.equalsIgnoreCase("sModuleDs")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Module Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sModuleDs LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sModuleCd"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Module Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sModuleCd LIKE " + SQLUtil.toSQL(fsValue));
        }
         
        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
                
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS,
                                                                       "Code»Description",
                                                                       "sModuleCd»sModuleDs");
              
                if (loValue != null){
                    setModule((String) loValue.get("sModuleCd"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    private boolean searchOrigin(String fsFieldNm, String fsValue){
        System.out.println("Inside searchOrigin");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Branch();
        if(fsFieldNm.equalsIgnoreCase("sBranchNm")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Branch Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sBranchCd"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Branch Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd LIKE " + SQLUtil.toSQL(fsValue));
        }
         
        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Code»Description", 
                                                                "sBranchCd»sBranchNm");
              
                if (loValue != null){
                    setOrigin((String) loValue.get("sBranchCd"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    private boolean searchDepartment(String fsFieldNm, String fsValue){
        System.out.println("Inside searchDeparment");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Department();
        if(fsFieldNm.equalsIgnoreCase("sDeptName")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Department Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptName LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sDeptIDxx"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Department Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx LIKE " + SQLUtil.toSQL(fsValue));
        }

        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Code»Department", 
                                                                "sDeptIDxx»sDeptName");
              
                if (loValue != null){
                    setDepartment((String) loValue.get("sDeptIDxx"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
   
    private boolean searchEmployee(String fsFieldNm, String fsValue){
        System.out.println("Inside searchEmployee");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_Employee();
        if(fsFieldNm.equalsIgnoreCase("sCompnyNm")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Empoyee Name!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sEmployID"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Employee Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sEmployID LIKE " + SQLUtil.toSQL(fsValue));
        }

        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);

            if(loRS.next()){
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Code»Employee", 
                                                                "sEmployID»sCompnyNm");
              
                if (loValue != null){
                    setEmployee((String) loValue.get("sEmployID"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    private boolean searchEDocSysFile(String fsFieldNm, String fsValue){
        System.out.println("Inside searchModule");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }

        String lsSQL = getSQL_EDocSysFile();
        if(fsFieldNm.equalsIgnoreCase("sBarrcode")){
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Barcode!");                
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sBarrcode LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sFileCode"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same File Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "sFileCode LIKE " + SQLUtil.toSQL(fsValue));
        }

        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);
            loRS = loStmt.executeQuery(lsSQL);
            if(!loRS.next()){
                setMessage("No record found...");
            }else{
                JSONObject loValue = showFXDialog.jsonBrowse(poGRider, loRS, 
                                                                "Code»Barcode»Description", 
                                                                "sFileCode»sBarrcode»sBriefDsc");
              
                if (loValue != null){
                    setEDocSysFile((String) loValue.get("sFileCode"));
                    lbHasRec = true;
                }
            }

            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return lbHasRec;
    }
    
    public XMBranch getBranch(){
        if(poBranch == null)
            poBranch = new XMBranch(poGRider, psBranchCd, true);

        poBranch.openRecord(psBranchCd);
        return poBranch;
    }
     
    public XMBranch getOrigin(){
        if(poOrigin == null)
            poOrigin = new XMBranch(poGRider, psOriginxx, true);
        
        poOrigin.openRecord(psOriginxx);
        return poOrigin;
    }
    
    public XMDepartment getDepartment(){
        if(poDepartment == null)
            poDepartment = new XMDepartment(poGRider, psBranchCd, true);
        poDepartment.openRecord(psDeptIDxx);
        return poDepartment;
    }
    
    public XMBarcode getFileCode(){
        if(poEDocSysFile == null)
            poEDocSysFile = new XMBarcode(poGRider, psFileCode, true);

        poEDocSysFile.openRecord(psFileCode);
        return poEDocSysFile;
    }
    
    public XMModule getModuleCode(){
        if(poModule == null)
            poModule = new XMModule(poGRider, psModuleCd, true);

        poModule.openRecord(psModuleCd);
        return poModule;
    }
    
    public XMEmployee getEmployee(){
        if(poEmployee == null)
            poEmployee = new XMEmployee(poGRider, psEmployID, true);

        poEmployee.openRecord(psEmployID);
        return poEmployee;
    }

    public void setBranch(String fsBranchCD) {
        psBranchCd = fsBranchCD;
        poCtrl.setBranch(fsBranchCD);
    }
    
    public void setOrigin(String fsOriginxx) {
        psOriginxx = fsOriginxx;
        poCtrl.setOrigin(fsOriginxx);
    }
    
    public void setDepartment(String fsDeptIDxx) {
        psDeptIDxx = fsDeptIDxx;
        poCtrl.setDepartment(fsDeptIDxx);
    }
    
    public void setEmployee(String fsEmployID) {
        psEmployID = fsEmployID;
        poCtrl.setEmployee(fsEmployID);
    }
    
    public void setEDocSysFile(String fsFileCode) {
        psFileCode = fsFileCode;
        poCtrl.setFileCd(fsFileCode);
    }
    
     public void setModule(String fsModuleCD) {
        psModuleCd = fsModuleCD;
        poCtrl.setModule(fsModuleCD);
    }

    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
        poCtrl.setGRider(foGRider);
    }
    
    public void setSourceCD(String fsSourceCd){
        this.psSourceCd = fsSourceCd;
    }

    public int getEditMode() {
        return pnEditMode;
    }
    
    public int ItemCount(){
        return poData.ItemCount();
    }
    
    public int MasterFileCount(){
        return poData.MasterFileItemCount();
    }
    
    private String getSQL_Master(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", b.sBranchNm" +
                    ", a.dTransact" +
                    ", c.sDeptName" +
                    ", d.sCompnyNm" +
                    ", e.sModuleDs" +
                " FROM EDocSys_Master a" +
                    " LEFT JOIN Department c" +
                        " ON a.sDeptIDxx = c.sDeptIDxx" +
                    " LEFT JOIN Client_Master d" +
                        " ON a.sEmployID = d.sClientID" +
                    " LEFT JOIN EDocSys_Module e" +
                        " ON a.sModuleCd = e.sModuleCd" +
                    ", Branch b" +
                    ", EDocSys_Detail f" +
                " WHERE a.sBranchCd = b.sBranchCd" +
                    " AND a.sTransNox = f.sTransNox" +
                " GROUP BY a.sTransNox" +
                " ORDER BY a.sTransNox";
    }
    
    private String getSQL_Branch(){
        return "SELECT" +
                    "  sBranchCd" +
                    ", sBranchNm" +
                " FROM Branch" +
                " WHERE cRecdStat = '1'" +
                    " AND LEFT(sBranchCd, 1) IN('M','G')";
    }
    
    private String getSQL_Department(){
        return "SELECT" +
                    "  sDeptIDxx" +
                    ", sDeptName" +
                " FROM Department" +
                " WHERE cRecdStat = '1'";
    }

    private String getSQL_Employee(){
        return "SELECT" +
                    "  a.sEmployID" +
                    ", b.sCompnyNm" +
                " FROM Employee_Master001 a" +
                    ", Client_Master b" +
                " WHERE a.sEmployID = b.sClientID" +
                    " AND a.cRecdStat = '1'";
    }
    
    private String getSQL_EDocSysFile(){
        return "SELECT" +
                    "  sFileCode" +
                    ", sBarrcode" +
                    ", sBriefDsc" +
                    ", nNoPagesx" +
                    ", nNoCopies" +
                    ", sB2BPagex" +
                " FROM EDocSys_File";
    }
    private String getSQL_Module(){
        return "SELECT" +
                    "  sModuleCd" +
                    ", sModuleDs" +
                " FROM EDocSys_Module" +
                " WHERE cRecdStat = '1'";
    }
    
    private void loadMCAccount(){
        String lsSQL = "SELECT" +
                            "  a.sAcctNmbr" +
                            ", a.dPurchase" +
                            ", b.sCompnyNm" +
                            ", c.sBranchNm" +
                            " FROM MC_AR_Master a" +
                                ", Client_Master b" +
                                ", Branch c" +
                            " WHERE a.sClientID = b.sClientID" +
                                " AND a.sBranchCd = c.sBranchCd" +
                                " AND e.sTransNox =  " + SQLUtil.toSQL(poData.getMaster().getTransNox()) +
                            " ORDER BY e.nEntryNox";
        
        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;
        
        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);

            loRS = loStmt.executeQuery(lsSQL);  
            
            poData.getDetail().clear();
            addDetail();  
            
            if(MiscUtil.RecordCount(loRS)==0){
                setMessage("Empty record retrieved!!!");
            }else{  
                while (loRS.next()) {
                    setDetail(poData.ItemCount()-1,"sSerialID", loRS.getString("sSerialID"));
                    setDetail(poData.ItemCount()-1,"sClientNm", loRS.getString("sCompnyNm"));
                    setDetail(poData.ItemCount()-1,"sCRNoxxxx", loRS.getString("sCRNoxxxx"));
                    setDetail(poData.ItemCount()-1,"sEngineNo", loRS.getString("sEngineNo"));
                    
                    if(!loRS.isLast()){
                        addDetail();
                    }
                }
            }
                                           
            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }
    }
    
    private void loadRegistration(){
        String lsSQL = "SELECT" +
                            "  a.sAcctNmbr" +
                            ", a.dPurchase" +
                            ", b.sCompnyNm" +
                            ", c.sBranchNm" +
                            " FROM MC_AR_Master a" +
                                ", Client_Master b" +
                                ", Branch c" +
                            " WHERE a.sClientID = b.sClientID" +
                                " AND a.sBranchCd = c.sBranchCd" +
                                " AND e.sTransNox =  " + SQLUtil.toSQL(poData.getMaster().getTransNox()) +
                            " ORDER BY e.nEntryNox";
        
        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
        }

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;
        
        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.out.println(lsSQL);

            loRS = loStmt.executeQuery(lsSQL);  
            
            poData.getDetail().clear();
            addDetail();  
            
            if(MiscUtil.RecordCount(loRS)==0){
                setMessage("Empty record retrieved!!!");
            }else{  
                while (loRS.next()) {
                    setDetail(poData.ItemCount()-1,"sSerialID", loRS.getString("sSerialID"));
                    setDetail(poData.ItemCount()-1,"sClientNm", loRS.getString("sCompnyNm"));
                    setDetail(poData.ItemCount()-1,"sCRNoxxxx", loRS.getString("sCRNoxxxx"));
                    setDetail(poData.ItemCount()-1,"sEngineNo", loRS.getString("sEngineNo"));
                    
                    if(!loRS.isLast()){
                        addDetail();
                    }
                }
            }
                                           
            System.out.println("After Execute");

        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }
    }
     
    private void updateMCAR(){
        for(int lnCtr=0;lnCtr<=poData.getMasterFile().size() -1;lnCtr++) {
            String lsSQL = "UPDATE MC_AR_Master SET" +
                                " sEdocScan = sEdocScan + 1" +
                            " WHERE sAcctNmbr = " + SQLUtil.toSQL(poData.getDetail().get(lnCtr).getAcctNumber());                   
            
            poGRider.executeQuery(lsSQL, "", "", "");
        }
    }
    
    public void setScan(FiscnApp fsScan){
        this.poScan= fsScan;
    }
    
    public void moveMasterFile(int fromPosition, int toPosition){
        String lsTempFileName;
        Integer lnTempEntryNox;
        
        if(getMasterFile(fromPosition, "nEntryNox") != getMasterFile(toPosition, "nEntryNox")){
            setMasterFile(fromPosition, "nEntryNox",getMasterFile(toPosition, "nEntryNox"));
        }else{      
            lsTempFileName=(String)getMasterFile(toPosition, "sFileName");
            lnTempEntryNox=(Integer)getMasterFile(toPosition, "nEntryNox");

            setMasterFile(toPosition, "sFileName", getMasterFile(fromPosition, "sFileName"));
            setMasterFile(toPosition, "nEntryNox", getMasterFile(toPosition, "nEntryNox"));

            setMasterFile(fromPosition, "sFileName", lsTempFileName);
            setMasterFile(fromPosition, "nEntryNox", lnTempEntryNox);
        }
    }
    
    public String getMessage(){return psMessage;}
    private void setMessage(String fsValue){psMessage = fsValue;}
  
    private XMBranch poBranch = null;
    private XMBranch poOrigin = null;
    private XMDepartment poDepartment = null;
    private XMBarcode poEDocSysFile = null;
    private XMEmployee poEmployee = null;
    private XMModule poModule = null;

    private UnitEDocuments poData;
    private EDocuments poCtrl;
    private GRider poGRider;
    private FiscnApp poScan;
    private String psSourceCd = "";

    private int pnEditMode;
    private String psBranchCd;
    private String psOriginxx;
    private String psDeptIDxx;
    private String psEmployID;
    private String psFileCode;
    private String psMessage;
    private String psModuleCd;
    SimpleDateFormat psDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private boolean pbWithParnt = false;
}