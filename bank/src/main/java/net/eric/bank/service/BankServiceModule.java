package net.eric.bank.service;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import net.eric.bank.biz.Validator;
import net.eric.bank.entity.TransferBill;

/**
 * Created by bison on 5/3/17.
 */
public class BankServiceModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(new TypeLiteral<Validator<TransferBill>>(){}).to(BillBasicValidator.class);
        binder.bind(BillSaveStrategy.class);
    }
}
