package com.ben.csp.entity.ai.dosye;

import net.minecraft.nbt.NbtCompound;
import java.util.List;
import java.util.ArrayList;

/**
 * "Science Grade": Represents a personnel dossier (Dosye).
 * Contains personal history and psychological state for an Enterprise agent.
 */
public class Dosye {
    public enum PersonalityTrait { STOIC, ANXIOUS, DILIGENT, REBELLIOUS, QUESTIONING, RELIABLE }
    public enum PartyStatus { NON_MEMBER, CANDIDATE, MEMBER, ELITE }
    public record WorkRecord(String enterprise, String position, int startYear, int endYear) {}

    private String firstName;
    private String patronymic;
    private String lastName;
    private String hometown;
    private int birthYear;
    private double stressLevel; // 0.0 to 1.0
    private boolean partyMember;
    private int securityClearance;
    private String content;
    private PersonalityTrait trait;
    private PartyStatus partyStatus;
    private List<WorkRecord> workHistory = new ArrayList<>();

    public Dosye(String firstName, String patronymic, String lastName, String nationality, String hometown, PersonalityTrait trait, List<WorkRecord> history, PartyStatus partyStatus, String kharakteristika, List<String> kompromat) {
        this.firstName = firstName;
        this.patronymic = patronymic;
        this.lastName = lastName;
        this.hometown = hometown;
        this.trait = trait;
        this.workHistory = history;
        this.partyStatus = partyStatus;
        this.content = kharakteristika;
        this.stressLevel = 0.0;
        this.securityClearance = 0;
    }

    public Dosye(String hometown, int birthYear) {
        this.hometown = hometown;
        this.birthYear = birthYear;
        this.stressLevel = 0.0;
        this.securityClearance = 0;
        this.content = "";
        this.partyStatus = PartyStatus.NON_MEMBER;
    }

    public String getHometown() { return hometown; }
    public int getBirthYear() { return birthYear; }
    public double getStressLevel() { return stressLevel; }

    public void setStressLevel(double stressLevel) {
        this.stressLevel = Math.max(0.0, Math.min(1.0, stressLevel));
    }

    public void addStress(double amount) {
        setStressLevel(this.stressLevel + amount);
    }

    public boolean requiresPartyMembership() { return partyMember; }
    public void setPartyMember(boolean partyMember) { this.partyMember = partyMember; }

    public int getRequiredSecurityClearance() { return securityClearance; }
    public void setSecurityClearance(int securityClearance) { this.securityClearance = securityClearance; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Hometown", hometown);
        nbt.putInt("BirthYear", birthYear);
        nbt.putDouble("StressLevel", stressLevel);
        nbt.putBoolean("PartyMember", partyMember);
        nbt.putInt("SecurityClearance", securityClearance);
        nbt.putString("Content", content);
        return nbt;
    }

    public static Dosye fromNbt(NbtCompound nbt) {
        Dosye dosye = new Dosye(
            nbt.getString("Hometown"),
            nbt.getInt("BirthYear")
        );
        dosye.setStressLevel(nbt.getDouble("StressLevel"));
        dosye.setPartyMember(nbt.getBoolean("PartyMember"));
        dosye.setSecurityClearance(nbt.getInt("SecurityClearance"));
        dosye.setContent(nbt.getString("Content"));
        return dosye;
    }

    // Authentic Soviet hometowns for the Dynamic Personality Engine
    public static final String[] HOMETOWNS = {
        "Pripyat", "Chernobyl", "Kiev", "Minsk", "Moscow", 
        "Leningrad", "Gomel", "Kharkiv", "Vilnius", "Zaporizhzhia"
    };
}