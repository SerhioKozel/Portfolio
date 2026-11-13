package by.strimka.input;

/**
 * Строка CSV после разбора. Значения хранятся в типах-обёртках:
 * null означает «значение не прочитано или пустое», а 0 — настоящий ноль.
 * С примитивами эти случаи не различить. Заполняется CsvReader, читается валидатором.
 */
public class CsvRow {

    // Номер физической строки файла. Заголовок — строка 1.
    private long lineNumber;
    private String teamName;
    private String purpose;
    private String criticality;
    private Integer month;
    private Integer year;
    private Integer prodBugs;
    private Integer closedTasks;
    private Integer testBugs;
    private Double testingTime;
    private Double leadTime;
    private Double changesLeadTime;
    private Integer releases;
    private Integer failures;
    private Double mttr;

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getCriticality() {
        return criticality;
    }

    public void setCriticality(String criticality) {
        this.criticality = criticality;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getProdBugs() {
        return prodBugs;
    }

    public void setProdBugs(Integer prodBugs) {
        this.prodBugs = prodBugs;
    }

    public Integer getClosedTasks() {
        return closedTasks;
    }

    public void setClosedTasks(Integer closedTasks) {
        this.closedTasks = closedTasks;
    }

    public Integer getTestBugs() {
        return testBugs;
    }

    public void setTestBugs(Integer testBugs) {
        this.testBugs = testBugs;
    }

    public Double getTestingTime() {
        return testingTime;
    }

    public void setTestingTime(Double testingTime) {
        this.testingTime = testingTime;
    }

    public Double getLeadTime() {
        return leadTime;
    }

    public void setLeadTime(Double leadTime) {
        this.leadTime = leadTime;
    }

    public Double getChangesLeadTime() {
        return changesLeadTime;
    }

    public void setChangesLeadTime(Double changesLeadTime) {
        this.changesLeadTime = changesLeadTime;
    }

    public Integer getReleases() {
        return releases;
    }

    public void setReleases(Integer releases) {
        this.releases = releases;
    }

    public Integer getFailures() {
        return failures;
    }

    public void setFailures(Integer failures) {
        this.failures = failures;
    }

    public Double getMttr() {
        return mttr;
    }

    public void setMttr(Double mttr) {
        this.mttr = mttr;
    }
}
