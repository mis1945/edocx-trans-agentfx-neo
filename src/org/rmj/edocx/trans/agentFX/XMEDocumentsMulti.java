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
import org.rmj.edocx.trans.pojo.UnitEDocumentsMulti;
import org.rmj.parameters.agent.XMBranch;
import org.rmj.parameters.agent.XMDepartment;
import org.rmj.client.agent.XMEmployee;
import org.rmj.appdriver.MiscUtil;
import java.text.SimpleDateFormat;
import org.rmj.appdriver.agentfx.StringHelper;
import org.rmj.parameters.agent.XMModule;
import com.fujitsu.pfu.fiscn.sdk.FiscnApp;
import com.fujitsu.pfu.fiscn.sdk.FiscnException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.edocx.trans.EDocumentsMulti;
import org.rmj.edocx.trans.pojo.UnitMasterFile;
import org.rmj.edocx.trans.pojo.UnitSysFile;

/**
 *
 * @author jef
 */
public class XMEDocumentsMulti {
    public XMEDocumentsMulti(GRider foGRider, String fsBranchCd, boolean fbWithParent) throws FiscnException{
        this.poGRider = foGRider;
        if(foGRider != null){
            this.pbWithParnt = fbWithParent;
            this.psBranchCd = fsBranchCd;
            poCtrl = new EDocumentsMulti();
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
        String lsFileName="";
        String lsCompTerminal ="";
        int lnRow =0;
//        if (poScan.gets == true){
//            TRANSNOX-FILECODE-0001 = XXXXXXXXXXXX-XXXX-XXXX-????.JPG
//            lsFileName = getMaster("sTransNox").toString() + '-';
        lsCompTerminal = System.getProperty("computer.terminal");
        String lsSQL="SELECT"+
                            " sValuexxx"+
                        " FROM xxxOtherConfig"+
                        " WHERE sProdctID = " + SQLUtil.toSQL(poGRider.getProductID())+
                            " AND sConfigID= " + SQLUtil.toSQL(getSourceCD());      
        
        //Create the connection object
        Connection loCon = poGRider.getConnection();

        if(loCon == null){
            setMessage("Invalid connection!");
            return false;
        }

        Statement loStmt = null;
        ResultSet loRS = null;

        try {
            System.out.println("Before Execute");

            loStmt = loCon.createStatement();
            System.err.println(lsSQL);

            loRS = loStmt.executeQuery(lsSQL);
            
            if(MiscUtil.RecordCount(loRS)==0){
                lsFileName = getMaster("sTransNox").toString() + '-';
            }else{
                while(loRS.next()){
                    lsFileName =  getMaster("sTransNox").toString()+loRS.getString("sValuexxx").toString() + '-';
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            setMessage(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }
            
        if(poScan.scan(getDefaultPath(), lsFileName+lsCompTerminal,poData.MasterFileItemCount()+1)==true){
//                poData.getMasterFile().clear();
//                if((lnTotalPage * ItemCount()) != poScan.imageCount){
//                    if(ShowMessageFX.YesNo("Do you want to continue?", "Confirmation", "Total number of scanned pages mismatch! Docs scanned: "+poScan.imageCount+"\n Supposed Value: "+lnTotalPage * ItemCount())== false){
//                         for(int lnCtr=0;lnCtr<= poScan.imageCount;lnCtr++) {
//                             deleteImage(lsFileName+(String) StringHelper.prepad(String.valueOf(lnCtr+1), 4, '0'));
//                         }
//                        return false;
//                    }
//                }

            lnRow= poData.MasterFileItemCount();
            for(int lnCtr=0;lnCtr<= poScan.imageCount -1;lnCtr++) {
                addMasterFile();
                setMasterFile(lnCtr+lnRow,"sTransNox", getMaster("sTransNox"));
                setMasterFile(lnCtr+lnRow,"sFileName", lsFileName+lsCompTerminal+ (String) StringHelper.prepad(String.valueOf((lnCtr+lnRow)+1), 4, '0') );
            }
            validate_ScanFile();
        }else{
            setMessage(poScan.getErrMessage());
            return false;
        }
        return true;
    }
    
    public void validate_ScanFile(){        
        Integer lnB2BPagexx = 0;
        Integer lnPageNo=0;
        Integer lnFileNo=0;
        Integer lnTotlPg=0;
        Integer lnClient=0;
        Integer lnFilePg=0;    
        String lsValue = "";
        
        for(int lnCtr=0;lnCtr<= poData.SysFileItemCount()-1;lnCtr++) {
            lsValue = (String) getSysFile(lnCtr,"sB2BPagex");
            String [] arr = lsValue.split("»");
            lnB2BPagexx =0;
            for(int nCtr = 0; nCtr<= arr.length -1 ; nCtr++){
                lnB2BPagexx=lnB2BPagexx+Integer.parseInt(arr[nCtr]);
            }
            lnTotlPg = lnTotlPg+((int) getSysFile(lnCtr, "nNoPagesx") *(int) getSysFile(lnCtr, "nNoCopies"))+lnB2BPagexx;
        }
        
        for(int lnCtr=0;lnCtr<= MasterFileCount()-1;lnCtr++) {
            if(lnFileNo<=poData.SysFileItemCount()-1){
                lnPageNo=lnPageNo+1;  
            }
            
            setMasterFile(lnCtr,"nFileNoxx",lnFileNo+1);
            setMasterFile(lnCtr,"nEntryNox",lnPageNo);
            setMasterFile(lnCtr,"nClientNo",lnClient+1);

            lsValue = (String) getSysFile(lnFileNo,"sB2BPagex");
            String [] arr = lsValue.split("»");

            lnB2BPagexx=0;
            for(int nCtr = 0; nCtr<= arr.length -1; nCtr++){
                lnB2BPagexx=lnB2BPagexx+Integer.parseInt(arr[nCtr]);
            }
            
            lnFilePg = ((int) getSysFile(lnFileNo, "nNoPagesx") *(int) getSysFile(lnFileNo, "nNoCopies"))+lnB2BPagexx;
                
            if(lnFileNo<poData.SysFileItemCount()-1){
                if(lnPageNo==lnFilePg){
                    lnPageNo=0;
                    lnFileNo=lnFileNo+1;
                }
            }else{
                if(poData.ItemCount()>1){
                    if(lnPageNo==lnFilePg){
                        lnPageNo=0;
                        lnFileNo=0;}
                }
            }
            
            if(lnClient<poData.ItemCount()-1){
                if((lnClient+1)*lnTotlPg<=(lnCtr+1)){
                    lnClient=lnClient+1;
                }
            }
        }
        for(int x = 0; x<= MasterFileCount() -1; x++){
            System.err.println(getMasterFile(x, "nFileNoxx")+" "+getMasterFile(x, "nClientNo")+" "+getMasterFile(x, "nEntryNox")+" "+getMasterFile(x, "sFileName"));
        }
    }
    
    public void sample_fileSingle(){
        String lsFileName;
        Integer lnFileNox = 0;
        Integer lnTotalPage = 0;
        Integer lnB2BPagexx = 0;
        Integer lnEntryNox = 1;
        Integer lnClientNo = 0;
        Integer lnComparex = 0;
        String lsValue = "";
                    
            lsFileName = getMaster("sTransNox").toString() + '-' + psFileCode + '-';
                
                int lnRow =0;
                for(int lnCtr=0;lnCtr<= 50-1 ;lnCtr++) {
                    addMasterFile();
                    
                    setMasterFile(lnCtr+lnRow,"sTransNox", getMaster("sTransNox"));
                    setMasterFile(lnCtr+lnRow,"sFileName", lsFileName+ (String) StringHelper.prepad(String.valueOf((lnCtr+lnRow)+1), 4, '0') );
                    
                    if(lnFileNox<=poData.SysFileItemCount()) {
                        if(lnTotalPage==0){
                            if(lnFileNox<poData.SysFileItemCount()){
                                lsValue = (String) getSysFile(lnFileNox,"sB2BPagex");
                                String [] arr = lsValue.split("»");

                                for(int nCtr = 0; nCtr< arr.length ; nCtr++){
                                    lnB2BPagexx=lnB2BPagexx+Integer.parseInt(arr[nCtr]);
                                }
                                lnTotalPage = ((int) getSysFile(lnFileNox, "nNoPagesx") *(int) getSysFile(lnFileNox, "nNoCopies"))+lnB2BPagexx;
                                lnFileNox=lnFileNox+1;
                                lnEntryNox=1;
                            }else {
                                lnEntryNox=lnEntryNox+1;
                            }
                        }
                    } else {
                        lnEntryNox=lnEntryNox+1;
                    }
                    
                    if(lnEntryNox<=lnTotalPage){
                        if(lnTotalPage==lnEntryNox){
                            lnFileNox=lnFileNox+1;
                            lnTotalPage=0;
                        }
                        
                        if(lnFileNox+1<=poData.SysFileItemCount()) {
                            lnFileNox=lnFileNox+1;
                        }else{
                            lnFileNox=poData.SysFileItemCount();
                        }
                        lnEntryNox=lnEntryNox+1;
                    }
                    
                    setMasterFile(lnCtr+lnRow,"nFileNoxx",lnFileNox);
                    setMasterFile(lnCtr+lnRow,"nEntryNox",lnEntryNox);
                    setMasterFile(lnCtr+lnRow,"nClientNo",lnClientNo);
                    
//                    lnComparex=lnComparex+1;
                }
                for(int x= 0; x <= MasterFileCount() -1; x++){
                    System.out.println(getMasterFile(x, "nFileNoxx")+" "+ getMasterFile(x, "nEntryNox") + " " + getMasterFile(x, "sFileName"));
                }
            }
        
    
    public void sample_fileMultiple(){
        boolean isClicked = false;
        String lsFileName;
        
//        if (poScan.gets == true){
//            TRANSNOX-FILECODE-0001 = XXXXXXXXXXXX-XXXX-XXXX-????.JPG
            lsFileName = getMaster("sTransNox").toString() + '-' + 0001 + '-';
//            if(poScan.scan(getDefaultPath(), lsFileName,poData.MasterFileItemCount()+1)==true){

        int lnRow = 0;
        
        lnRow = 6;
        for(int lnCtr=0;lnCtr<= lnRow -1;lnCtr++) {
            addMasterFile();
            setMasterFile(lnCtr+lnRow,"sTransNox", getMaster("sTransNox"));
            setMasterFile(lnCtr+lnRow,"sFileName", lsFileName+ (String) StringHelper.prepad(String.valueOf((lnCtr+lnRow)+1), 4, '0') );            
        }
        validate_ScanFile();
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
    
    public Object getSysFile(int row, int col){
        if(pnEditMode == EditMode.UNKNOWN || poCtrl == null)
            return null;
        else if(row < 0 || row >= poData.getSysFile().size())
            return null;
      
        return poData.getSysFile().get(row).getValue(col);
    }
    
    public Object getSysFile(int row, String fsCol){
        return getSysFile(row, poData.getSysFile().get(row).getColumn(fsCol));
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
    
    public void setSysFile(int row, String fsCol, Object value){
        setSysFile(row, poData.getSysFile().get(row).getColumn(fsCol), value);
    }

    public void setSysFile(int row, int col, Object value){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(row >= 0 && row <= poData.getSysFile().size()){
                if(row == poData.getSysFile().size()){
                    poData.getSysFile().add(new UnitSysFile());
                }
                
                poData.getSysFile().get(row).setValue(col, value);
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
    
    public void addSysFile(){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            poData.getSysFile().add(new UnitSysFile());
        }
    }
    
    public void deleteDetail(int row){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(!(row < 0 || row >= poData.getDetail().size())){
                poData.getDetail().remove(row);
            }
        }
    }
    
    public void deleteDetailS(String rows){
        String lsValue = rows;
        String [] arr = lsValue.split("»");
        
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(arr.length==0){
                deleteDetail(Integer.valueOf(rows) -1);
            }else{
                for(int lnCtr = 0; lnCtr<= arr.length-1 ; lnCtr++){
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
    
    public void deleteSysFile(int row){
        if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
            if(!(row < 0 || row >= poData.getSysFile().size())){
                poData.getSysFile().remove(row);
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
        
        poData = (UnitEDocumentsMulti) poCtrl.newTransaction();

        if(poData == null){
            return false;
        }
        else{
            //set the values of foreign key(object) to null
            poBranch = null;
            poData.getMasterFile().clear();
            poData.getDetail().clear();
            poData.getSysFile().clear();
            
            pnEditMode = EditMode.ADDNEW;
            addDetail();
            addSysFile();
            
            return true;
        }
    }

    public boolean loadTransaction(String fsTransNox) {
        if(poCtrl == null){
            return false;
        }
        
        poData = (UnitEDocumentsMulti) poCtrl.loadTransaction(fsTransNox,psSourceCd);
       
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
    
    public String getSourceCD(){
        return psSourceCd;
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
             
            UnitEDocumentsMulti loResult=null;
            setMaster("nEntryNox", poData.MasterFileItemCount());
            if(pnEditMode == EditMode.ADDNEW){
                poCtrl.setSourceCD(getSourceCD());
                loResult = (UnitEDocumentsMulti) poCtrl.saveUpdate(poData, "");
            }else{
                poCtrl.setSourceCD(getSourceCD());
                loResult = (UnitEDocumentsMulti) poCtrl.saveUpdate(poData, (String) poData.getMaster().getValue(1));
            }
            
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
                
                if(getSourceCD().equals("RExp")) sendEmail();
                
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
        if(getSourceCD() != "") lsSQL = MiscUtil.addCondition(lsSQL, "f.sSourceCD = " + SQLUtil.toSQL(psSourceCd));

        System.out.println(lsSQL);

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
    
    public boolean searchSysFile(int row, String fsFieldNm, String fsValue){
      System.out.println("Inside searchSysFile");
      fsValue = fsValue.trim();
      if(fsValue.trim().length() == 0){
         System.out.println("Nothing to process!");
         return false;
      }

      String lsSQL = getSQL_EDocSysFile();
      if(fsFieldNm.equalsIgnoreCase("sBarrCode")){
         if(getSysFile(row, "sBarrCode") != null){  
            if(fsValue.trim().equalsIgnoreCase((String)getSysFile(row, "sBarrCode"))){
                System.out.println("The same Barcode!");
                return false;
            }
         }
         lsSQL = MiscUtil.addCondition(lsSQL, "sBarrCode LIKE " + SQLUtil.toSQL(fsValue));
      }
      else if(fsFieldNm.equalsIgnoreCase("sBriefDsc"))
      {
         if(getSysFile(row, "sBriefDsc") != null){  
             if(fsValue.trim().equalsIgnoreCase((String)getSysFile(row, "sBriefDsc"))){
                System.out.println("The same Description Code!");
                return false;
             }
         }
         lsSQL = MiscUtil.addCondition(lsSQL, "sBriefDsc LIKE " + SQLUtil.toSQL(fsValue));
      }
      
      //Create the connection object
      Connection loCon = poGRider.getConnection();

      if(loCon == null){
         System.out.println("Invalid connection!");
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
                                                                "Code»Barcode»Description", 
                                                                "sFileCode»sBarrcode»sBriefDsc");
             if (loValue != null){
                    poData.getSysFile().get(row).setFileCode(loRS.getString("sFileCode"));
                    poData.getSysFile().get(row).setBarCode(loRS.getString("sBarrcode"));
                    poData.getSysFile().get(row).setBriefDsc(loRS.getString("sBriefDsc"));
                    poData.getSysFile().get(row).setNoCopies(loRS.getInt("nNoCopies"));
                    poData.getSysFile().get(row).setNoPages(loRS.getInt("nNoPagesx"));
                    poData.getSysFile().get(row).setB2BPagex(loRS.getString("sB2BPagex"));
                    lbHasRec = true;
                }
             
//            poEDocSysFile.set(row, new XMBarcode(poGRider, psBranchCd, true)); 
//            poEDocSysFile.get(row).openRecord(loRS.getString("sFileCode"));
//            lbHasRec = true;
         }

         System.out.println("After Execute");

      } catch (SQLException ex) {
         ex.printStackTrace();
         System.out.println(ex.getMessage());
      }
      finally{
         MiscUtil.close(loRS);
         MiscUtil.close(loStmt);
      }

      return lbHasRec;
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
                                ", d.sClientID" +
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
                                " AND a.cScannedx IN ('0','')" +
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
                System.err.println(lsSQL);
                
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
                        setDetail(poData.ItemCount() -1,"sClientID", loRS.getString("sClientID"));
                        setDetail(poData.ItemCount() -1,"sSerialID", loRS.getString("sSerialID"));
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
                                ", a.sClientID" +
                                ", a.sSerialID" +
                            " FROM MC_AR_Master a" +
                                ", Client_Master b" +
                                ", Branch c" +
                                ", MC_SO_Master d" +
                                ", MC_SO_Detail e" +
                            " WHERE a.sClientID = b.sClientID" +
                                " AND a.sBranchCd = c.sBranchCd" +
                                " AND LEFT(d.sTransNox, 4) LIKE" + SQLUtil.toSQL(fsBranchCd + '%') +
                                " AND a.dPurchase BETWEEN " + SQLUtil.toSQL(CommonUtils.toDate(fsDateFrom)) +  " AND " + SQLUtil.toSQL(CommonUtils.toDate(fsDateThru)) +
                                " AND a.sEdocScan IN('0','')" +
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
                        setDetail(poData.ItemCount() -1,"sClientID", loRS.getString("sClientID"));
                        setDetail(poData.ItemCount() -1,"sSerialID", loRS.getString("sSerialID"));
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
    
    public boolean getRegisExpense(String fsBatchNox){
        int lnCtr;
        if(poCtrl == null){
            return false;
        }
        else if(pnEditMode != EditMode.ADDNEW){
            return false;
        }
        else{
            String lsSQL = "SELECT" +
                                "  a.sBatchNox" +
                                ", b.sReferNox" +
                                ", d.sCompnyNm" +
                                ", e.sEngineNo" +
                                ", f.sModelNme" +
                                ", d.sClientID" +
                                ", e.sSerialID" +
                            " FROM Registration_Expense_Master a" +
                                ", Registration_Expense_Detail b" +
                                ", MC_Registration_Expense c" +
                                ", Client_Master d" +
                                ", MC_Serial e" +
                                ", MC_Model f" +
                                ", MC_Registration g" +
                            " WHERE a.sBatchNox = " + SQLUtil.toSQL(fsBatchNox) +
                                " AND a.sBatchNox = b.sBatchNox" +
                                " AND b.sReferNox = c.sTransNox" +
                                " AND c.sReferNox = g.sTransNox" +
                                " AND g.sClientID = d.sClientID" +
                                " AND g.sSerialID = e.sSerialID" +
                                " AND e.sModelIDx = f.sModelIDx" +
                                " AND (b.cScannedx IS NULL OR b.cScannedx = '' OR b.cScannedx = '0')" +
                            " ORDER BY d.sCompnyNm";
                                    
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
                        setDetail(poData.ItemCount() -1,"sSourceNo", loRS.getString("sReferNox"));
                        setDetail(poData.ItemCount() -1,"sSourceCd", "RExp");
                        setDetail(poData.ItemCount() -1,"sCompnyNm", loRS.getString("sCompnyNm"));
                        setDetail(poData.ItemCount() -1,"sEngineNo", loRS.getString("sEngineNo"));
                        setDetail(poData.ItemCount() -1,"sModelNme", loRS.getString("sModelNme"));
                        setDetail(poData.ItemCount() -1,"sClientID", loRS.getString("sClientID"));
                        setDetail(poData.ItemCount() -1,"sSerialID", loRS.getString("sSerialID"));
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
                    loadDepartmentFile((String) loValue.get("sDeptIDxx"));
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
    
//    public XMBarcode getFileCode(){
//        if(poEDocSysFile == null)
//            poEDocSysFile = new XMBarcode(poGRider, psFileCode, true);
//
//        poEDocSysFile.openRecord(psFileCode);
//        return poEDocSysFile;
//    }
    
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

    public int getEditMode() {
        return pnEditMode;
    }
    
    public int ItemCount(){
        return poData.ItemCount();
    }
    
    public int MasterFileCount(){
        return poData.MasterFileItemCount();
    }
    
    public int SysFileCount(){
        return poData.SysFileItemCount();
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
                "  a.sDeptIDxx" + 
                ", b.sDeptName" +
                " FROM EDocSys_Department_File a" + 
                " LEFT JOIN Department b" + 
                " ON a.sDeptIDxx = b.sDeptIDxx" +
                " WHERE a.cRecdStat = '1'" +
                " GROUP BY a.sDeptIDxx;";
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
                " FROM EDocSys_File" +
                " WHERE cRecdStat = 1";
    }
    
    private String getSQL_Module(){
        return "SELECT" +
                    "  sModuleCd" +
                    ", sModuleDs" +
                " FROM EDocSys_Module" +
                " WHERE cRecdStat = '1'";
    }
    
//    private void loadMCAccount(){
//        String lsSQL = "SELECT" +
//                            "  a.sAcctNmbr" +
//                            ", a.dPurchase" +
//                            ", b.sCompnyNm" +
//                            ", c.sBranchNm" +
//                            " FROM MC_AR_Master a" +
//                                ", Client_Master b" +
//                                ", Branch c" +
//                            " WHERE a.sClientID = b.sClientID" +
//                                " AND a.sBranchCd = c.sBranchCd" +
//                                " AND e.sTransNox =  " + SQLUtil.toSQL(poData.getMaster().getTransNox()) +
//                            " ORDER BY e.nEntryNox";
//        
//        //Create the connection object
//        Connection loCon = poGRider.getConnection();
//
//        if(loCon == null){
//            setMessage("Invalid connection!");
//        }
//
//        boolean lbHasRec = false;
//        Statement loStmt = null;
//        ResultSet loRS = null;
//        
//        try {
//            System.out.println("Before Execute");
//
//            loStmt = loCon.createStatement();
//            System.out.println(lsSQL);
//
//            loRS = loStmt.executeQuery(lsSQL);  
//            
//            poData.getDetail().clear();
//            addDetail();  
//            
//            if(MiscUtil.RecordCount(loRS)==0){
//                setMessage("Empty record retrieved!!!");
//            }else{  
//                while (loRS.next()) {
//                    setDetail(poData.ItemCount()-1,"sSerialID", loRS.getString("sSerialID"));
//                    setDetail(poData.ItemCount()-1,"sClientNm", loRS.getString("sCompnyNm"));
//                    setDetail(poData.ItemCount()-1,"sCRNoxxxx", loRS.getString("sCRNoxxxx"));
//                    setDetail(poData.ItemCount()-1,"sEngineNo", loRS.getString("sEngineNo"));
//                    
//                    if(!loRS.isLast()){
//                        addDetail();
//                    }
//                }
//            }
//                                           
//            System.out.println("After Execute");
//
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//            setMessage(ex.getMessage());
//        }
//        finally{
//            MiscUtil.close(loRS);
//            MiscUtil.close(loStmt);
//        }
//    }
     
//    private void updateMCAR(){
//        for(int lnCtr=0;lnCtr<=poData.getMasterFile().size() -1;lnCtr++) {
//            String lsSQL = "UPDATE MC_AR_Master SET" +
//                                " sEdocScan = sEdocScan + 1" +
//                            " WHERE sAcctNmbr = " + SQLUtil.toSQL(poData.getDetail().get(lnCtr).getAcctNumber());                   
//            
//            poGRider.executeQuery(lsSQL, "", "", "");
//        }
//    }
    
    public void setScan(FiscnApp fsScan){
        this.poScan= fsScan;
    }
    
    public void moveMasterFile(int fromPosition, int toPosition){
        String lsTempFileName;
        Integer lnTempClientNo;
        Integer lnTempFileNoxx;
//        
//        if(toPosition+1>poData.MasterFileItemCount()) {
//            poData.addMasterFile();
//        }
        lsTempFileName=(String)getMasterFile(toPosition, "sFileName");
        lnTempFileNoxx=(Integer)getMasterFile(toPosition, "nFileNoxx");
        lnTempClientNo=(Integer)getMasterFile(toPosition, "nClientNo");
        
        if(getMasterFile(toPosition, "nFileNoxx")==null){
//            setMasterFile(fromPosition, "nFileNoxx", getMasterFile(toPosition, "nFileNoxx"));
            ShowMessageFX.Warning("For Assistance!", "Multiple Document Scanner", "Please contact MIS/SEG!");
            return;
//            setMasterFile(toPosition, "sFileName", getMasterFile(fromPosition, "sFileName"));
//            setMasterFile(toPosition, "nFileNoxx", getMasterFile(fromPosition, "nFileNoxx"));   
        }
                    
        if(getMasterFile(fromPosition, "nClientNo") != getMasterFile(toPosition, "nClientNo")){
            setMasterFile(fromPosition, "nFileNoxx", getMasterFile(toPosition, "nFileNoxx"));
            setMasterFile(fromPosition, "nClientNo", getMasterFile(toPosition, "nClientNo"));
//            setMasterFile(fromPosition, "sFileName", getMasterFile(toPosition, "sFileName"));
        }else{      
            if(getMasterFile(fromPosition, "nFileNoxx") != getMasterFile(toPosition, "nFileNoxx")){
                setMasterFile(toPosition, "sFileName", getMasterFile(fromPosition, "sFileName"));
//                setMasterFile(toPosition, "nFileNoxx", getMasterFile(fromPosition, "nFileNoxx"));
//                setMasterFile(toPosition, "nClientNo", getMasterFile(fromPosition, "nClientNo"));

                setMasterFile(fromPosition, "sFileName", lsTempFileName);
//                setMasterFile(fromPosition, "nFileNoxx", lnTempFileNoxx);
//                setMasterFile(fromPosition, "nClientNo", lnTempClientNo);
            }else{  
                setMasterFile(toPosition, "sFileName", getMasterFile(fromPosition, "sFileName"));
                setMasterFile(toPosition, "nFileNoxx", getMasterFile(fromPosition, "nFileNoxx"));
                setMasterFile(toPosition, "nClientNo", getMasterFile(fromPosition, "nClientNo"));

                setMasterFile(fromPosition, "sFileName", lsTempFileName);
                setMasterFile(fromPosition, "nFileNoxx", lnTempFileNoxx);
                setMasterFile(fromPosition, "nClientNo", lnTempClientNo);
            }
        }
    }
    
    private void loadDepartmentFile(String foDeptIDxx){
        String lsSQL = "SELECT" +
                            "  a.sDeptIDxx" +
                            ", a.sFileCode" +
                            ", a.nEntryNox" +
                            ", b.sBarrcode" +
                            ", b.sBriefDsc" +
                            ", b.nNoPagesx" +
                            ", b.nNoCopies" +
                            ", b.sB2BPagex" +
                        " FROM EDocSys_Department_File a" +
                            " LEFT JOIN EDocSys_File b" +
                            " ON a.sFileCode = b.sFileCode" +
                        " WHERE a.sDeptIDxx =  " + SQLUtil.toSQL(foDeptIDxx) +
                        " ORDER BY a.nEntryNox";
        
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
            poData.getSysFile().clear();
            addSysFile();  
            
            if(MiscUtil.RecordCount(loRS)==0){
                setMessage("Empty record retrieved!!!");
            }else{  
                while (loRS.next()) {
                    setSysFile(poData.SysFileItemCount() -1,"sTransNox", getMaster("sTransNox"));
                    setSysFile(poData.SysFileItemCount() -1,"nEntryNox", loRS.getInt("nEntryNox"));
                    setSysFile(poData.SysFileItemCount() -1,"sFileCode", loRS.getString("sFileCode"));
                    setSysFile(poData.SysFileItemCount() -1,"sBarrcode", loRS.getString("sBarrcode"));
                    setSysFile(poData.SysFileItemCount() -1,"sBriefDsc", loRS.getString("sBriefDsc"));
                    setSysFile(poData.SysFileItemCount() -1,"nNoPagesx", loRS.getInt("nNoPagesx"));
                    setSysFile(poData.SysFileItemCount() -1,"nNoCopies", loRS.getInt("nNoCopies"));
                    setSysFile(poData.SysFileItemCount() -1,"sB2BPagex", loRS.getString("sB2BPagex"));                  
                    
                    if(!loRS.isLast()){
                        addSysFile();
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
    
    public void setSourceCD(String fsSourceCd){
        this.psSourceCd = fsSourceCd;
    }
        
    public void sendEmail(){
        Connection loCon = poGRider.getConnection();
        
        for(int lnCtr=0;lnCtr<= poData.ItemCount() -1;lnCtr++) {
        String lsSendTo="";
        Integer lnEntryNo=0;
//        JSONParser oParser = new JSONParser();
//        JSONObject oJson = null;
//        FileReader fileReader;
        JSONObject oPar = new JSONObject();        
        
            String lsSQL = "SELECT" +
                                "  sBranchCd" +
                                ", sBranchNm" +
                                ", IF(ISNULL(sEmailAdd),'',sEmailAdd) xEmailAdd" +
                            " FROM Branch" +
                            " WHERE sBranchCd =  " + SQLUtil.toSQL(poData.getDetail().get(lnCtr).getSourceNo().substring(0, 4));
            
            Statement loStmt = null;
            ResultSet loRS = null;
            try {                
                loStmt = loCon.createStatement();
                loRS = loStmt.executeQuery(lsSQL);
                while(loRS.next()){
                    lsSendTo=loRS.getString("xEmailAdd");
                }                
            } catch (SQLException ex){
                ex.printStackTrace();
                setMessage(ex.getMessage());
            }
            finally{
                MiscUtil.close(loRS);
                MiscUtil.close(loStmt);
            }     
            
            oPar.put("to", lsSendTo.equals("")? "gemencias@guanzongroup.com.ph":lsSendTo);
//            oPar.put("to", lsSendTo.equals("")? "jovancasilang_20@hotmail.com":lsSendTo);
            if(lsSendTo.equals("")){
                oPar.put("subject", "Registration Expense - "+poData.getDetail().get(lnCtr).getCompanyName()+"Branch has no valid email Address");
            }else{
                oPar.put("subject", "Registration Expense - "+poData.getDetail().get(lnCtr).getCompanyName());
            }
            oPar.put("body", "Please see attached scanned documents for subject client.\n Thanks!");
            lnEntryNo=0;
            for(int nCtr=0;nCtr<= poData.MasterFileItemCount()-1;nCtr++) {
                if(poData.getMasterFile().get(nCtr).getClientNo()==lnCtr+1){
                    oPar.put("filename"+(lnEntryNo+1), "D:\\EDocx_Scanned\\"+poData.getMasterFile().get(nCtr).getFileName());
                    lnEntryNo=lnEntryNo+1;
                }
            }
            System.err.print("JSON writing to file...");
            try (FileWriter file = new FileWriter("D:/GGC_Java_Systems/temp/json/"+poData.getDetail().get(lnCtr).getSourceNo()+".json")) {
                file.write(oPar.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            String[] xargs = new String[] {"D:/GGC_Java_Systems/access.token", "D:/GGC_Java_Systems/temp/json/"+poData.getDetail().get(lnCtr).getSourceNo()+".json"};
//            SendRawMail.main(xargs);
//            try {              
//                fileReader = new FileReader("D:/GGC_Java_Systems/temp/json/"+poData.getDetail().get(lnCtr).getSourceNo()+".json");
//                oJson = (JSONObject)oParser.parse(fileReader);
//                if(((String)oJson.get("result")).equalsIgnoreCase("success")){
//                    fileReader.close();
//                    System.gc();
//                    File myObj = new File("D:/GGC_Java_Systems/temp/json/"+poData.getDetail().get(lnCtr).getSourceNo()+".json");
//                    if(myObj.delete()){ 
//                        System.out.println("Deleted the file: " + myObj.getName());
//                    }else{
//                        System.err.println("Failed to delete the file." + myObj.getName());
//                    } 
//                }
//            } catch (IOException | ParseException ex) {
//                Logger.getLogger(XMEDocumentsMulti.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
    }

    public String getMessage(){return psMessage;}
    private void setMessage(String fsValue){psMessage = fsValue;}
  
    private XMBranch poBranch = null;
    private XMBranch poOrigin = null;
    private XMDepartment poDepartment = null;
//    private XMBarcode poEDocSysFile = null;
    private XMEmployee poEmployee = null;
    private XMModule poModule = null;

    private UnitEDocumentsMulti poData;
    private EDocumentsMulti poCtrl;
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
    private int pnCounterx = 0;
    private int pnClientNo = 0;
    SimpleDateFormat psDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private boolean pbWithParnt = false;
}

