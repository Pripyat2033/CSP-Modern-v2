package com.ben.csp.entity.ai;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The "Dosye" (Dossier) holds the generated background, personality, and history for an NPC.
 * This is the core data structure for the Dynamic Personality Engine (DPE).
 */
public class Dosye {
    public enum PersonalityTrait {
        METICULOUS,
        CARELESS,
        BY_THE_BOOK,
        AMBITIOUS,
        JOKER,
        GRIM
    }

    // Science Grade: Added Party Status, a critical component of any Soviet-era dossier.
    public enum PartyStatus {
        NON_PARTY,
        KOMSOMOL, // Youth League
        CPSU_MEMBER, // Communist Party of the Soviet Union
        EXPELLED
    }

    // Science Grade: Replaced a simple list of strings with a structured record for work history.
    public record WorkRecord(String enterprise, String position, int startYear, int endYear) {
        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("Enterprise", enterprise);
            nbt.putString("Position", position);
            nbt.putInt("StartYear", startYear);
            nbt.putInt("EndYear", endYear);
            return nbt;
        }

        public static WorkRecord fromNbt(NbtCompound nbt) {
            return new WorkRecord(
                nbt.getString("Enterprise"),
                nbt.getString("Position"),
                nbt.getInt("StartYear"),
                nbt.getInt("EndYear")
            );
        }
    }

    private final String firstName;
    private final String patronymic;
    private final String lastName;
    private final String nationality;
    private final String birthplace;
    private final PersonalityTrait personalityTrait;
    private final List<WorkRecord> workHistory;
    private final PartyStatus partyStatus;
    private final String kharakteristika; // Formal character reference
    private final List<String> kompromat; // Compromising materials

    public Dosye(String firstName, String patronymic, String lastName, String nationality, String birthplace, PersonalityTrait trait, List<WorkRecord> workHistory, PartyStatus partyStatus, String kharakteristika, List<String> kompromat) {
        this.firstName = firstName;
        this.patronymic = patronymic;
        this.lastName = lastName;
        this.nationality = nationality;
        this.birthplace = birthplace;
        this.personalityTrait = trait;
        this.workHistory = workHistory;
        this.partyStatus = partyStatus;
        this.kharakteristika = kharakteristika;
        this.kompromat = kompromat;
    }

    public String getFullName() {
        return String.format("%s %s %s", firstName, patronymic, lastName);
    }

    public String getNationality() { return nationality; }

    public PersonalityTrait getPersonalityTrait() {
        return personalityTrait;
    }

    public List<WorkRecord> getWorkHistory() {
        return workHistory;
    }

    public PartyStatus getPartyStatus() {
        return partyStatus;
    }

    public String getKharakteristika() {
        return kharakteristika;
    }

    public List<String> getKompromat() {
        return kompromat;
    }

    public void writeNbt(NbtCompound nbt) {
        NbtCompound dosyeNbt = new NbtCompound();
        dosyeNbt.putString("FirstName", firstName);
        dosyeNbt.putString("Patronymic", patronymic);
        dosyeNbt.putString("LastName", lastName);
        dosyeNbt.putString("Nationality", nationality);
        dosyeNbt.putString("Birthplace", birthplace);
        dosyeNbt.putString("Personality", personalityTrait.name());
        dosyeNbt.putString("PartyStatus", partyStatus.name());
        dosyeNbt.putString("Kharakteristika", kharakteristika);

        NbtList workHistoryNbt = new NbtList();
        workHistory.forEach(rec -> workHistoryNbt.add(rec.toNbt()));
        dosyeNbt.put("WorkHistory", workHistoryNbt);

        NbtList kompromatNbt = new NbtList();
        kompromat.forEach(k -> kompromatNbt.add(NbtString.of(k)));
        dosyeNbt.put("Kompromat", kompromatNbt);

        nbt.put("Dosye", dosyeNbt);
    }

    public static Dosye fromNbt(NbtCompound nbt) {
        if (!nbt.contains("Dosye")) {
            // Generate a default/placeholder if none exists
            return new Dosye("Ivan", "Ivanovich", "Ivanov", "Russian", "Unknown", PersonalityTrait.BY_THE_BOOK, Collections.emptyList(), PartyStatus.NON_PARTY, "An unremarkable worker.", Collections.emptyList());
        }
        NbtCompound dosyeNbt = nbt.getCompound("Dosye");

        List<WorkRecord> workHistory = new ArrayList<>();
        if (dosyeNbt.contains("WorkHistory", NbtElement.LIST_TYPE)) {
            NbtList workHistoryNbt = dosyeNbt.getList("WorkHistory", NbtElement.COMPOUND_TYPE);
            workHistoryNbt.forEach(rec -> workHistory.add(WorkRecord.fromNbt((NbtCompound) rec)));
        }

        List<String> kompromat = new ArrayList<>();
        if (dosyeNbt.contains("Kompromat", NbtElement.LIST_TYPE)) {
            NbtList kompromatNbt = dosyeNbt.getList("Kompromat", NbtElement.STRING_TYPE);
            kompromatNbt.forEach(k -> kompromat.add(k.asString()));
        }

        return new Dosye(
            dosyeNbt.getString("FirstName"),
            dosyeNbt.getString("Patronymic"),
            dosyeNbt.getString("LastName"),
            dosyeNbt.getString("Nationality"),
            dosyeNbt.getString("Birthplace"),
            PersonalityTrait.valueOf(dosyeNbt.getString("Personality")),
            workHistory,
            PartyStatus.valueOf(dosyeNbt.getString("PartyStatus")),
            dosyeNbt.getString("Kharakteristika"),
            kompromat
        );
    }
}