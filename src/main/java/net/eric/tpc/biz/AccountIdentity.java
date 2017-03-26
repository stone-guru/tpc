package net.eric.tpc.biz;

public class AccountIdentity {
    private String bankCode;
    private String number;

    public AccountIdentity() {

    }

    public AccountIdentity(String number, String bankCode) {
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
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bankCode == null) ? 0 : bankCode.hashCode());
        result = prime * result + ((number == null) ? 0 : number.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AccountIdentity other = (AccountIdentity) obj;
        if (bankCode == null) {
            if (other.bankCode != null)
                return false;
        } else if (!bankCode.equals(other.bankCode))
            return false;
        if (number == null) {
            if (other.number != null)
                return false;
        } else if (!number.equals(other.number))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return this.number + "@" + this.bankCode;
    }
    
    
}
