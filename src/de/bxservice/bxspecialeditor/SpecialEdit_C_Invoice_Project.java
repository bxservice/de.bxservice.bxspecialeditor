package de.bxservice.bxspecialeditor;

import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.model.MPeriod;
import org.compiere.model.PO;
import org.compiere.model.X_C_Invoice;
import org.compiere.model.X_C_Payment;
import org.idempiere.base.ISpecialEditCallout;
import org.idempiere.base.SpecialEditorUtils;

public class SpecialEdit_C_Invoice_Project implements ISpecialEditCallout {

	@Override
	public boolean canEdit(GridTab mTab, GridField mField, PO po) {
		boolean canEdit = true;
		if (po instanceof MInvoice) {
			//Validate accounting date on invoice
			MInvoice invoice = (MInvoice) po;
			if (!MPeriod.isOpen(invoice.getCtx(), invoice.getDateAcct(), invoice.getC_DocType().getDocBaseType(), invoice.getAD_Org_ID())) {
				// Cannot change date if accounting period is closed
				canEdit = false;
			}
			
			//Validate accounting date on payment
			MPayment payment = (MPayment) invoice.getC_Payment();
			if (payment != null &&
					!MPeriod.isOpen(payment.getCtx(), payment.getDateAcct(), payment.getC_DocType().getDocBaseType(), payment.getAD_Org_ID())) {
				// Cannot change date if accounting period is closed
				canEdit = false;
			}

		}		
		return canEdit;
	}

	@Override
	public String validateEdit(GridTab mTab, GridField mField, PO po,
			Object newValue) {
		return null;
	}

	@Override
	public boolean preEdit(GridTab mTab, GridField mField, PO po) {
		return true;
	}

	@Override
	public boolean updateEdit(GridTab mTab, GridField mField, PO po, Object newValue) {
		if (po instanceof MInvoice && newValue != null && newValue instanceof Integer) {
			//Update invoice
			X_C_Invoice invoice = new X_C_Invoice(po.getCtx(), po.get_ID(), po.get_TrxName()); // use X_C_invoice to avoid the validation of MInvoice.beforeSave
			Integer newC_Project_ID = (Integer) newValue;
			invoice.setC_Project_ID(newC_Project_ID);
			invoice.saveEx();
			
			//Update payment
			X_C_Payment payment = new X_C_Payment(po.getCtx(), invoice.getC_Payment_ID(), po.get_TrxName());
			payment.setC_Project_ID(newC_Project_ID);
			payment.saveEx();
			
			return true;
		}
		return false;
	}

	@Override
	public boolean postEdit(GridTab mTab, GridField mField, PO po) {
		// Repost invoice
		SpecialEditorUtils.post(mTab, po);
		
		if (po instanceof MInvoice) {
			MInvoice invoice = (MInvoice) po;
			
			//Repost payment
			MPayment payment = (MPayment) invoice.getC_Payment();
			if (payment != null) {
				SpecialEditorUtils.post(mTab, payment);
			}
		}
		
		//Refresh
		SpecialEditorUtils.refresh(mTab);
		return true;
	}

}
