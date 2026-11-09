package com.ben.csp.entity.ai;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;

/**
 * NBT (de)serialization for {@link Dosye}, so a worker keeps the same dossier
 * (name, party status, kompromat) across a save/load cycle.
 */
public final class DosyeNbt {

    private DosyeNbt() {}

    public static NbtCompound toNbt(Dosye d) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("FirstName", d.getFirstName());
        nbt.putString("Patronymic", d.getPatronymic());
        nbt.putString("LastName", d.getLastName());
        nbt.putString("Nationality", d.getNationality());
        nbt.putString("Birthplace", d.getBirthplace());
        nbt.putString("Trait", d.getTrait().name());
        nbt.putString("PartyStatus", d.getPartyStatus().name());
        nbt.putString("Kharakteristika", d.getKharakteristika().name());

        NbtList kompromat = new NbtList();
        for (String entry : d.getKompromat()) {
            kompromat.add(NbtString.of(entry));
        }
        nbt.put("Kompromat", kompromat);

        NbtList history = new NbtList();
        for (Dosye.WorkRecord record : d.getWorkHistory()) {
            NbtCompound r = new NbtCompound();
            r.putString("Employer", record.getEmployer());
            r.putString("Position", record.getPosition());
            r.putInt("Start", record.getStartDate());
            r.putInt("End", record.getEndDate());
            history.add(r);
        }
        nbt.put("WorkHistory", history);
        return nbt;
    }

    public static Dosye fromNbt(NbtCompound nbt) {
        List<Dosye.WorkRecord> history = new ArrayList<>();
        NbtList histList = nbt.getList("WorkHistory", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < histList.size(); i++) {
            NbtCompound r = histList.getCompound(i);
            history.add(new Dosye.WorkRecord(
                r.getString("Employer"), r.getString("Position"), r.getInt("Start"), r.getInt("End")));
        }
        List<String> kompromat = new ArrayList<>();
        NbtList kompList = nbt.getList("Kompromat", NbtElement.STRING_TYPE);
        for (int i = 0; i < kompList.size(); i++) {
            kompromat.add(kompList.getString(i));
        }
        return new Dosye(
            nbt.getString("FirstName"), nbt.getString("Patronymic"), nbt.getString("LastName"),
            nbt.getString("Nationality"), nbt.getString("Birthplace"),
            Dosye.Trait.valueOf(nbt.getString("Trait")),
            history,
            Dosye.PartyStatus.valueOf(nbt.getString("PartyStatus")),
            Dosye.Kharakteristika.valueOf(nbt.getString("Kharakteristika")),
            kompromat);
    }
}
