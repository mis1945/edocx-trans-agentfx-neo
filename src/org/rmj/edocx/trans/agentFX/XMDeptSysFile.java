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
import org.rmj.edocx.trans.DeptSysFile;
import org.rmj.edocx.trans.pojo.UnitDeptSysFile;
import org.rmj.edocx.trans.pojo.UnitSysFile;
import org.rmj.parameters.agent.XMDepartment;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import com.fujitsu.pfu.fiscn.sdk.FiscnException;
import java.util.ArrayList;
import org.rmj.appdriver.agent.MsgBox;
import org.rmj.parameter.agentfx.XMBarcode;

/**
 *
 * @author jef
 */
public class XMDeptSysFile {
    public XMDeptSysFile(GRider foGRider, String fsBranchCd, boolean fbWithParent) throws FiscnException{
        this.poGRider = foGRider;
        if(foGRider != null){
            this.pbWithParnt = fbWithParent;
            this.psBranchCd = fsBranchCd;
            poCtrl = new DeptSysFile();
            
            poCtrl.setGRider(foGRider);
            poCtrl.setBranch(psBranchCd);
            poCtrl.setOrigin(psOriginxx);
            poCtrl.setFileCd(psFileCode);
            poCtrl.setDepartment(psDeptIDxx);
            pnEditMode = EditMode.UNKNOWN;
        }
    }
       
    public XMBarcode getFileCode(int row){
      if(pnEditMode == EditMode.UNKNOWN || poCtrl == null)
         return null;
      else if(row < 0 || row >= poData.getSysFile().size())
         return null;
      
      if(poEDocSysFile.get(row) == null)
         poEDocSysFile.set(row, new XMBarcode(poGRider, psBranchCd, true));

      poEDocSysFile.get(row).openRecord(poData.getSysFile().get(row).getFileCode());
      return poEDocSysFile.get(row);
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
    
    public Object getSysFile(int row, String fsCol){
      return getSysFile(row, poData.getSysFile().get(row).getColumn(fsCol));
   }

   public Object getSysFile(int row, int col){
      if(pnEditMode == EditMode.UNKNOWN || poCtrl == null)
         return null;
      else if(row < 0 || row >= poData.getSysFile().size())
         return null;
      
      return poData.getSysFile().get(row).getValue(col);
   }   

   public void setSysFile(int row, String fsCol, Object value){
      setSysFile(row, poData.getSysFile().get(row).getColumn(fsCol), value);
   }

   public void setSysFile(int row, int col, Object value){
      if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
          if(row >= 0 && row <= poData.getSysFile().size()){
             if(row == poData.getSysFile().size()){
                 poData.getSysFile().add(new UnitSysFile());
                 poEDocSysFile.add(null);
             }
             
             poData.getSysFile().get(row).setValue(col, value);
          }
      }
   }   
   
   public void addDetail(){
      if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
         poData.getSysFile().add(new UnitSysFile());
         poEDocSysFile.add(null);
      }
   }
   
   public void deleteDetail(int row){
      if(pnEditMode != EditMode.UNKNOWN && poCtrl != null){
          if(!(row < 0 || row >= poData.getSysFile().size()))
            poData.getSysFile().remove(row);
      }
   }
    
    public boolean newTransaction() {
        poEDocSysFile.clear();
        if(poCtrl == null){
            return false;
        }
        
        poData = (UnitDeptSysFile) poCtrl.newTransaction();

        if(poData == null){
            return false;
        }
        else{
            //set the values of foreign key(object) to null
//            po = null;
//            addDetail();
            pnEditMode = EditMode.ADDNEW;
            
            return true;
        }
    }

    public boolean loadTransaction(String fsTransNox) {
        poEDocSysFile.clear();
        if(poCtrl == null){
            return false;
        }

        poData = (UnitDeptSysFile) poCtrl.loadTransaction(fsTransNox);

        if(poData.getMaster().getDeptIDxx()== null){
            return false;
        }
        else{
         //set the values of foreign key(object) to null        
//         for(int lnCtr=0;lnCtr<=poData.getSysFile().size();lnCtr++)
//             poEDocSysFile.add(null);
         
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
             
            UnitDeptSysFile loResult=null;
//            setMaster("nEntryNox", poData.SysFileItemCount() -1);
            if(pnEditMode == EditMode.ADDNEW)
                loResult = (UnitDeptSysFile) poCtrl.saveUpdate(poData, "");
            else
                loResult = (UnitDeptSysFile) poCtrl.saveUpdate(poData, (String) poData.getMaster().getValue(1));
            
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
//                if(!pbWithParnt) poGRider.commitTrans();
                
                setMessage("Transaction saved successfully...");
                return true;
            }
        }
    }

    public boolean searchMaster(String field, String value){
        if(field.equalsIgnoreCase("sDeptIDxx")){
            return searchDepartment(field, value);
        }else if(field.equalsIgnoreCase("sDeptName")){
            return searchDepartment(field, value);
        }else{
            setMessage("Invalid search field [" + field + "]  detected!");
            return false;
        }
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
    
    private boolean searchSysFile(int row, String fsFieldNm, String fsValue){
      System.out.println("Inside searchSysFile");
      fsValue = fsValue.trim();
      if(fsValue.trim().length() == 0){
         System.out.println("Nothing to process!");
         return false;
      }

      String lsSQL = getSQL_EDocSysFile();
      if(fsFieldNm.equalsIgnoreCase("sBarrCode")){
         if(getFileCode(row) != null){  
            if(fsValue.trim().equalsIgnoreCase((String)getFileCode(row).getMaster("sBarrCode"))){
                System.out.println("The same Barcode!");
                return false;
            }
         }
         lsSQL = MiscUtil.addCondition(lsSQL, "sBarrCode LIKE " + SQLUtil.toSQL(fsValue));
      }
      else if(fsFieldNm.equalsIgnoreCase("sDescript"))
      {
         if(getFileCode(row) != null){  
             if(fsValue.trim().equalsIgnoreCase((String)getFileCode(row).getMaster("sBriefDsc"))){
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
            poData.getSysFile().get(row).setFileCode(loRS.getString("sFileCode"));
            poEDocSysFile.set(row, new XMBarcode(poGRider, psBranchCd, true)); 
            poEDocSysFile.get(row).openRecord(loRS.getString("sFileCode"));
            lbHasRec = true;
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
    
    public void setEDocSysFile(String fsFileCode) {
        psFileCode = fsFileCode;
        poCtrl.setFileCd(fsFileCode);
    }
    
    
    public boolean searchWithCondition(String fsFieldNm, String fsValue, String fsFilter){
        System.out.println("Inside SearchWithCondition");
        fsValue = fsValue.trim();
        
        if(fsValue.trim().length() == 0){
            setMessage("Nothing to process!");
            return false;
        }//end: if(fsValue.trim().length() == 0)

        String lsSQL = getSQL_Master();
        if(fsFieldNm.equalsIgnoreCase("sDeptIDxx")){
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
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDeptIDxx LIKE " + SQLUtil.toSQL(lsPrefix + "%" + fsValue));
        }// end: if(fsFieldNm.equalsIgnoreCase("sTransNox"))
        else if(fsFieldNm.equalsIgnoreCase("sDeptName")){
            if(pnEditMode != EditMode.UNKNOWN){
                if(fsValue.trim().equalsIgnoreCase((String)getMaster("sDeptName"))){
                    setMessage("The same branch name!");
                    return false;
                }
            }
                   
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sDeptName LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }//end: if(fsFieldNm.equalsIgnoreCase("sTransNox")) - else if(fsFieldNm.equalsIgnoreCase("sClientNm"))=
        
        if(!fsFilter.isEmpty()){
           lsSQL = MiscUtil.addCondition(lsSQL, fsFilter);
        }

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
                                                                "Department ID»Department", 
                                                                "sDeptIDxx»sDeptName");
              
                if (loValue != null){
                    lbHasRec = loadTransaction((String) loValue.get("sDeptIDxx"));
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
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDeptName LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        else if(fsFieldNm.equalsIgnoreCase("sDeptIDxx"))
        {
            if(fsValue.trim().equalsIgnoreCase((String)getMaster(fsFieldNm))){
                setMessage("The same Department Code!");
                return false;
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDeptIDxx LIKE " + SQLUtil.toSQL(fsValue));
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
    
    public boolean searchField(int row, String field, String value){
      if(field.equalsIgnoreCase("sFileCode")){
         return searchFile(row, field, value);
      }else if(field.equalsIgnoreCase("sBarrcode")){
         return searchFile(row, field, value);
      }else if(field.equalsIgnoreCase("sBriefDsc")){
         return searchFile(row, field, value);
      }
      else{
         MsgBox.showOk("Invalid search field [" + field + "]  detected!");
         return false;
      }
   }

   private boolean searchFile(int row, String fsFieldNm, String fsValue){
      fsValue = fsValue.trim();
      if(fsValue.trim().length() == 0){
         System.out.println("Nothing to process!");
         return false;
      }

      String lsSQL = getSQL_EDocSysFile();
      if(fsFieldNm.equalsIgnoreCase("sBarrcode")){
         if(getSysFile(row,"sBarrcode") != null){  
            if(fsValue.trim().equalsIgnoreCase((String)getSysFile(row,"sBarrcode"))){
                System.out.println("The same Barcode!");
                return false;
            }
         }
         lsSQL = MiscUtil.addCondition(lsSQL, "sBarrcode LIKE " + SQLUtil.toSQL(fsValue));
      }else if(fsFieldNm.equalsIgnoreCase("sBriefDsc"))
      {
         if(getSysFile(row,"sBriefDsc") != null){  
             if(fsValue.trim().equalsIgnoreCase((String)getSysFile(row,"sBriefDsc"))){
                System.out.println("The same Description Code!");
                return false;
             }
         }
         lsSQL = MiscUtil.addCondition(lsSQL, "sBriefDsc LIKE " + SQLUtil.toSQL(fsValue));
      }else if(fsFieldNm.equalsIgnoreCase("sFileCode"))
      {
         if(getSysFile(row,"sFileCode") != null){  
             if(fsValue.trim().equalsIgnoreCase((String)getSysFile(row,"sFileCode"))){
                System.out.println("The same File Code!");
                return false;
             }
         }
         lsSQL = MiscUtil.addCondition(lsSQL, "sFileCode LIKE " + SQLUtil.toSQL(fsValue));
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
                                                                "Code»Barrcode»Description", 
                                                                "sFileCode»sBarrcode»sBriefDsc");
              
                if (loValue != null){
                    poData.getSysFile().get(row).setFileCode(loRS.getString("sFileCode"));

                    poData.getSysFile().get(row).setBarCode(loRS.getString("sBarrcode"));
                    poData.getSysFile().get(row).setBriefDsc(loRS.getString("sBriefDsc"));
                    poData.getSysFile().get(row).setNoPages(loRS.getInt("nNoPagesx"));
                    poData.getSysFile().get(row).setNoCopies(loRS.getInt("nNoCopies"));
                    poData.getSysFile().get(row).setB2BPagex(loRS.getString("sB2BPagex"));
                    
                    poEDocSysFile.set(row, new XMBarcode(poGRider, psBranchCd, true)); 
                    poEDocSysFile.get(row).openRecord(loRS.getString("sFileCode"));
                    lbHasRec = true;
                }
            
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
   
    public XMDepartment getDepartment(){
        if(poDepartment == null)
            poDepartment = new XMDepartment(poGRider, psBranchCd, true);
        poDepartment.openRecord(psDeptIDxx);
        return poDepartment;
    }
        
    public void setDepartment(String fsDeptIDxx) {
        psDeptIDxx = fsDeptIDxx;
        poCtrl.setDepartment(fsDeptIDxx);
    }
    
    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
        poCtrl.setGRider(foGRider);
    }

    public int getEditMode() {
        return pnEditMode;
    }
    
    public int SysFileCount(){
        return poData.SysFileItemCount();
    }
    
//    private String getSQL_Master(){
//        return "SELECT" +
//                    "  a.sTransNox" +
//                    ", b.sBranchNm" +
//                    ", a.dTransact" +
//                    ", c.sDeptName" +
//                    ", d.sCompnyNm" +
//                    ", e.sModuleDs" +
//                " FROM EDocSys_Master a" +
//                    " LEFT JOIN Department c" +
//                        " ON a.sDeptIDxx = c.sDeptIDxx" +
//                    " LEFT JOIN Client_Master d" +
//                        " ON a.sEmployID = d.sClientID" +
//                    " LEFT JOIN EDocSys_Module e" +
//                        " ON a.sModuleCd = e.sModuleCd" +
//                    ", Branch b" +
//                " WHERE a.sBranchCd = b.sBranchCd";
//    }
    
     private String getSQL_Master(){
        return "SELECT" +
                " a.sDeptIDxx" +
                ", a.sFileCode" +
                ", b.sDeptName" +
                ", c.sBarrcode" +
                ", c.sBriefDsc" +
                ", c.nNoPagesx" +
                ", c.nNoCopies" +
                ", c.sB2BPagex" +
                " FROM EDocSys_Department_File a" +
                " LEFT JOIN Department b" +
                " ON a.sDeptIDxx = b.sDeptIDxx" +
                " LEFT JOIN EDocSys_File c" +
                " ON a.sFileCode = c.sFileCode" +
                " GROUP BY sDeptIDxx";
    }
    
    private String getSQL_Department(){
        return "SELECT" +
                " a.sDeptIDxx" +
                ", a.sDeptName" +
                " FROM Department a" +
                " LEFT JOIN EDocSys_Department_File b" +
                " ON a.sDeptIDxx = b.sDeptIDxx" +
                " WHERE a.cRecdStat = '1'" +
                " AND a.sDeptIDxx NOT IN(SELECT sDeptIDxx " +
                " FROM EDocSys_Department_File)";
    }
    
    public String getMessage(){return psMessage;}
    private void setMessage(String fsValue){psMessage = fsValue;}
    private XMDepartment poDepartment = null;
    ArrayList<XMBarcode> poEDocSysFile = new ArrayList<XMBarcode>();
    
    private UnitDeptSysFile poData;
    private DeptSysFile poCtrl;
    private GRider poGRider;

    private int pnEditMode;
    private String psBranchCd;
    private String psOriginxx;
    private String psDeptIDxx;
    private String psFileCode;
    private String psMessage;
    private boolean pbWithParnt = false;
}