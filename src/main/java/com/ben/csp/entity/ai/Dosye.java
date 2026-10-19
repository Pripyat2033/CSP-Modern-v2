package com.ben.csp.entity.ai;

import net.minecraft.text.Text;

/**
 * Dosye - Employee dossier/personnel file containing employee information.
 * This class holds personality traits, work history, party status, etc.
 */
public class Dosye {
    
    private static final java.util.Random random = new java.util.Random();
    
    // Personality trait types
    public enum Trait {
        ANXIOUS, OPTIMISTIC, PRAGMATIC, DUTIFUL, RECKLESS, LOYAL
    }
    
    // Party status (for Soviet-era organization context)
    public enum PartyStatus {
        CANDIDATE, MEMBER, NON_MEMBER, EX_COMUNIST, ELITE
    }
    
    // Kharakteristika - character assessment
    public enum Kharakteristika {
        POSITIVE, NEUTRAL, NEGATIVE, UNDER_INVESTIGATION
    }
    
    // Work record for employment history
    public static class WorkRecord {
        private final String employer;
        private final String position;
        private int startDate;
        private int endDate;
        
        public WorkRecord(String employer, String position, int startYear, int endYear) {
            this.employer = employer;
            this.position = position;
            this.startDate = startYear;
            this.endDate = endYear;
        }
        
        public String getEmployer() { return employer; }
        public String getPosition() { return position; }
        public int getStartDate() { return startDate; }
        public int getEndDate() { return endDate; }
        
        @Override
        public String toString() {
            return "WorkRecord{employer='" + employer + "', position='" + position + "', start=" + startDate + ", end=" + endDate + "}";
        }
    }
    
    private final String firstName;
    private final String patronymic;
    private final String lastName;
    private final String nationality;
    private final String birthplace;
    private final Trait trait;
    private final java.util.List<WorkRecord> workHistory;
    private final PartyStatus partyStatus;
    private final Kharakteristika kharakteristika;
    private final java.util.List<String> kompromat;
    
    /**
     * Create Dosye with full information.
     */
    public Dosye(String firstName, String patronymic, String lastName, 
                 String nationality, String birthplace, Trait trait,
                 java.util.List<WorkRecord> workHistory, PartyStatus partyStatus,
                 Kharakteristika kharakteristika) {
        this.firstName = firstName;
        this.patronymic = patronymic;
        this.lastName = lastName;
        this.nationality = nationality;
        this.birthplace = birthplace;
        this.trait = trait;
        this.workHistory = workHistory != null ? workHistory : new java.util.ArrayList<>();
        this.partyStatus = partyStatus;
        this.kharakteristika = kharakteristika;
        this.kompromat = new java.util.ArrayList<>();
    }
    
    /**
     * Create Dosye with full information including kompromat.
     */
    public Dosye(String firstName, String patronymic, String lastName,
                 String nationality, String birthplace, Trait trait,
                 java.util.List<WorkRecord> workHistory, PartyStatus partyStatus,
                 Kharakteristika kharakteristika, java.util.List<String> kompromat) {
        this.firstName = firstName;
        this.patronymic = patronymic;
        this.lastName = lastName;
        this.nationality = nationality;
        this.birthplace = birthplace;
        this.trait = trait;
        this.workHistory = workHistory != null ? workHistory : new java.util.ArrayList<>();
        this.partyStatus = partyStatus;
        this.kharakteristika = kharakteristika;
        this.kompromat = kompromat != null ? kompromat : new java.util.ArrayList<>();
    }
    
    /**
     * Create simplified Dosye for entity initialization.
     */
    public Dosye(String firstName, String patronymic, String lastName) {
        this.firstName = firstName;
        this.patronymic = patronymic;
        this.lastName = lastName;
        this.nationality = getRandomNationality();
        this.birthplace = "Semikhody";
        this.trait = Trait.values()[random.nextInt(Trait.values().length)];
        this.workHistory = new java.util.ArrayList<>();
        this.partyStatus = PartyStatus.MEMBER;
        this.kharakteristika = Kharakteristika.POSITIVE;
        this.kompromat = new java.util.ArrayList<>();
    }
    
    private static String getRandomNationality() {
        return "Russian";
    }
    
    /**
     * Get full name for custom entity name display.
     */
    public String getFullName() {
        return firstName + " " + patronymic + " " + lastName;
    }
    
    public String getFirstName() { return firstName; }
    public String getPatronymic() { return patronymic; }
    public String getLastName() { return lastName; }
    public String getNationality() { return nationality; }
    public String getBirthplace() { return birthplace; }
    public Trait getTrait() { return trait; }
    public java.util.List<WorkRecord> getWorkHistory() { return workHistory; }
    public PartyStatus getPartyStatus() { return partyStatus; }
    public Kharakteristika getKharakteristika() { return kharakteristika; }
    public java.util.List<String> getKompromat() { return kompromat; }
    
    /**
     * Get text representation for the entity name.
     */
    public Text getTextRepresentation() {
        return Text.literal(this.getFullName());
    }
    
    /**
     * Get detailed personality profile string for debugging and gameplay.
     * Uses all Dosye fields including firstName, patronymic, lastName, trait,
     * partyStatus, workHistory, and kompromat to provide meaningful access.
     */
    public String getPersonalityProfile() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DOSYЕ: ").append(getFullName()).append("\n");
        sb.append("Nationality:").append(getNationality()).append("\n");
        sb.append("Birthplace:").append(getBirthplace()).append("\n");
        sb.append("Trait:").append(getTrait()).append("\n");
        sb.append("Party Status:").append(getPartyStatus()).append("\n");
        sb.append("Character Assessment: ").append(getKharakteristika()).append("\n");
        if (!getWorkHistory().isEmpty()) {
            sb.append("Work History:\n");
            for (Dosye.WorkRecord record : getWorkHistory()) {
                sb.append("  - ").append(record.getEmployer()).append(" as ")
                  .append(record.getPosition()).append(" (\u0413/" + record.getStartDate() + "-\u0413/" + record.getEndDate() + ")\n");
            }
        }
        if (!getKompromat().isEmpty()) {
            sb.append("Kompromat:").append("\n");
            for (String entry : getKompromat()) {
                sb.append("  * ").append(entry).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Get text representation for debugging.
     */
    @Override
    public String toString() {
        // Use getters to access unused fields meaningfully
        return "Dosye{" +
                "firstName=" + getFirstName() + ", " +
                "patronymic=" + getPatronymic() + ", " +
                "lastName=" + getLastName() + ", " +
                "nationality=" + getNationality() + ", " +
                "birthplace=" + getBirthplace() + ", " +
                "trait=" + getTrait() + ", " +
                "partyStatus=" + getPartyStatus() +", " +
                "kharakteristika=" + getKharakteristika() +"}";
    }
}
