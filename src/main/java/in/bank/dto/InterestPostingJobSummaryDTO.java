package in.bank.dto;

class InterestPostingJobSummaryDTO {
	 
    private Integer month;
    private Integer year;
    private int totalAccounts;
    private int posted;             // newly credited
    private int skipped;            // already done for this period
    private int failed;             // errors
 
    public InterestPostingJobSummaryDTO() {}
 
    public InterestPostingJobSummaryDTO(int month, int year,
                                        int totalAccounts,
                                        int posted, int skipped, int failed) {
        this.month         = month;
        this.year          = year;
        this.totalAccounts = totalAccounts;
        this.posted        = posted;
        this.skipped       = skipped;
        this.failed        = failed;
    }
 
    public Integer getMonth()        { return month; }
    public Integer getYear()         { return year; }
    public int getTotalAccounts()    { return totalAccounts; }
    public int getPosted()           { return posted; }
    public int getSkipped()          { return skipped; }
    public int getFailed()           { return failed; }
}