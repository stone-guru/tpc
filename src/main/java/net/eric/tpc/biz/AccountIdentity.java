package net.eric.tpc.biz;

public class AccountIdentity {
	private String bankCode;
	private String number;
	
	public AccountIdentity(){
		
	}
	
	public AccountIdentity(String number, String bankCode){
		this.number = number;
		this.bankCode = bankCode;
	}
	
	public String getBankCode() {
		return bankCode;
	}
	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String accountNumber) {
		this.number = accountNumber;
	}
}
