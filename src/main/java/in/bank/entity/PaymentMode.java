package in.bank.entity;

public enum PaymentMode {
	CASH,       //Physical cash transaction, ex:Deposit or Withdraw
    UPI,       //Mobile payments, ex: phone pap, google pay
    NEFT,      // National Electronic Funds Transfer - bank-to-bank transfer. Usually processed in batches or near real-time.
    RTGS,      //Real-Time Gross Settlement - used for high-value transactions.Minimum amount is typically ₹2,00,000 and processed instantly.
    IMPS,      //Immediate Payment Service - instant interbank transfer available 24/7.
    CARD,      // Card-based payment using debit or credit cards. Example: ATM withdrawal, POS machine, online card payment.
    SYSTEM

}
